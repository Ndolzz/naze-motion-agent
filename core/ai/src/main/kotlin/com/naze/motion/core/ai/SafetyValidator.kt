package com.naze.motion.core.ai

import com.naze.motion.core.domain.Action

/**
 * Safety gate before execution (NMA-AI-004 step 4, NMA-SEC).
 * Bounds plan size, per-action timeouts, and coordinate usage.
 */
object SafetyValidator {

    const val MAX_ACTIONS = 50
    const val MAX_ACTION_TIMEOUT_MS = 60_000L

    fun validate(actions: List<Action>, allowCoordinateFallback: Boolean = false): Result<Unit> {
        if (actions.isEmpty()) {
            return Result.failure(PlanningError.SchemaViolation("plan must contain at least one action"))
        }
        if (actions.size > MAX_ACTIONS) {
            return Result.failure(
                PlanningError.SafetyViolation("plan exceeds MAX_ACTIONS=" + MAX_ACTIONS)
            )
        }
        for (action in actions) {
            if (action.timeoutMs > MAX_ACTION_TIMEOUT_MS) {
                return Result.failure(
                    PlanningError.SafetyViolation(
                        "action " + action.id + " timeout exceeds " + MAX_ACTION_TIMEOUT_MS + "ms"
                    )
                )
            }
            val target = action.target
            if (target != null && target.coordinateX != null && target.coordinateY != null && !allowCoordinateFallback) {
                return Result.failure(
                    PlanningError.SafetyViolation(
                        "action " + action.id + " uses coordinates but coordinate fallback is not allowed"
                    )
                )
            }
        }
        return Result.success(Unit)
    }
}
