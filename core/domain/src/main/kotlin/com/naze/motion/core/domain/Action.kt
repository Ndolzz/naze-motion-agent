package com.naze.motion.core.domain

/** V1 action type allowlist (NMA-ACTION-002, NMA-SEC-004). */
enum class ActionType {
    OPEN_APP, WAIT, TAP, LONG_PRESS, SWIPE, SCROLL, TYPE_TEXT,
    PRESS_BACK, SCREENSHOT, FIND_ELEMENT, CREATE_PROJECT, ADD_MEDIA,
    ADD_TEXT, EXPORT,
}

/** Semantic target selector; coordinate fallback is explicit (NMA-ACTION-007/008). */
data class ActionTarget(
    val resourceId: String? = null,
    val contentDescription: String? = null,
    val text: String? = null,
    val normalizedText: String? = null,
    val coordinateX: Int? = null,
    val coordinateY: Int? = null,
    val coordinateFallbackAllowed: Boolean = false,
) {
    init {
        val hasSemantic = resourceId != null || contentDescription != null ||
            text != null || normalizedText != null
        val hasCoordinate = coordinateX != null && coordinateY != null
        require(hasSemantic || hasCoordinate) {
            "target must define a semantic selector or a full coordinate pair"
        }
        require(!hasCoordinate || coordinateFallbackAllowed) {
            "coordinate targets must explicitly allow coordinate fallback"
        }
    }
}

/** Retry policy — bounded, no infinite retry (NMA-DOMAIN-004). */
data class RetryPolicy(val maxAttempts: Int = 2) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
    }
}

enum class VerificationType {
    ELEMENT_VISIBLE, ELEMENT_NOT_VISIBLE, TEXT_EXISTS, TEXT_NOT_EXISTS,
    PACKAGE_ACTIVE, SCREEN_CHANGED, ACTION_COMPLETED, CUSTOM,
}

data class VerificationRule(
    val type: VerificationType,
    val target: ActionTarget? = null,
    val expectedText: String? = null,
    val expectedPackage: String? = null,
)

/**
 * Structured automation command (NMA-DOMAIN-002, NMA-ACTION-001).
 * Invariants enforced at construction (NMA-DOMAIN-003, NMA-DOMAIN-004).
 */
data class Action(
    val id: String,
    val type: ActionType,
    val target: ActionTarget? = null,
    val parameters: Map<String, String> = emptyMap(),
    val timeoutMs: Long = 5_000L,
    val retryPolicy: RetryPolicy = RetryPolicy(),
    val verification: VerificationRule? = null,
) {
    init {
        require(id.isNotBlank()) { "action id must not be blank" }
        require(timeoutMs > 0) { "timeoutMs must be > 0 (NMA-DOMAIN-003)" }
    }

    companion object {
        fun of(
            id: String,
            rawType: String,
            target: ActionTarget? = null,
            parameters: Map<String, String> = emptyMap(),
            timeoutMs: Long = 5_000L,
            retryPolicy: RetryPolicy = RetryPolicy(),
            verification: VerificationRule? = null,
        ): Result<Action> {
            val type = ActionType.entries.firstOrNull { it.name == rawType }
                ?: return Result.failure(
                    IllegalArgumentException("UNKNOWN_ACTION_TYPE: $rawType (NMA-ACTION-009)")
                )
            return runCatching { Action(id, type, target, parameters, timeoutMs, retryPolicy, verification) }
        }
    }
}

/** Ordered, non-empty plan with unique action ids (NMA-DOMAIN-005). */
data class ActionPlan(val actions: List<Action>) {
    init {
        require(actions.isNotEmpty()) { "plan must contain at least one action" }
        require(actions.map { it.id }.distinct().size == actions.size) {
            "action ids must be unique within a plan"
        }
    }
}
