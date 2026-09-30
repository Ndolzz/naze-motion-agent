package com.naze.motion.core.engine

import com.naze.motion.core.domain.Action
import com.naze.motion.core.domain.ActionPlan
import com.naze.motion.core.domain.ActionTarget
import com.naze.motion.core.domain.ActionType
import com.naze.motion.core.domain.AgentCancellationToken
import com.naze.motion.core.domain.ErrorCode
import com.naze.motion.core.domain.ExecutionProfile
import com.naze.motion.core.domain.RetryPolicy
import com.naze.motion.core.domain.TargetApplication
import com.naze.motion.core.domain.VerificationRule
import com.naze.motion.core.domain.VerificationType
import com.naze.motion.core.engine.fake.FakeAutomationDriver
import com.naze.motion.core.engine.fake.FakeTargetResolver
import com.naze.motion.core.state.AgentState
import com.naze.motion.core.state.AgentStateMachine
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 22: the configurable safety profile caps every run. An action can
 * never retry more or wait longer than the profile allows, and bounded
 * recovery comes from the profile when no manager is injected.
 */
class ExecutionEngineProfileTest {

    private val target = TargetApplication("com.alightmotion.motion", "Alight Motion")

    private fun machine(): AgentStateMachine = AgentStateMachine(AgentState.READY)

    @Test fun profileCapsRetryAttempts() {
        val driver = FakeAutomationDriver()
        val resolver = FakeTargetResolver()
        resolver.failuresRemaining = 10
        val action = Action(
            id = "a1",
            type = ActionType.TAP,
            target = ActionTarget(resourceId = "btn_export"),
            retryPolicy = RetryPolicy(maxAttempts = 5),
        )
        val summary = runBlocking {
            val engine = ExecutionEngine(profile = ExecutionProfile(maxAttempts = 2))
            engine.execute(
                "task1",
                ActionPlan(listOf(action)),
                driver,
                resolver,
                target,
                AgentCancellationToken(),
                machine(),
            )
        }
        assertEquals(ExecutionOutcome.FAILED, summary.outcome)
        assertEquals(ErrorCode.TARGET_NOT_FOUND, summary.error?.code)
        assertEquals(2, summary.log.count { it.type == "ACTION_ATTEMPT_FAILED" })
    }

    @Test fun profileCapsActionTimeout() {
        val driver = FakeAutomationDriver()
        val slow = Action(
            id = "w1",
            type = ActionType.WAIT,
            parameters = mapOf("durationMs" to "5000"),
            timeoutMs = 30_000L,
        )
        val summary = runBlocking {
            val engine = ExecutionEngine(profile = ExecutionProfile(actionTimeoutMs = 200L))
            engine.execute(
                "task1",
                ActionPlan(listOf(slow)),
                driver,
                FakeTargetResolver(),
                target,
                AgentCancellationToken(),
                machine(),
            )
        }
        assertEquals(ExecutionOutcome.FAILED, summary.outcome)
        assertEquals(ErrorCode.TIMEOUT, summary.error?.code)
        assertTrue(summary.log.any { it.type == "ACTION_ATTEMPT_FAILED" })
    }

    @Test fun profileDrivesRecoveryWhenNoManagerInjected() {
        val driver = FakeAutomationDriver()
        var treeCalls = 0
        driver.onTree = {
            treeCalls++
            if (treeCalls >= 4) driver.visibleText = listOf("Export")
        }
        val action = Action(
            id = "a1",
            type = ActionType.TAP,
            target = ActionTarget(resourceId = "btn_Export"),
            verification = VerificationRule(
                type = VerificationType.ELEMENT_VISIBLE,
                target = ActionTarget(text = "Export"),
            ),
        )
        val summary = runBlocking {
            val engine = ExecutionEngine(
                recoveryManager = null,
                profile = ExecutionProfile(recoveryMaxAttempts = 3, recoveryBackoffBaseMs = 0L),
            )
            engine.execute(
                "task1",
                ActionPlan(listOf(action)),
                driver,
                FakeTargetResolver(),
                target,
                AgentCancellationToken(),
                machine(),
            )
        }
        assertEquals(ExecutionOutcome.COMPLETED, summary.outcome)
        assertTrue(summary.log.any { it.type == "RECOVERY_COMPLETED" })
    }
}
