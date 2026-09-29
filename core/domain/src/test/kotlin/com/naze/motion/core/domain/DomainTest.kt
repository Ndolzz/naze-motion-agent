package com.naze.motion.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ActionTest {
    @Test fun `valid action is accepted`() { // NMA-TEST-002
        val a = Action.of("a1", "TAP", target = ActionTarget(text = "Create Project"))
        assertTrue(a.isSuccess)
        assertEquals(ActionType.TAP, a.getOrNull()!!.type)
    }

    @Test fun `unknown action type is rejected`() { // NMA-TEST-004
        val a = Action.of("a1", "DO_MAGIC")
        assertTrue(a.isFailure)
    }

    @Test fun `timeout must be positive`() { // NMA-DOMAIN-003
        assertFailsWith<IllegalArgumentException> { Action("a1", ActionType.TAP, timeoutMs = 0) }
        assertFailsWith<IllegalArgumentException> { Action("a1", ActionType.TAP, timeoutMs = -5) }
    }

    @Test fun `retry policy must be bounded`() { // NMA-DOMAIN-004
        assertFailsWith<IllegalArgumentException> { RetryPolicy(maxAttempts = 0) }
        assertEquals(2, RetryPolicy().maxAttempts)
    }

    @Test fun `coordinate target requires explicit fallback flag`() { // NMA-ACTION-007
        assertFailsWith<IllegalArgumentException> {
            ActionTarget(coordinateX = 10, coordinateY = 20)
        }
        ActionTarget(coordinateX = 10, coordinateY = 20, coordinateFallbackAllowed = true)
    }

    @Test fun `blank action id is rejected`() {
        assertFailsWith<IllegalArgumentException> { Action("", ActionType.TAP) }
    }
}

class ActionPlanTest {
    private fun tap(id: String) = Action(id, ActionType.TAP, target = ActionTarget(text = "x"))

    @Test fun `plan must be non-empty`() { // NMA-DOMAIN-005
        assertFailsWith<IllegalArgumentException> { ActionPlan(emptyList()) }
    }

    @Test fun `plan action ids must be unique`() {
        assertFailsWith<IllegalArgumentException> { ActionPlan(listOf(tap("a"), tap("a"))) }
    }

    @Test fun `valid plan keeps order`() {
        val p = ActionPlan(listOf(tap("a"), tap("b")))
        assertEquals(listOf("a", "b"), p.actions.map { it.id })
    }
}

class AgentTaskTest {
    @Test fun `task requires instruction`() {
        assertFailsWith<IllegalArgumentException> {
            AgentTask(id = "t1", userInstruction = " ", createdAtMs = 0L)
        }
    }
}

class TargetApplicationTest {
    @Test fun `package names are validated`() { // NMA-DOMAIN-010, NMA-SEC-006
        assertFailsWith<IllegalArgumentException> { TargetApplication("../evil", "Evil") }
        TargetApplication("com.alightmotion.motion", "Alight Motion")
    }
}

class CancellationTokenTest {
    @Test fun `cancellation is sticky`() {
        val t = AgentCancellationToken()
        assertTrue(!t.isCancelled)
        t.cancel()
        assertTrue(t.isCancelled)
        t.cancel()
        assertTrue(t.isCancelled)
    }
}
