package com.naze.motion.core.action

import com.naze.motion.core.domain.Action
import com.naze.motion.core.domain.ActionResult
import com.naze.motion.core.domain.ErrorCode
import com.naze.motion.core.domain.ExecutionContext

/**
 * Handlers that resolve a target first. Semantic interaction is primary;
 * coordinate fallback is explicit and reported.
 */
abstract class TargetedActionHandler : BaseActionHandler() {
    abstract val verb: String
    abstract suspend fun onResolved(
        action: Action,
        target: ResolvedTarget,
        driver: AutomationDriver,
        context: ExecutionContext,
    ): ActionResult

    override suspend fun execute(
        action: Action,
        driver: AutomationDriver,
        resolver: TargetResolver,
        context: ExecutionContext,
    ): ActionResult {
        if (checkCancelled(context)) return failure(action, ErrorCode.CANCELLED, "cancelled before " + verb)
        if (!driver.isConnected()) return failure(action, ErrorCode.ACCESSIBILITY_DISCONNECTED, "driver disconnected")
        val resolved = resolveTarget(action, resolver)
            ?: return failure(action, ErrorCode.TARGET_NOT_FOUND, "target not found for " + verb)
        if (resolved.usedCoordinateFallback && !action.target!!.coordinateFallbackAllowed) {
            return failure(action, ErrorCode.COORDINATE_FALLBACK_REJECTED, "coordinate fallback not allowed")
        }
        return onResolved(action, resolved, driver, context)
    }
}

class TapActionHandler : TargetedActionHandler() {
    override val actionType = "TAP"
    override val verb = "TAP"
    override suspend fun onResolved(
        action: Action,
        target: ResolvedTarget,
        driver: AutomationDriver,
        context: ExecutionContext,
    ): ActionResult {
        if (!target.node.isClickable) return failure(action, ErrorCode.TARGET_NOT_FOUND, "target not clickable")
        return if (driver.click(target.node)) success(action.id)
        else failure(action, ErrorCode.INTERNAL_ERROR, "click returned false")
    }
}

class LongPressActionHandler : TargetedActionHandler() {
    override val actionType = "LONG_PRESS"
    override val verb = "LONG_PRESS"
    override suspend fun onResolved(
        action: Action,
        target: ResolvedTarget,
        driver: AutomationDriver,
        context: ExecutionContext,
    ): ActionResult {
        if (!target.node.isClickable) return failure(action, ErrorCode.TARGET_NOT_FOUND, "target not clickable")
        return if (driver.longClick(target.node)) success(action.id)
        else failure(action, ErrorCode.INTERNAL_ERROR, "longClick returned false")
    }
}

class FindElementActionHandler : TargetedActionHandler() {
    override val actionType = "FIND_ELEMENT"
    override val verb = "FIND_ELEMENT"
    override suspend fun onResolved(
        action: Action,
        target: ResolvedTarget,
        driver: AutomationDriver,
        context: ExecutionContext,
    ): ActionResult {
        // Finding the element is the success condition itself.
        return success(action.id)
    }
}

class ScrollActionHandler : BaseActionHandler() {
    override val actionType = "SCROLL"
    override suspend fun execute(
        action: Action,
        driver: AutomationDriver,
        resolver: TargetResolver,
        context: ExecutionContext,
    ): ActionResult {
        if (checkCancelled(context)) return failure(action, ErrorCode.CANCELLED, "cancelled before SCROLL")
        val direction = action.parameters["direction"]?.let { name ->
            ScrollDirection.entries.firstOrNull { it.name == name }
        } ?: ScrollDirection.DOWN
        return if (driver.scroll(direction)) success(action.id)
        else failure(action, ErrorCode.INTERNAL_ERROR, "scroll returned false")
    }
}

class SwipeActionHandler : BaseActionHandler() {
    override val actionType = "SWIPE"
    override suspend fun execute(
        action: Action,
        driver: AutomationDriver,
        resolver: TargetResolver,
        context: ExecutionContext,
    ): ActionResult {
        if (checkCancelled(context)) return failure(action, ErrorCode.CANCELLED, "cancelled before SWIPE")
        val startX = action.parameters["startX"]?.toIntOrNull() ?: 500
        val startY = action.parameters["startY"]?.toIntOrNull() ?: 1000
        val endX = action.parameters["endX"]?.toIntOrNull() ?: 500
        val endY = action.parameters["endY"]?.toIntOrNull() ?: 500
        val durationMs = action.parameters["durationMs"]?.toLongOrNull() ?: 300L
        return if (driver.swipe(startX, startY, endX, endY, durationMs)) success(action.id)
        else failure(action, ErrorCode.INTERNAL_ERROR, "swipe returned false")
    }
}

/**
 * Target app workflow handlers. V1 delegates to semantic targets inside the
 * target application. App specific selectors live in the target adapter
 * (Phase 8), never here.
 */
class CreateProjectActionHandler : TargetedActionHandler() {
    override val actionType = "CREATE_PROJECT"
    override val verb = "CREATE_PROJECT"
    override suspend fun onResolved(
        action: Action,
        target: ResolvedTarget,
        driver: AutomationDriver,
        context: ExecutionContext,
    ): ActionResult {
        if (!target.node.isClickable) return failure(action, ErrorCode.TARGET_NOT_FOUND, "target not clickable")
        return if (driver.click(target.node)) success(action.id)
        else failure(action, ErrorCode.INTERNAL_ERROR, "click returned false")
    }
}

class AddMediaActionHandler : TargetedActionHandler() {
    override val actionType = "ADD_MEDIA"
    override val verb = "ADD_MEDIA"
    override suspend fun onResolved(
        action: Action,
        target: ResolvedTarget,
        driver: AutomationDriver,
        context: ExecutionContext,
    ): ActionResult {
        if (!target.node.isClickable) return failure(action, ErrorCode.TARGET_NOT_FOUND, "target not clickable")
        return if (driver.click(target.node)) success(action.id)
        else failure(action, ErrorCode.INTERNAL_ERROR, "click returned false")
    }
}

class AddTextActionHandler : TargetedActionHandler() {
    override val actionType = "ADD_TEXT"
    override val verb = "ADD_TEXT"
    override suspend fun onResolved(
        action: Action,
        target: ResolvedTarget,
        driver: AutomationDriver,
        context: ExecutionContext,
    ): ActionResult {
        if (!target.node.isClickable) return failure(action, ErrorCode.TARGET_NOT_FOUND, "target not clickable")
        return if (driver.click(target.node)) success(action.id)
        else failure(action, ErrorCode.INTERNAL_ERROR, "click returned false")
    }
}

class ExportActionHandler : TargetedActionHandler() {
    override val actionType = "EXPORT"
    override val verb = "EXPORT"
    override suspend fun onResolved(
        action: Action,
        target: ResolvedTarget,
        driver: AutomationDriver,
        context: ExecutionContext,
    ): ActionResult {
        if (!target.node.isClickable) return failure(action, ErrorCode.TARGET_NOT_FOUND, "target not clickable")
        return if (driver.click(target.node)) success(action.id)
        else failure(action, ErrorCode.INTERNAL_ERROR, "click returned false")
    }
}
