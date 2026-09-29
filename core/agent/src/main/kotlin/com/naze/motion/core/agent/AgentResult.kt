package com.naze.motion.core.agent

import com.naze.motion.core.ai.PlanningError
import com.naze.motion.core.engine.ExecutionSummary

/**
 * Typed end to end result of one agent run. Terminal, never partially
 * running. Planning and target failures carry their own cases so the UI
 * never has to guess a reason (NMA-ERR-001..005).
 */
sealed class AgentResult {
    abstract val summary: ExecutionSummary?

    data class Completed(override val summary: ExecutionSummary) : AgentResult()
    data class Failed(override val summary: ExecutionSummary) : AgentResult()
    data class Cancelled(override val summary: ExecutionSummary) : AgentResult()

    data class PlanningFailed(val error: PlanningError) : AgentResult() {
        override val summary: ExecutionSummary? = null
    }

    data class TargetUnavailable(val packageName: String) : AgentResult() {
        override val summary: ExecutionSummary? = null
    }

    data class InvalidInstruction(val reason: String) : AgentResult() {
        override val summary: ExecutionSummary? = null
    }
}
