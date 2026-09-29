package com.naze.motion.core.action

import com.naze.motion.core.domain.Action
import com.naze.motion.core.domain.ActionType
import com.naze.motion.core.domain.ErrorCode
import com.naze.motion.core.domain.AgentError

/**
 * Validates an Action before dispatch. A malformed action is rejected, never
 * guessed or executed.
 */
object ActionValidator {

    private val packageNameRegex = Regex("^[A-Za-z][A-Za-z0-9_.]*$")

    fun validate(action: Action): Result<Action> {
        val error = typeSpecificError(action)
        return if (error != null) {
            Result.failure(ValidationException(error))
        } else {
            Result.success(action)
        }
    }

    private fun typeSpecificError(action: Action): AgentError? {
        val t = action.type
        val p = action.parameters
        return when (t) {
            ActionType.OPEN_APP -> {
                val pkg = p["packageName"]
                if (pkg.isNullOrBlank() || !packageNameRegex.matches(pkg)) {
                    AgentError(ErrorCode.INVALID_ACTION, "OPEN_APP requires a valid packageName parameter")
                } else null
            }
            ActionType.WAIT -> {
                val duration = p["durationMs"]?.toLongOrNull()
                if (duration == null || duration <= 0 || duration > 60_000L) {
                    AgentError(ErrorCode.INVALID_ACTION, "WAIT requires durationMs in 1..60000")
                } else null
            }
            ActionType.TYPE_TEXT -> {
                if (p["text"].isNullOrBlank()) {
                    AgentError(ErrorCode.INVALID_ACTION, "TYPE_TEXT requires a text parameter")
                } else null
            }
            ActionType.TAP, ActionType.LONG_PRESS, ActionType.FIND_ELEMENT -> {
                if (action.target == null) {
                    AgentError(ErrorCode.INVALID_ACTION, t.name + " requires a target")
                } else null
            }
            ActionType.SWIPE, ActionType.SCROLL -> {
                if (action.target == null && p["direction"].isNullOrBlank()) {
                    AgentError(ErrorCode.INVALID_ACTION, t.name + " requires a target or a direction parameter")
                } else null
            }
            ActionType.SCREENSHOT -> {
                val path = p["filePath"]
                if (path != null && (path.contains("..") || path.isBlank())) {
                    AgentError(ErrorCode.INVALID_ACTION, "SCREENSHOT filePath must not traverse directories")
                } else null
            }
            ActionType.PRESS_BACK,
            ActionType.CREATE_PROJECT,
            ActionType.ADD_MEDIA,
            ActionType.ADD_TEXT,
            ActionType.EXPORT -> null
        }
    }

    class ValidationException(val error: AgentError) : Exception(error.message)
}
