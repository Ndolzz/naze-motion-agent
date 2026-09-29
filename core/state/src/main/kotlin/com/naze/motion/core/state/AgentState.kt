package com.naze.motion.core.state

/** Agent states (NMA-STATE-001). */
enum class AgentState {
    IDLE, PLANNING, VALIDATING, READY, EXECUTING, OBSERVING, VERIFYING,
    RECOVERING, COMPLETED, FAILED, CANCELLED;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == FAILED || this == CANCELLED
}

/** Legal transition table (NMA-STATE-002). Explicit: no dynamic invention. */
object AgentTransitions {
    private val legal: Map<AgentState, Set<AgentState>> = mapOf(
        AgentState.IDLE to setOf(AgentState.PLANNING),
        AgentState.PLANNING to setOf(AgentState.VALIDATING, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.VALIDATING to setOf(AgentState.READY, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.READY to setOf(AgentState.EXECUTING, AgentState.CANCELLED),
        AgentState.EXECUTING to setOf(AgentState.OBSERVING, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.OBSERVING to setOf(AgentState.VERIFYING, AgentState.FAILED, AgentState.CANCELLED),
        // COMPLETED is reachable from VERIFYING: a plan ends after its final action verifies.
        AgentState.VERIFYING to setOf(AgentState.EXECUTING, AgentState.RECOVERING, AgentState.COMPLETED, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.RECOVERING to setOf(AgentState.OBSERVING, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.COMPLETED to setOf(AgentState.IDLE),
        AgentState.FAILED to setOf(AgentState.IDLE),
        AgentState.CANCELLED to setOf(AgentState.IDLE),
    )

    fun isLegal(from: AgentState, to: AgentState): Boolean =
        legal[from]?.contains(to) == true

    fun legalTargets(from: AgentState): Set<AgentState> =
        legal[from] ?: emptySet()
}
