package com.naze.motion.core.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Deterministic provider double with a scripted raw output (NMA-TEST-011). */
class FakeProvider(private val onComplete: suspend () -> String) : AIProvider {
    override val name = "fake"
    override suspend fun complete(request: PlanningRequest): Result<String> =
        runCatching { onComplete() }
}

class AiPlannerTest {

    private val validPlan = """
        {"task":"create_video","actions":[
            {"id":"a1","type":"OPEN_APP","parameters":{"packageName":"com.alightmotion.motion"}},
            {"id":"a2","type":"TAP","target":{"normalizedText":"new project"}},
            {"id":"a3","type":"WAIT","parameters":{"durationMs":"500"}}
        ]}
    """.trimIndent()

    @Test
    fun happyPathProducesActionPlan() = runTest {
        val planner = AiPlanner(FakeProvider { validPlan })
        val result = planner.plan(PlanningRequest("buat video baru", "com.alightmotion.motion"))
        assertTrue(result.isSuccess)
        val plan = result.getOrThrow()
        assertEquals(3, plan.actions.size)
        assertEquals(com.naze.motion.core.domain.ActionType.OPEN_APP, plan.actions[0].type)
    }

    @Test
    fun malformedJsonIsRejected() = runTest {
        val planner = AiPlanner(FakeProvider { "this is not json at all" })
        val result = planner.plan(PlanningRequest("buat video", "com.alightmotion.motion"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PlanningError.MalformedJson)
    }

    @Test
    fun unknownActionTypeIsRejected() = runTest {
        val raw = """{"task":"x","actions":[{"id":"a1","type":"DESTROY_WORLD"}]}"""
        val planner = AiPlanner(FakeProvider { raw })
        val result = planner.plan(PlanningRequest("x", "com.alightmotion.motion"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PlanningError.SchemaViolation)
    }

    @Test
    fun missingActionsFieldIsRejected() = runTest {
        val raw = """{"task":"x"}"""
        val planner = AiPlanner(FakeProvider { raw })
        val result = planner.plan(PlanningRequest("x", "com.alightmotion.motion"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PlanningError.SchemaViolation)
    }

    @Test
    fun emptyActionsArrayIsRejectedBySafety() = runTest {
        val raw = """{"task":"x","actions":[]}"""
        val planner = AiPlanner(FakeProvider { raw })
        val result = planner.plan(PlanningRequest("x", "com.alightmotion.motion"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PlanningError.SchemaViolation)
    }

    @Test
    fun invalidActionParametersAreRejected() = runTest {
        val raw = """{"task":"x","actions":[{"id":"a1","type":"OPEN_APP","parameters":{}}]}"""
        val planner = AiPlanner(FakeProvider { raw })
        val result = planner.plan(PlanningRequest("x", "com.alightmotion.motion"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PlanningError.InvalidAction)
    }

    @Test
    fun coordinateTargetWithoutExplicitFallbackIsRejected() = runTest {
        val raw = """{"task":"x","actions":[{"id":"a1","type":"TAP",
            "target":{"coordinateX":100,"coordinateY":200}}]}""".replace("\n", " ")
        val planner = AiPlanner(FakeProvider { raw })
        val result = planner.plan(PlanningRequest("x", "com.alightmotion.motion"))
        assertTrue(result.isFailure)
    }

    @Test
    fun oversizedActionTimeoutIsRejected() = runTest {
        val raw = """{"task":"x","actions":[{"id":"a1","type":"WAIT",
            "parameters":{"durationMs":"1000"},"timeoutMs":999999}]}""".replace("\n", " ")
        val planner = AiPlanner(FakeProvider { raw })
        val result = planner.plan(PlanningRequest("x", "com.alightmotion.motion"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PlanningError.SafetyViolation)
    }

    @Test
    fun providerFailureIsTypedProviderError() = runTest {
        val provider = object : AIProvider {
            override val name = "boom"
            override suspend fun complete(request: PlanningRequest): Result<String> =
                Result.failure(IllegalStateException("network down"))
        }
        val planner = AiPlanner(provider)
        val result = planner.plan(PlanningRequest("x", "com.alightmotion.motion"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PlanningError.ProviderError)
    }

    @Test
    fun planningTimeoutIsTypedTimeoutError() = runTest {
        val provider = FakeProvider { delay(60_000); validPlan }
        val planner = AiPlanner(provider, planningTimeoutMs = 1_000)
        val result = planner.plan(PlanningRequest("x", "com.alightmotion.motion"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PlanningError.Timeout)
    }

    @Test
    fun duplicateActionIdsAreRejected() = runTest {
        val raw = """{"task":"x","actions":[
            {"id":"a1","type":"WAIT","parameters":{"durationMs":"100"}},
            {"id":"a1","type":"WAIT","parameters":{"durationMs":"200"}}]}"""
        val planner = AiPlanner(FakeProvider { raw })
        val result = planner.plan(PlanningRequest("x", "com.alightmotion.motion"))
        assertTrue(result.isFailure)
    }

    @Test
    fun blankInstructionIsRejectedAtRequestConstruction() {
        var thrown = false
        try {
            PlanningRequest("  ", "com.alightmotion.motion")
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
