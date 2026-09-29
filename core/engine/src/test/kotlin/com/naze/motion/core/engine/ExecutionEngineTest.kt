package com.naze.motion.core.engine

import com.naze.motion.core.domain.Action
import com.naze.motion.core.domain.ActionPlan
import com.naze.motion.core.domain.ActionTarget
import com.naze.motion.core.domain.ActionType
import com.naze.motion.core.domain.AgentCancellationToken
import com.naze.motion.core.domain.ErrorCode
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

class ExecutionEngineTest {

    private val target = TargetApplication("com.alightmotion.motion", "Alight Motion")

    private fun machine(): AgentStateMachine = AgentStateMachine(AgentState.READY)

    private fun openApp(id: String) = Action(
        id = id,
        type = ActionType.OPEN_APP,
        parameters = mapOf("packageName" to target.packageName),
        verification = VerificationRule(
            type = VerificationType.PACKAGE_ACTIVE,
            expectedPackage = target.packageName,
        ),
    )

    private fun tap(id: String, retryPolicy: RetryPolicy = RetryPolicy()) = Action(
        id = id,
        type = ActionType.TAP,
        target = ActionTarget(resourceId = "btn_export"),
        retryPolicy = retryPolicy,
    )

    private fun tapVerified(id: String, visibleText: String) = Action(
        id = id,
        type = ActionType.TAP,
        target = ActionTarget(resourceId = "btn_" + visibleText),
        verification = VerificationRule(
            type = VerificationType.ELEMENT_VISIBLE,
            target = ActionTarget(text = visibleText),
        ),
    )

    private fun run(
        plan: ActionPlan,
        driver: FakeAutomationDriver,
        token: AgentCancellationToken = AgentCancellationToken(),
        m: AgentStateMachine = machine(),
        recovery: RecoveryManager = RecoveryManager(maxAttempts = 1),
    ): ExecutionSummary = runBlocking {
        val engine = ExecutionEngine(recoveryManager = recovery)
        engine.execute("task1", plan, driver, FakeTargetResolver(), target, token, m)
    }

    @Test fun happyPathCompletesPlan() {
        val driver = FakeAutomationDriver()
        val summary = run(ActionPlan(listOf(openApp("a1"), tap("a2"), tap("a3"))), driver)
        assertEquals(ExecutionOutcome.COMPLETED, summary.outcome)
        assertEquals(listOf("a1", "a2", "a3"), summary.completedActionIds)
    }

    @Test fun happyPathDrivesMachineToCompleted() {
        val driver = FakeAutomationDriver()
        val m = machine()
        run(ActionPlan(listOf(openApp("a1"), tap("a2"))), driver, m = m)
        assertEquals(AgentState.COMPLETED, m.current)
    }

    @Test fun rejectsMachineNotReady() {
        val driver = FakeAutomationDriver()
        val m = AgentStateMachine(AgentState.IDLE)
        val summary = run(ActionPlan(listOf(tap("a1"))), driver, m = m)
        assertEquals(ExecutionOutcome.FAILED, summary.outcome)
        assertEquals(ErrorCode.ILLEGAL_STATE_TRANSITION, summary.error?.code)
        assertEquals(AgentState.IDLE, m.current)
    }

    @Test fun failsFastWhenDriverDisconnected() {
        val driver = FakeAutomationDriver()
        driver.connected = false
        val m = machine()
        val summary = run(ActionPlan(listOf(tap("a1"))), driver, m = m)
        assertEquals(ExecutionOutcome.FAILED, summary.outcome)
        assertEquals(ErrorCode.ACCESSIBILITY_DISCONNECTED, summary.error?.code)
        assertEquals(AgentState.READY, m.current)
    }

    @Test fun cancelBeforeStartIsCancelled() {
        val driver = FakeAutomationDriver()
        val token = AgentCancellationToken()
        token.cancel()
        val m = machine()
        val summary = run(ActionPlan(listOf(tap("a1"))), driver, token = token, m = m)
        assertEquals(ExecutionOutcome.CANCELLED, summary.outcome)
        assertEquals(AgentState.CANCELLED, m.current)
    }

    @Test fun transientTargetFailureIsRetried() {
        val driver = FakeAutomationDriver()
        val resolver = FakeTargetResolver()
        resolver.failuresRemaining = 1
        val plan = ActionPlan(listOf(tap("a1", RetryPolicy(maxAttempts = 2))))
        val summary = runBlocking {
            ExecutionEngine().execute("task1", plan, driver, resolver, target, AgentCancellationToken(), machine())
        }
        assertEquals(ExecutionOutcome.COMPLETED, summary.outcome)
        assertEquals(listOf("a1"), summary.completedActionIds)
        assertTrue(summary.log.any { it.type == "ACTION_RETRYING" })
    }

    @Test fun verificationFailureRecoversAndCompletes() {
        val driver = FakeAutomationDriver()
        var treeCalls = 0
        driver.onTree = {
            treeCalls++
            if (treeCalls >= 2) driver.visibleText = listOf("Export")
        }
        val summary = run(ActionPlan(listOf(tapVerified("a1", "Export"))), driver)
        assertEquals(ExecutionOutcome.COMPLETED, summary.outcome)
        assertTrue(summary.log.any { it.type == "VERIFICATION_FAILED" })
        assertTrue(summary.log.any { it.type == "RECOVERY_COMPLETED" })
    }

    @Test fun recoveryExhaustionFailsThePlan() {
        val driver = FakeAutomationDriver()
        val summary = run(ActionPlan(listOf(tapVerified("a1", "Missing"))), driver)
        assertEquals(ExecutionOutcome.FAILED, summary.outcome)
        assertEquals(ErrorCode.RECOVERY_EXHAUSTED, summary.error?.code)
        assertEquals("a1", summary.failedActionId)
    }

    @Test fun slowActionTimesOut() {
        val driver = FakeAutomationDriver()
        val slow = Action(
            id = "w1",
            type = ActionType.WAIT,
            parameters = mapOf("durationMs" to "5000"),
            timeoutMs = 200L,
        )
        val m = machine()
        val summary = run(ActionPlan(listOf(slow)), driver, m = m)
        assertEquals(ExecutionOutcome.FAILED, summary.outcome)
        assertEquals(ErrorCode.TIMEOUT, summary.error?.code)
        assertEquals(AgentState.FAILED, m.current)
    }

    @Test fun cancellationMidPlanStopsAtBoundary() {
        val driver = FakeAutomationDriver()
        val token = AgentCancellationToken()
        driver.onTree = { token.cancel() }
        val m = machine()
        val summary = run(ActionPlan(listOf(openApp("a1"), tap("a2"))), driver, token = token, m = m)
        assertEquals(ExecutionOutcome.CANCELLED, summary.outcome)
        assertEquals(listOf("a1"), summary.completedActionIds)
        assertEquals(AgentState.CANCELLED, m.current)
    }

    @Test fun structuredLogCoversTheWholeRun() {
        val driver = FakeAutomationDriver()
        val summary = run(ActionPlan(listOf(openApp("a1"))), driver)
        val types = summary.log.map { it.type }
        assertTrue(types.contains("EXECUTION_STARTED"))
        assertTrue(types.contains("ACTION_STARTED"))
        assertTrue(types.contains("ACTION_COMPLETED"))
        assertTrue(types.contains("VERIFICATION_PASSED"))
        assertTrue(types.contains("EXECUTION_COMPLETED"))
        assertTrue(summary.log.all { it.taskId == "task1" })
    }
}
