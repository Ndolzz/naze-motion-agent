package com.naze.motion.core.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AgentStateMachineTest {

    private val machine = AgentStateMachine()

    @Test fun `happy path pipeline is legal`() { // NMA-STATE-002
        val m = AgentStateMachine()
        assertTrue(m.transitionTo(AgentState.PLANNING).isSuccess)
        assertTrue(m.transitionTo(AgentState.VALIDATING).isSuccess)
        assertTrue(m.transitionTo(AgentState.READY).isSuccess)
        assertTrue(m.transitionTo(AgentState.EXECUTING).isSuccess)
        assertTrue(m.transitionTo(AgentState.OBSERVING).isSuccess)
        assertTrue(m.transitionTo(AgentState.VERIFYING).isSuccess)
        assertTrue(m.transitionTo(AgentState.EXECUTING).isSuccess)
        assertEquals(AgentState.EXECUTING, m.current)
    }

    @Test fun `recovery path is legal`() {
        val m = AgentStateMachine(AgentState.VERIFYING)
        assertTrue(m.transitionTo(AgentState.RECOVERING).isSuccess)
        assertTrue(m.transitionTo(AgentState.OBSERVING).isSuccess)
    }

    @Test fun `terminal failure path is legal`() {
        val m = AgentStateMachine(AgentState.EXECUTING)
        assertTrue(m.transitionTo(AgentState.FAILED).isSuccess)
    }

    @Test fun `illegal transition is rejected`() { // NMA-STATE-003, NMA-TEST-012
        val result = machine.transitionTo(AgentState.EXECUTING) // IDLE -> EXECUTING
        assertTrue(result.isFailure)
        assertEquals(AgentState.IDLE, machine.current) // state unchanged
    }

    @Test fun `cancellation reachable from every non-terminal state`() { // NMA-STATE-004
        val nonTerminal = AgentState.entries.filter { !it.isTerminal }
        for (s in nonTerminal) {
            val m = AgentStateMachine(s)
            if (s == AgentState.IDLE) continue // IDLE has no work to cancel
            assertTrue(m.transitionTo(AgentState.CANCELLED).isSuccess, "cancel from $s failed")
        }
    }

    @Test fun `terminal states only reset to IDLE`() { // NMA-STATE-005
        for (t in listOf(AgentState.COMPLETED, AgentState.FAILED, AgentState.CANCELLED)) {
            val m = AgentStateMachine(t)
            assertTrue(m.transitionTo(AgentState.PLANNING).isFailure)
            assertTrue(m.reset().isSuccess)
            assertEquals(AgentState.IDLE, m.current)
        }
    }

    @Test fun `transition emits event`() { // NMA-STATE-006
        val events = mutableListOf<Pair<AgentState, AgentState>>()
        val m = AgentStateMachine(onTransition = { f, t -> events.add(f to t) })
        m.transitionTo(AgentState.PLANNING)
        assertEquals(listOf(AgentState.IDLE to AgentState.PLANNING), events)
    }

    @Test fun `full legal table matches spec`() {
        // spot-check a representative illegal pair from each state
        val illegal = mapOf(
            AgentState.PLANNING to AgentState.READY,
            AgentState.VALIDATING to AgentState.EXECUTING,
            AgentState.READY to AgentState.OBSERVING,
            AgentState.EXECUTING to AgentState.COMPLETED,
            AgentState.OBSERVING to AgentState.READY,
            AgentState.RECOVERING to AgentState.EXECUTING,
        )
        for ((from, to) in illegal) {
            assertTrue(!AgentTransitions.isLegal(from, to), "$from -> $to must be illegal")
        }
    }
}
