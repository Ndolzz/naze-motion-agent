package com.naze.motion.core.agent

import com.naze.motion.core.adapter.AlightMotionAdapter
import com.naze.motion.core.ai.AIProvider
import com.naze.motion.core.ai.AiPlanner
import com.naze.motion.core.ai.PlanningError
import com.naze.motion.core.ai.PlanningRequest
import com.naze.motion.core.domain.AgentCancellationToken
import com.naze.motion.core.domain.ErrorCode
import com.naze.motion.core.engine.fake.FakeAutomationDriver
import com.naze.motion.core.engine.fake.FakeTargetResolver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Deterministic provider double with a scripted raw output. */
private class FakeProvider(private val onComplete: suspend () -> String) : AIProvider {
    override val name = "fake"
    override suspend fun complete(request: PlanningRequest): Result<String> =
        runCatching { onComplete() }
}

class MotionAgentTest {

    private val planJson = """
        {"task":"create_video","actions":[
        {"id":"a1","type":"OPEN_APP","parameters":{"packageName":"com.alightmotion.motion"}},
        {"id":"a2","type":"TAP","target":{"normalizedText":"new project"}},
        {"id":"a3","type":"WAIT","parameters":{"durationMs":"100"}}]}
    """.trimIndent()

    private fun agent(driver: FakeAutomationDriver, provider: AIProvider): MotionAgent =
        MotionAgent(AiPlanner(provider), AlightMotionAdapter(driver), nowMs = { 1L })

    @Test
    fun completedRunReturnsCompletedWithFullSummary() = runTest {
        val driver = FakeAutomationDriver()
        val result = agent(driver, FakeProvider { planJson })
            .run("buat video baru", driver, FakeTargetResolver(), AgentCancellationToken())
        assertTrue(result is AgentResult.Completed)
        val completed = result as AgentResult.Completed
        assertEquals(3, completed.summary.completedActionIds.size)
        assertTrue(completed.summary.log.isNotEmpty())
    }

    @Test
    fun malformedProviderOutputSurfacesAsPlanningFailed() = runTest {
        val driver = FakeAutomationDriver()
        val result = agent(driver, FakeProvider { "this is not json" })
            .run("buat video", driver, FakeTargetResolver(), AgentCancellationToken())
        assertTrue(result is AgentResult.PlanningFailed)
        val failed = result as AgentResult.PlanningFailed
        assertTrue(failed.error is PlanningError.MalformedJson)
    }

    @Test
    fun blankInstructionIsRejectedBeforePlanning() = runTest {
        val driver = FakeAutomationDriver()
        val result = agent(driver, FakeProvider { planJson })
            .run("   ", driver, FakeTargetResolver(), AgentCancellationToken())
        assertTrue(result is AgentResult.InvalidInstruction)
    }

    @Test
    fun disconnectedDriverFailsWithTypedError() = runTest {
        val driver = FakeAutomationDriver()
        driver.connected = false
        val result = agent(driver, FakeProvider { planJson })
            .run("buat video", driver, FakeTargetResolver(), AgentCancellationToken())
        assertTrue(result is AgentResult.Failed)
        val failed = result as AgentResult.Failed
        assertEquals(ErrorCode.ACCESSIBILITY_DISCONNECTED, failed.summary.error?.code)
    }

    @Test
    fun cancellationBeforeRunReturnsCancelled() = runTest {
        val driver = FakeAutomationDriver()
        val token = AgentCancellationToken()
        token.cancel()
        val result = agent(driver, FakeProvider { planJson })
            .run("buat video", driver, FakeTargetResolver(), token)
        assertTrue(result is AgentResult.Cancelled)
    }

    @Test
    fun unavailableTargetAppReturnsTargetUnavailable() = runTest {
        val driver = FakeAutomationDriver()
        driver.currentPackage = "com.other.app"
        driver.launchFailuresRemaining = 1
        val result = agent(driver, FakeProvider { planJson })
            .run("buat video", driver, FakeTargetResolver(), AgentCancellationToken())
        assertTrue(result is AgentResult.TargetUnavailable)
    }

    @Test
    fun localTemplateProviderEndsToEndCompleted() = runTest {
        val driver = FakeAutomationDriver()
        val planner = AiPlanner(LocalTemplateProvider())
        val result = MotionAgent(planner, AlightMotionAdapter(driver), nowMs = { 1L })
            .run("buat project baru", driver, FakeTargetResolver(), AgentCancellationToken())
        assertTrue(result is AgentResult.Completed)
        val completed = result as AgentResult.Completed
        assertEquals(3, completed.summary.completedActionIds.size)
    }

    @Test
    fun localTemplateProviderOutputPassesPlanningValidation() = runTest {
        val planner = AiPlanner(LocalTemplateProvider())
        val plan = planner.plan(PlanningRequest("export video", "com.alightmotion.motion"))
        assertTrue(plan.isSuccess)
        assertEquals(3, plan.getOrThrow().actions.size)
    }
}
