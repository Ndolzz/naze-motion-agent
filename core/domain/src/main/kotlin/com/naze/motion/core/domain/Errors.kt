package com.naze.motion.core.domain

/** Typed agent error taxonomy (NMA-ERR-001..005). */
enum class ErrorCode {
    PLANNING_FAILED,
    INVALID_PLAN,
    INVALID_ACTION,
    UNKNOWN_ACTION_TYPE,
    TARGET_NOT_FOUND,
    TIMEOUT,
    VERIFICATION_FAILED,
    RETRY_EXHAUSTED,
    RECOVERY_EXHAUSTED,
    PACKAGE_NOT_ACTIVE,
    ACCESSIBILITY_DISCONNECTED,
    SERVICE_NOT_ENABLED,
    ILLEGAL_STATE_TRANSITION,
    CANCELLED,
    TARGET_APP_NOT_FOUND,
    COORDINATE_FALLBACK_REJECTED,
    INTERNAL_ERROR,
}

data class AgentError(
    val code: ErrorCode,
    val message: String,
    val cause: AgentError? = null,
) {
    init {
        require(message.isNotBlank()) { "error message must not be blank" }
    }
}
