package com.naze.motion.core.engine

import com.naze.motion.core.domain.AgentError

/** Whole-plan execution outcome. Terminal, never partially running. */
enum class ExecutionOutcome { COMPLETED, FAILED, CANCELLED }

/**
 * Structured result of a plan execution (NMA-DOMAIN-011).
 * completedActionIds are in execution order. log is the frozen event list.
 */
data class ExecutionSummary(
    val outcome: ExecutionOutcome,
    val taskId: String,
    val completedActionIds: List<String>,
    val failedActionId: String? = null,
    val error: AgentError? = null,
    val durationMs: Long,
    val log: List<EngineLogEvent>,
)
