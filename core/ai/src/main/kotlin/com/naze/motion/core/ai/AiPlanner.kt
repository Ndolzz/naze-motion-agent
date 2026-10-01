package com.naze.motion.core.ai

import com.naze.motion.core.action.ActionValidator
import com.naze.motion.core.domain.ActionPlan
import kotlinx.coroutines.withTimeout

/**
 * AI planner (NMA-AI-001): maps Natural Language to a validated ActionPlan.
 * Pipeline: provider -> JSON parser -> schema validation (in parser) ->
 * safety validation -> per-action validation -> ActionPlan (NMA-AI-004).
 * Malformed or unsafe output is rejected, never guessed (NMA-AI-005).
 * Every provider call is bounded by a timeout (NMA-AI-008).
 * Errors are mapped to user-facing messages (NMA-UI-001).
 */
class AiPlanner(
    private val provider: AIProvider,
    private val planningTimeoutMs: Long = 30_000L,
    private val allowCoordinateFallback: Boolean = false,
) {

    suspend fun plan(request: PlanningRequest): Result<ActionPlan> {
        val raw = try {
            withTimeout(planningTimeoutMs) { provider.complete(request) }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            return Result.failure(
                PlanningError.Timeout("planning timed out after ${planningTimeoutMs}ms")
            )
        } catch (e: Exception) {
            return Result.failure(
                PlanningError.NetworkError(e.message ?: "network error")
            )
        }

        if (raw.isFailure) {
            val cause = raw.exceptionOrNull()
            val mapped = when (cause) {
                is PlanningError -> cause
                else -> PlanningError.ProviderError(cause?.message ?: "provider failed")
            }
            return Result.failure(mapped)
        }

        val parsed = PlannerJsonParser.parse(raw.getOrThrow())
        if (parsed.isFailure) {
            val err = parsed.exceptionOrNull() as? PlanningError
                ?: PlanningError.MalformedJson("parse failed")
            return Result.failure(err)
        }

        val (_, actions) = parsed.getOrThrow()

        val safety = SafetyValidator.validate(actions, allowCoordinateFallback)
        if (safety.isFailure) {
            val err = safety.exceptionOrNull() as? PlanningError
                ?: PlanningError.SafetyViolation("validation failed")
            return Result.failure(err)
        }

        for (action in actions) {
            val validated = ActionValidator.validate(action)
            if (validated.isFailure) {
                val err = (validated.exceptionOrNull() as ActionValidator.ValidationException).error
                return Result.failure(
                    PlanningError.InvalidAction("${action.id}: ${err.message}")
                )
            }
        }

        return runCatching { ActionPlan(actions) }
    }
}
