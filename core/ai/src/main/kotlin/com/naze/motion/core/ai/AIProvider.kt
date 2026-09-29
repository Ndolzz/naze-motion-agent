package com.naze.motion.core.ai

/**
 * Provider abstraction (NMA-AI-003). Implementations translate a
 * PlanningRequest into raw structured JSON text. No vendor SDK types
 * leak into core; implementations live outside the domain.
 */
interface AIProvider {
    val name: String

    /**
     * Returns the raw model output. Must never throw: failures are typed
     * results (NMA-AI-008 requires callers to enforce timeout/cancellation).
     */
    suspend fun complete(request: PlanningRequest): Result<String>
}

/**
 * Planner input: user instruction plus observation summary and the target
 * app profile (NMA-AI inputs). No AccessibilityService, Activity, or
 * Android Context may appear here (NMA-AI-002).
 */
data class PlanningRequest(
    val instruction: String,
    val targetAppPackage: String,
    val observationSummary: String = "",
) {
    init {
        require(instruction.isNotBlank()) { "instruction must not be blank" }
    }
}

/** Typed planning failures (NMA-ERR, NMA-AI-005). */
sealed class PlanningError(
    val code: com.naze.motion.core.domain.ErrorCode,
    override val message: String,
) : Exception(message) {
    class ProviderError(message: String) : PlanningError(
        com.naze.motion.core.domain.ErrorCode.PLANNING_FAILED, message)
    class Timeout(message: String) : PlanningError(
        com.naze.motion.core.domain.ErrorCode.TIMEOUT, message)
    class MalformedJson(message: String) : PlanningError(
        com.naze.motion.core.domain.ErrorCode.INVALID_PLAN, message)
    class SchemaViolation(message: String) : PlanningError(
        com.naze.motion.core.domain.ErrorCode.INVALID_PLAN, message)
    class SafetyViolation(message: String) : PlanningError(
        com.naze.motion.core.domain.ErrorCode.INVALID_PLAN, message)
    class InvalidAction(message: String) : PlanningError(
        com.naze.motion.core.domain.ErrorCode.INVALID_ACTION, message)
}
