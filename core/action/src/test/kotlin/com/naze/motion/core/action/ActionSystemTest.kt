package com.naze.motion.core.action

import com.naze.motion.core.domain.Action
import com.naze.motion.core.domain.ActionTarget
import com.naze.motion.core.domain.AgentCancellationToken
import com.naze.motion.core.domain.ExecutionContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** Minimal in memory fakes for dispatcher tests. */
class FakeNode(
    override val resourceId: String? = null,
    override val text: String? = null,
    override val contentDescription: String? = null,
    override val isClickable: Boolean = true,
) : NodeHandle

class FakeDriver(var connected: Boolean = true) : AutomationDriver {
    var lastClickText: String? = null
    var launchedPackage: String? = null
    override suspend fun find(target: ActionQuery): NodeHandle? =
        nodes.firstOrNull { it.text == target.text || it.resourceId == target.resourceId }
    val nodes = mutableListOf<FakeNode>()
    override suspend fun click(node: NodeHandle): Boolean {
        lastClickText = node.text
        return true
    }
    override suspend fun longClick(node: NodeHandle): Boolean = true
    override suspend fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long): Boolean = true
    override suspend fun scroll(direction: ScrollDirection): Boolean = true
    override suspend fun typeText(text: String): Boolean = true
    override suspend fun pressBack(): Boolean = true
    override suspend fun launchApp(packageName: String): Boolean {
        launchedPackage = packageName
        return true
    }
    override suspend fun captureScreen(): ScreenCapture? = null
    override suspend fun getCurrentPackage(): String? = launchedPackage
    override suspend fun getAccessibilityTree(): AccessibilityTreeSnapshot? = null
    override fun isConnected(): Boolean = connected
}

class FakeResolver(private val driver: FakeDriver) : TargetResolver {
    override suspend fun resolve(query: ActionQuery): ResolvedTarget? {
        val node = driver.find(query) ?: return null
        return ResolvedTarget(node, ResolutionMethod.EXACT_TEXT, false)
    }
}

class ActionDispatcherTest {

    private val dispatcher = ActionDispatcher.withDefaultHandlers()

    @Test fun `all V1 action types are registered`() {
        val expected = setOf(
            "OPEN_APP", "WAIT", "TAP", "LONG_PRESS", "SWIPE", "SCROLL", "TYPE_TEXT",
            "PRESS_BACK", "SCREENSHOT", "FIND_ELEMENT", "CREATE_PROJECT", "ADD_MEDIA",
            "ADD_TEXT", "EXPORT",
        )
        assertEquals(expected, dispatcher.registeredTypes)
    }

