package com.naze.motion.core.domain

/** Observation snapshot (NMA-DOMAIN-007). Pure data — no Android types. */
data class Observation(
    val currentPackage: String?,
    val visibleText: List<String>,
    val contentDescriptions: List<String>,
    val screenState: ScreenState,
    val timestampMs: Long,
)

enum class ScreenState { UNKNOWN, HOME, TARGET_APP, OTHER_APP, LOCKED }

data class VerificationResult(
    val type: VerificationType,
    val passed: Boolean,
    val reason: String? = null,
)

sealed interface ActionResult {
    data class Success(val actionId: String, val durationMs: Long) : ActionResult
    data class Failure(
        val actionId: String,
        val error: AgentError,
        val attemptsUsed: Int,
        val durationMs: Long,
    ) : ActionResult

    /** Convenience: never assume success from a raw API boolean (NMA-DOMAIN-006). */
    fun isSuccess(): Boolean = this is Success
}

enum class AgentTaskStatus { PENDING, PLANNING, VALIDATING, EXECUTING, COMPLETED, FAILED, CANCELLED }

data class AgentTask(
    val id: String,
    val userInstruction: String,
    val actionPlan: ActionPlan? = null,
    val status: AgentTaskStatus = AgentTaskStatus.PENDING,
    val createdAtMs: Long,
    val executionResult: ActionResult? = null,
) {
    init {
        require(id.isNotBlank()) { "task id must not be blank" }
        require(userInstruction.isNotBlank()) { "user instruction must not be blank" }
    }
}

/** Validated external target application (NMA-DOMAIN-010, NMA-SEC-006). */
data class TargetApplication(
    val packageName: String,
    val displayName: String,
) {
    init {
        require(Regex("^[A-Za-z][A-Za-z0-9_.]*$").matches(packageName)) {
            "invalid package name: $packageName"
        }
    }
}

/**
 * Cancellation token for Emergency Stop (NMA-SEC-008/009).
 * Thread-safe; once cancelled it stays cancelled.
 */
class AgentCancellationToken {
    @Volatile private var cancelled = false
    val isCancelled: Boolean get() = cancelled
    fun cancel() { cancelled = true }
}

/** Per-task execution bookkeeping (NMA-DOMAIN-011). */
data class ExecutionContext(
    val taskId: String,
    val cancellationToken: AgentCancellationToken,
    val startedAtMs: Long,
    val completedActionIds: List<String> = emptyList(),
) {
    fun isCancelled(): Boolean = cancellationToken.isCancelled
}
