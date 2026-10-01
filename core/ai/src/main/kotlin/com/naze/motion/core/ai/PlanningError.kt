package com.naze.motion.core.ai

/**
 * User-facing planning error types with human-readable messages.
 * Separates technical details from presentation concerns (NMA-AI-009).
 */
sealed class PlanningError(
    override val message: String,
    open val displayMessage: String = message,
) : Exception(message) {

    /** AI provider unreachable or network error. */
    data class NetworkError(
        override val message: String,
    ) : PlanningError(
        message,
        displayMessage = "Network error. Check your connection and API endpoint."
    )

    /** Provider quota exceeded or rate limited (429). */
    data class QuotaExceeded(
        override val message: String,
    ) : PlanningError(
        message,
        displayMessage = "API quota exceeded. Please wait a moment and try again."
    )

    /** Provider temporarily unavailable (5xx). */
    data class ProviderUnavailable(
        override val message: String,
    ) : PlanningError(
        message,
        displayMessage = "AI provider temporarily unavailable. Please try again in a moment."
    )

    /** Generic provider error (non-transient). */
    data class ProviderError(
        override val message: String,
    ) : PlanningError(
        message,
        displayMessage = "Planning failed. Please check your API key and try again."
    )

    /** Planning timed out. */
    data class Timeout(
        override val message: String,
    ) : PlanningError(
        message,
        displayMessage = "Planning took too long. Please try a simpler instruction."
    )

    /** Response from provider was malformed. */
    data class MalformedJson(
        override val message: String,
    ) : PlanningError(
        message,
        displayMessage = "AI returned invalid response. Please try again."
    )

    /** Response violates schema. */
    data class SchemaViolation(
        override val message: String,
    ) : PlanningError(
        message,
        displayMessage = "Invalid plan structure. Please try a different instruction."
    )

    /** One action in the plan is invalid. */
    data class InvalidAction(
        override val message: String,
    ) : PlanningError(
        message,
        displayMessage = "Plan contains invalid action. Please try again."
    )

    /** Safety validation failed. */
    data class SafetyViolation(
        override val message: String,
    ) : PlanningError(
        message,
        displayMessage = "Plan violates safety rules. Please try a different approach."
    )
}