    @Test fun `valid tap action dispatches and clicks target`() = runTest {
        val driver = FakeDriver()
        driver.nodes.add(FakeNode(text = "Create Project"))
        val action = Action("a1", com.naze.motion.core.domain.ActionType.TAP, target = ActionTarget(text = "Create Project"))
        val ctx = ExecutionContext("t1", AgentCancellationToken(), 0L)
        val result = dispatcher.dispatch(action, driver, FakeResolver(driver), ctx)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isSuccess())
        assertEquals("Create Project", driver.lastClickText)
    }

    @Test fun `invalid action is rejected before dispatch`() = runTest {
        val driver = FakeDriver()
        val action = Action(
            "a1",
            com.naze.motion.core.domain.ActionType.OPEN_APP,
            parameters = mapOf("packageName" to "../bad"),
        )
        val ctx = ExecutionContext("t1", AgentCancellationToken(), 0L)
        val result = dispatcher.dispatch(action, driver, FakeResolver(driver), ctx)
        assertTrue(result.isFailure)
    }

    @Test fun `unregistered action type is rejected`() = runTest {
        val empty = ActionDispatcher(emptyList())
        val driver = FakeDriver()
        val action = Action("a1", com.naze.motion.core.domain.ActionType.TAP, target = ActionTarget(text = "x"))
        val ctx = ExecutionContext("t1", AgentCancellationToken(), 0L)
        val result = empty.dispatch(action, driver, FakeResolver(driver), ctx)
        assertTrue(result.isFailure)
        val err = (result.exceptionOrNull() as ActionDispatcher.DispatchException).error
        assertEquals(com.naze.motion.core.domain.ErrorCode.UNKNOWN_ACTION_TYPE, err.code)
    }

    @Test fun `target not found yields typed failure`() = runTest {
        val driver = FakeDriver()
        val action = Action("a1", com.naze.motion.core.domain.ActionType.TAP, target = ActionTarget(text = "Missing"))
        val ctx = ExecutionContext("t1", AgentCancellationToken(), 0L)
        val result = dispatcher.dispatch(action, driver, FakeResolver(driver), ctx)
        assertTrue(result.isSuccess)
        val ar = result.getOrNull()!!
        assertTrue(ar is com.naze.motion.core.domain.ActionResult.Failure)
        assertEquals(com.naze.motion.core.domain.ErrorCode.TARGET_NOT_FOUND, (ar as com.naze.motion.core.domain.ActionResult.Failure).error.code)
    }

    @Test fun `cancellation prevents any action`() = runTest {
        val driver = FakeDriver()
        driver.nodes.add(FakeNode(text = "Create Project"))
        val token = AgentCancellationToken()
        val ctx = ExecutionContext("t1", token, 0L)
        token.cancel()
        val action = Action("a1", com.naze.motion.core.domain.ActionType.TAP, target = ActionTarget(text = "Create Project"))
        val result = dispatcher.dispatch(action, driver, FakeResolver(driver), ctx)
        assertTrue(result.isSuccess)
        val ar = result.getOrNull()!! as com.naze.motion.core.domain.ActionResult.Failure
        assertEquals(com.naze.motion.core.domain.ErrorCode.CANCELLED, ar.error.code)
        assertEquals(null, driver.lastClickText)
    }

    @Test fun `open app checks active package`() = runTest {
        val driver = FakeDriver()
        val action = Action(
            "a1",
            com.naze.motion.core.domain.ActionType.OPEN_APP,
            parameters = mapOf("packageName" to "com.alightmotion.motion"),
        )
        val ctx = ExecutionContext("t1", AgentCancellationToken(), 0L)
        val result = dispatcher.dispatch(action, driver, FakeResolver(driver), ctx)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isSuccess())
        assertEquals("com.alightmotion.motion", driver.launchedPackage)
    }

    @Test fun `driver disconnect yields typed failure`() = runTest {
        val driver = FakeDriver(connected = false)
        driver.nodes.add(FakeNode(text = "Create Project"))
        val action = Action("a1", com.naze.motion.core.domain.ActionType.TAP, target = ActionTarget(text = "Create Project"))
        val ctx = ExecutionContext("t1", AgentCancellationToken(), 0L)
        val result = dispatcher.dispatch(action, driver, FakeResolver(driver), ctx)
        val ar = result.getOrNull()!! as com.naze.motion.core.domain.ActionResult.Failure
        assertEquals(com.naze.motion.core.domain.ErrorCode.ACCESSIBILITY_DISCONNECTED, ar.error.code)
    }
}

class ActionValidatorTest {

    @Test fun `valid actions pass validation`() {
        val tap = Action("a1", com.naze.motion.core.domain.ActionType.TAP, target = ActionTarget(text = "ok"))
        assertTrue(ActionValidator.validate(tap).isSuccess)
        val wait = Action(
            "a2",
            com.naze.motion.core.domain.ActionType.WAIT,
            parameters = mapOf("durationMs" to "1000"),
        )
        assertTrue(ActionValidator.validate(wait).isSuccess)
    }

    @Test fun `wait without bounded duration is rejected`() {
        val bad = Action(
            "a1",
            com.naze.motion.core.domain.ActionType.WAIT,
            parameters = mapOf("durationMs" to "999999"),
        )
        assertTrue(ActionValidator.validate(bad).isFailure)
    }

    @Test fun `type text without text parameter is rejected`() {
        val bad = Action("a1", com.naze.motion.core.domain.ActionType.TYPE_TEXT, parameters = emptyMap())
        assertTrue(ActionValidator.validate(bad).isFailure)
    }

    @Test fun `tap without target is rejected`() {
        val bad = Action("a1", com.naze.motion.core.domain.ActionType.TAP)
        assertTrue(ActionValidator.validate(bad).isFailure)
    }

    @Test fun `screenshot path traversal is rejected`() {
        val bad = Action(
            "a1",
            com.naze.motion.core.domain.ActionType.SCREENSHOT,
            parameters = mapOf("filePath" to "../secret.png"),
        )
        assertTrue(ActionValidator.validate(bad).isFailure)
    }

    @Test fun `open app with invalid package is rejected`() {
        val bad = Action(
            "a1",
            com.naze.motion.core.domain.ActionType.OPEN_APP,
            parameters = mapOf("packageName" to "not a package"),
        )
        assertTrue(ActionValidator.validate(bad).isFailure)
    }
}
