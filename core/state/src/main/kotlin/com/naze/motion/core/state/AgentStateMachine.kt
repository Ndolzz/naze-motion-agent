package com.naze.motion.core.state

/**
 * Explicit agent state machine with transition validation
 * (NMA-STATE-001..006). Emits structured StateTransitioned events.
 */
class AgentStateMachine(
    initial: AgentState = AgentState.IDLE,
    private val onTransition: ((from: AgentState, to: AgentState) -> Unit)? = null,
) {
    var current: AgentState = initial
        private set

    data class TransitionError(val from: AgentState, val to: AgentState) {
        val message = "ILLEGAL_STATE_TRANSITION: $from -> $to (NMA-STATE-003)"
    }

    /** Attempts a transition; returns typed error on illegal transition. */
    fun transitionTo(target: AgentState): Result<AgentState> {
        val from = current
        if (!AgentTransitions.isLegal(from, target)) {
            return Result.failure(IllegalStateException(TransitionError(from, target).message))
        }
        current = target
        onTransition?.invoke(from, target)
        return Result.success(target)
    }

    /** Terminal-state reset to IDLE (NMA-STATE-005). */
    fun reset(): Result<AgentState> = transitionTo(AgentState.IDLE)
}
