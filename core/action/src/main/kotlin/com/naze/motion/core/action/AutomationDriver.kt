package com.naze.motion.core.action

/**
 * Automation contract. Domain and action layers know only this interface.
 * Implementations: FakeAutomationDriver (tests), AndroidAccessibilityDriver (Phase 7).
 */
interface AutomationDriver {
    suspend fun find(target: ActionQuery): NodeHandle?
    suspend fun click(node: NodeHandle): Boolean
    suspend fun longClick(node: NodeHandle): Boolean
    suspend fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long): Boolean
    suspend fun scroll(direction: ScrollDirection): Boolean
    suspend fun typeText(text: String): Boolean
    suspend fun pressBack(): Boolean
    suspend fun launchApp(packageName: String): Boolean
    suspend fun captureScreen(): ScreenCapture?
    suspend fun getCurrentPackage(): String?
    suspend fun getAccessibilityTree(): AccessibilityTreeSnapshot?
    fun isConnected(): Boolean
}

/** Semantic query for target resolution. Mirrors ActionTarget priorities. */
data class ActionQuery(
    val resourceId: String? = null,
    val contentDescription: String? = null,
    val text: String? = null,
    val normalizedText: String? = null,
    val coordinateX: Int? = null,
    val coordinateY: Int? = null,
    val coordinateFallbackAllowed: Boolean = false,
)

/** Opaque node handle. Android implementation wraps AccessibilityNodeInfo. */
interface NodeHandle {
    val resourceId: String?
    val text: String?
    val contentDescription: String?
    val isClickable: Boolean
}

interface ScreenCapture {
    val width: Int
    val height: Int
    val pixels: IntArray
}

interface AccessibilityTreeSnapshot {
    val nodeCount: Int
    val visibleText: List<String>
    val contentDescriptions: List<String>
    val clickableNodeCount: Int
}

enum class ScrollDirection { UP, DOWN, LEFT, RIGHT }
