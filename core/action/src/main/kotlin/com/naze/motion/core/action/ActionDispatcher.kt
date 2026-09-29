package com.naze.motion.core.action

import com.naze.motion.core.domain.Action
import com.naze.motion.core.domain.ActionResult
import com.naze.motion.core.domain.ErrorCode
import com.naze.motion.core.domain.ExecutionContext
import com.naze.motion.core.domain.AgentError

/**
 * Routes a validated Action to its handler. Unknown or unregistered types
 * are rejected, never guessed. Extensible: register handlers by type name.
 */
class ActionDispatcher(
    handlers: List<ActionHandler>,
    private val validator: Boolean = true,
) {
    private val registry: Map<String, ActionHandler> =
        handlers.associateBy { it.actionType }

    val registeredTypes: Set<String> get() = registry.keys

    suspend fun dispatch(
        action: Action,
        driver: AutomationDriver,
        resolver: TargetResolver,
        context: ExecutionContext,
    ): Result<ActionResult> {
        if (validator) {
            val validated = ActionValidator.validate(action)
            if (validated.isFailure) {
                val err = (validated.exceptionOrNull() as? ActionValidator.ValidationException)?.error
                    ?: AgentError(ErrorCode.INVALID_ACTION, "validation failed")
                return Result.failure(DispatchException(err))
            }
        }
        val handler = registry[action.type.name]
            ?: return Result.failure(
                DispatchException(AgentError(ErrorCode.UNKNOWN_ACTION_TYPE, "no handler for " + action.type.name))
            )
        val result = handler.execute(action, driver, resolver, context)
        return Result.success(result)
    }

    class DispatchException(val error: AgentError) : Exception(error.message)

    companion object {
        /** Default registry covering the full V1 allowlist. */
        fun withDefaultHandlers(): ActionDispatcher = ActionDispatcher(
            listOf(
                OpenAppActionHandler(),
                WaitActionHandler(),
                TapActionHandler(),
                LongPressActionHandler(),
                SwipeActionHandler(),
                ScrollActionHandler(),
                TypeTextActionHandler(),
                PressBackActionHandler(),
                ScreenshotActionHandler(),
                FindElementActionHandler(),
                CreateProjectActionHandler(),
                AddMediaActionHandler(),
                AddTextActionHandler(),
                ExportActionHandler(),
            )
        )
    }
}
