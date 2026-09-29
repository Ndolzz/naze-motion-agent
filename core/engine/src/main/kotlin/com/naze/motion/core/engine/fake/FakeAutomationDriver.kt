package com.naze.motion.core.engine.fake

import com.naze.motion.core.action.AccessibilityTreeSnapshot
import com.naze.motion.core.action.ActionQuery
import com.naze.motion.core.action.AutomationDriver
import com.naze.motion.core.action.NodeHandle
import com.naze.motion.core.action.ResolutionMethod
import com.naze.motion.core.action.ResolvedTarget
import com.naze.motion.core.action.ScrollDirection
import com.naze.motion.core.action.ScreenCapture
import com.naze.motion.core.action.TargetResolver

/** Deterministic node handle for tests and previews. */
data class FakeNodeHandle(
    override val resourceId: String?,
    override val text: String?,
    override val contentDescription: String?,
    override val isClickable: Boolean = true,
) : NodeHandle

/** Snapshot value object so no anonymous object captures driver state. */
private data class FakeTreeSnapshot(
    override val nodeCount: Int,
    override val visibleText: List<String>,
    override val contentDescriptions: List<String>,
    override val clickableNodeCount: Int,
) : AccessibilityTreeSnapshot

private data class FakeScreenCapture(
    override val width: Int,
    override val height: Int,
    override val pixels: IntArray,
) : ScreenCapture

/**
 * FakeAutomationDriver (Phase 6). Fully deterministic, no Android
 * dependencies. Failures are scripted via the remaining counters so tests
 * assert exact retry and recovery behavior (NMA-TEST-002, NMA-TEST-004).
 */
class FakeAutomationDriver : AutomationDriver {
    var connected = true
    var currentPackage: String? = "com.alightmotion.motion"
    var visibleText: List<String> = emptyList()
    var contentDescriptions: List<String> = emptyList()
    var clickableNodeCount = 1

    var findFailuresRemaining = 0
    var clickFailuresRemaining = 0
    var longClickFailuresRemaining = 0
    var typeTextFailuresRemaining = 0
    var launchFailuresRemaining = 0

    /** Invoked on every accessibility tree read; use to script state changes. */
    var onTree: (() -> Unit)? = null

    val calls = mutableListOf<String>()

    override suspend fun find(query: ActionQuery): NodeHandle? {
        calls.add("find")
        if (findFailuresRemaining > 0) {
            findFailuresRemaining--
            return null
        }
        return FakeNodeHandle(query.resourceId, query.text, query.contentDescription, true)
    }

    override suspend fun click(node: NodeHandle): Boolean = scripted("click", clickFailuresRemaining) { clickFailuresRemaining-- }

    override suspend fun longClick(node: NodeHandle): Boolean = scripted("longClick", longClickFailuresRemaining) { longClickFailuresRemaining-- }

    override suspend fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long): Boolean {
        calls.add("swipe")
        return true
    }

    override suspend fun scroll(direction: ScrollDirection): Boolean {
        calls.add("scroll:" + direction.name)
        return true
    }

    override suspend fun typeText(text: String): Boolean = scripted("typeText", typeTextFailuresRemaining) { typeTextFailuresRemaining-- }

    override suspend fun pressBack(): Boolean {
        calls.add("pressBack")
        return true
    }

    override suspend fun launchApp(packageName: String): Boolean {
        calls.add("launchApp")
        if (launchFailuresRemaining > 0) {
            launchFailuresRemaining--
            return false
        }
        currentPackage = packageName
        return true
    }

    override suspend fun captureScreen(): ScreenCapture {
        calls.add("captureScreen")
        return FakeScreenCapture(1080, 1920, IntArray(1))
    }

    override suspend fun getCurrentPackage(): String? {
        calls.add("getCurrentPackage")
        return currentPackage
    }

    override suspend fun getAccessibilityTree(): AccessibilityTreeSnapshot {
        calls.add("getAccessibilityTree")
        onTree?.invoke()
        val text = visibleText
        val descriptions = contentDescriptions
        val clickable = clickableNodeCount
        return FakeTreeSnapshot(
            nodeCount = text.size + descriptions.size + clickable,
            visibleText = text,
            contentDescriptions = descriptions,
            clickableNodeCount = clickable,
        )
    }

    override fun isConnected(): Boolean = connected

    private fun scripted(name: String, failuresRemaining: Int, consume: () -> Unit): Boolean {
        calls.add(name)
        if (failuresRemaining > 0) {
            consume()
            return false
        }
        return true
    }
}

/**
 * FakeTargetResolver: deterministic resolution with an optional scripted
 * failure count for target not found retry tests.
 */
class FakeTargetResolver : TargetResolver {
    var failuresRemaining = 0

    override suspend fun resolve(query: ActionQuery): ResolvedTarget? {
        if (failuresRemaining > 0) {
            failuresRemaining--
            return null
        }
        return ResolvedTarget(
            node = FakeNodeHandle(query.resourceId, query.text, query.contentDescription, true),
            method = ResolutionMethod.RESOURCE_ID,
        )
    }
}
