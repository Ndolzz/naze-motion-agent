package com.naze.motion.core.action

import com.naze.motion.core.domain.Action
import com.naze.motion.core.domain.ActionResult
import com.naze.motion.core.domain.ErrorCode
import com.naze.motion.core.domain.ExecutionContext

/** Shared helpers for handlers. */
abstract class BaseActionHandler : ActionHandler {

    protected fun failure(action: Action, code: ErrorCode, reason: String, attempts: Int = 1): ActionResult.Failure =
        ActionResult.Failure(action.id, com.naze.motion.core.domain.AgentError(code, reason), attempts, 0L)

    protected fun success(actionId: String): ActionResult.Success =
        ActionResult.Success(actionId, 0L)

    protected fun checkCancelled(context: ExecutionContext): Boolean = context.isCancelled()

    protected suspend fun resolveTarget(
        action: Action,
        resolver: TargetResolver,
    ): ResolvedTarget? {
        val t = action.target ?: return null
        return resolver.resolve(
            ActionQuery(
                resourceId = t.resourceId,
                contentDescription = t.contentDescription,
                text = t.text,
                normalizedText = t.normalizedText,
                coordinateX = t.coordinateX,
                coordinateY = t.coordinateY,
                coordinateFallbackAllowed = t.coordinateFallbackAllowed,
            )
        )
    }
}

class OpenAppActionHandler : BaseActionHandler() {
    override val actionType = "OPEN_APP"
    override suspend fun execute(
        action: Action,
        driver: AutomationDriver,
        resolver: TargetResolver,
        context: ExecutionContext,
    ): ActionResult {
        if (checkCancelled(context)) return failure(action, ErrorCode.CANCELLED, "cancelled before OPEN_APP")
        if (!driver.isConnected()) return failure(action, ErrorCode.ACCESSIBILITY_DISCONNECTED, "driver disconnected")
        val pkg = action.parameters.getValue("packageName")
        val launched = driver.launchApp(pkg)
        if (!launched) return failure(action, ErrorCode.TARGET_APP_NOT_FOUND, "could not launch " + pkg)
        val current = driver.getCurrentPackage()
        return if (current == pkg) success(action.id)
        else failure(action, ErrorCode.PACKAGE_NOT_ACTIVE, "expected " + pkg + " but active is " + current)
    }
}

class WaitActionHandler : BaseActionHandler() {
    override val actionType = "WAIT"
    override suspend fun execute(
        action: Action,
        driver: AutomationDriver,
        resolver: TargetResolver,
        context: ExecutionContext,
    ): ActionResult {
        val durationMs = action.parameters.getValue("durationMs").toLong()
        val step = 100L
        var waited = 0L
        while (waited < durationMs) {
            if (checkCancelled(context)) return failure(action, ErrorCode.CANCELLED, "cancelled during WAIT")
            val chunk = minOf(step, durationMs - waited)
            kotlinx.coroutines.delay(chunk)
            waited += chunk
        }
        return success(action.id)
    }
}

class PressBackActionHandler : BaseActionHandler() {
    override val actionType = "PRESS_BACK"
    override suspend fun execute(
        action: Action,
        driver: AutomationDriver,
        resolver: TargetResolver,
        context: ExecutionContext,
    ): ActionResult {
        if (checkCancelled(context)) return failure(action, ErrorCode.CANCELLED, "cancelled before PRESS_BACK")
        return if (driver.pressBack()) success(action.id)
        else failure(action, ErrorCode.INTERNAL_ERROR, "pressBack returned false")
    }
}

class TypeTextActionHandler : BaseActionHandler() {
    override val actionType = "TYPE_TEXT"
    override suspend fun execute(
        action: Action,
        driver: AutomationDriver,
        resolver: TargetResolver,
        context: ExecutionContext,
    ): ActionResult {
        if (checkCancelled(context)) return failure(action, ErrorCode.CANCELLED, "cancelled before TYPE_TEXT")
        val text = action.parameters.getValue("text")
        return if (driver.typeText(text)) success(action.id)
        else failure(action, ErrorCode.INTERNAL_ERROR, "typeText returned false")
    }
}

class ScreenshotActionHandler : BaseActionHandler() {
    override val actionType = "SCREENSHOT"
    override suspend fun execute(
        action: Action,
        driver: AutomationDriver,
        resolver: TargetResolver,
        context: ExecutionContext,
    ): ActionResult {
        if (checkCancelled(context)) return failure(action, ErrorCode.CANCELLED, "cancelled before SCREENSHOT")
        val capture = driver.captureScreen()
            ?: return failure(action, ErrorCode.INTERNAL_ERROR, "captureScreen returned null")
        action.parameters["minWidth"]?.toIntOrNull()?.let { min ->
            if (capture.width < min) {
                return failure(action, ErrorCode.INTERNAL_ERROR, "capture width " + capture.width + " below minimum " + min)
            }
        }
        return success(action.id)
    }
}
