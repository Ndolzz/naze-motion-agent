package com.naze.motion.core.access

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import com.naze.motion.core.action.AccessibilityTreeSnapshot
import com.naze.motion.core.action.ActionQuery
import com.naze.motion.core.action.AutomationDriver
import com.naze.motion.core.action.NodeHandle
import com.naze.motion.core.action.ScrollDirection
import com.naze.motion.core.action.ScreenCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.function.Consumer
import kotlin.coroutines.resume

/** Node handle wrapping a live AccessibilityNodeInfo (NMA-ACCESS-004). */
class NodeHandleImpl(val node: AccessibilityNodeInfo) : NodeHandle {
    override val resourceId: String? get() = node.viewIdResourceName
    override val text: String? get() = node.text?.toString()
    override val contentDescription: String? get() = node.contentDescription?.toString()
    override val isClickable: Boolean get() = node.isClickable
}

/** Coordinate fallback handle: no node, gesture tap at explicit coordinates. */
class CoordinateNodeHandle(
    val x: Int,
    val y: Int,
) : NodeHandle {
    override val resourceId: String? = null
    override val text: String? = null
    override val contentDescription: String? = null
    override val isClickable: Boolean = true
}

private class TreeSnapshot(
    override val nodeCount: Int,
    override val visibleText: List<String>,
    override val contentDescriptions: List<String>,
    override val clickableNodeCount: Int,
) : AccessibilityTreeSnapshot

private class Screenshot(
    override val width: Int,
    override val height: Int,
    override val pixels: IntArray,
) : ScreenCapture

/**
 * AutomationDriver on top of MotionAccessibilityService (NMA-ACCESS-001/002).
 * All node interaction happens on the main dispatcher; failures return false
 * or null as typed results, never crash (NMA-ACCESS-006).
 */
class AndroidAccessibilityDriver : AutomationDriver {

    private val service: AccessibilityService?
        get() = AccessibilityConnection.service

    override fun isConnected(): Boolean = AccessibilityConnection.connected

    override suspend fun find(query: ActionQuery): NodeHandle? = withContext(Dispatchers.Main) {
        val svc = service ?: return@withContext null
        val root = svc.rootInActiveWindow ?: return@withContext null
        val collected = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(root, collected, MAX_TRAVERSAL_NODES)
        val resourceId = query.resourceId
        val description = query.contentDescription
        val text = query.text
        val normalized = query.normalizedText
        val match: (AccessibilityNodeInfo) -> Boolean = when {
            resourceId != null -> { node ->
                val viewId = node.viewIdResourceName
                viewId != null && (viewId == resourceId || viewId.endsWith(":id/" + resourceId))
            }
            description != null -> { node ->
                val d = node.contentDescription
                d != null && d.toString() == description
            }
            text != null -> { node ->
                val t = node.text
                t != null && t.toString() == text
            }
            normalized != null -> { node ->
                val t = node.text
                t != null && t.toString().trim().lowercase().contains(normalized.trim().lowercase())
            }
            else -> { _ -> false }
        }
        collected.firstOrNull(match)?.let { NodeHandleImpl(it) }
    }

    override suspend fun click(node: NodeHandle): Boolean = withContext(Dispatchers.Main) {
        when (node) {
            is NodeHandleImpl -> node.node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            is CoordinateNodeHandle -> dispatchTap(node.x, node.y)
            else -> false
        }
    }

    override suspend fun longClick(node: NodeHandle): Boolean = withContext(Dispatchers.Main) {
        when (node) {
            is NodeHandleImpl -> node.node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            is CoordinateNodeHandle -> dispatchLongPress(node.x, node.y)
            else -> false
        }
    }

    override suspend fun swipe(startX: Int, startY: Int, endX: Int, endY: Int, durationMs: Long): Boolean =
        withContext(Dispatchers.Main) {
            val path = Path()
            path.moveTo(startX.toFloat(), startY.toFloat())
            path.lineTo(endX.toFloat(), endY.toFloat())
            val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceAtLeast(16L))
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture)
        }

    override suspend fun scroll(direction: ScrollDirection): Boolean = withContext(Dispatchers.Main) {
        val svc = service ?: return@withContext false
        val root = svc.rootInActiveWindow ?: return@withContext false
        val collected = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(root, collected, MAX_TRAVERSAL_NODES)
        val scrollable = collected.firstOrNull { it.isScrollable } ?: return@withContext false
        // LEFT/RIGHT only exist as AccessibilityAction objects (API 23), not int constants.
        val action = when (direction) {
            ScrollDirection.DOWN -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            ScrollDirection.UP -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            ScrollDirection.LEFT -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT.id
            ScrollDirection.RIGHT -> AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT.id
        }
        scrollable.performAction(action)
    }

    override suspend fun typeText(text: String): Boolean = withContext(Dispatchers.Main) {
        val svc = service ?: return@withContext false
        val root = svc.rootInActiveWindow ?: return@withContext false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return@withContext false
        if (!focused.isEditable) return@withContext false
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    override suspend fun pressBack(): Boolean = withContext(Dispatchers.Main) {
        val svc = service ?: return@withContext false
        svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    override suspend fun launchApp(packageName: String): Boolean = withContext(Dispatchers.Main) {
        val svc = service ?: return@withContext false
        try {
            val intent = svc.packageManager.getLaunchIntentForPackage(packageName)
                ?: return@withContext false
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            svc.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun captureScreen(): ScreenCapture? = withContext(Dispatchers.Main) {
        val svc = service ?: return@withContext null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext null
        try {
            // API 34 signature: takeScreenshot(displayId, executor, consumer).
            val result = suspendCancellableCoroutine<AccessibilityService.ScreenshotResult> { cont ->
                svc.takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    svc.mainExecutor,
                    Consumer<AccessibilityService.ScreenshotResult> { r -> cont.resume(r) },
                )
            }
            val buffer = result.hardwareBuffer ?: return@withContext null
            val bitmap = Bitmap.createBitmap(buffer)
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            buffer.close()
            Screenshot(width, height, pixels)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getCurrentPackage(): String? = withContext(Dispatchers.Main) {
        service?.rootInActiveWindow?.packageName?.toString()
    }

    override suspend fun getAccessibilityTree(): AccessibilityTreeSnapshot? =
        withContext(Dispatchers.Main) {
            val svc = service ?: return@withContext null
            val root = svc.rootInActiveWindow ?: return@withContext null
            val collected = mutableListOf<AccessibilityNodeInfo>()
            collectNodes(root, collected, MAX_TRAVERSAL_NODES)
            val text = mutableListOf<String>()
            val descriptions = mutableListOf<String>()
            var clickable = 0
            for (node in collected) {
                node.text?.let { text.add(it.toString()) }
                node.contentDescription?.let { descriptions.add(it.toString()) }
                if (node.isClickable) clickable++
            }
            TreeSnapshot(collected.size, text, descriptions, clickable)
        }

    private suspend fun dispatchTap(x: Int, y: Int): Boolean {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        val stroke = GestureDescription.StrokeDescription(path, 0L, 40L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture)
    }

    private suspend fun dispatchLongPress(x: Int, y: Int): Boolean {
        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())
        val stroke = GestureDescription.StrokeDescription(path, 0L, 800L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture)
    }

    private suspend fun dispatchGesture(gesture: GestureDescription): Boolean =
        suspendCancellableCoroutine { cont ->
            val svc = service
            if (svc == null) {
                cont.resume(false)
                return@suspendCancellableCoroutine
            }
            svc.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    cont.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    cont.resume(false)
                }
            }, null)
        }

    private fun collectNodes(
        node: AccessibilityNodeInfo,
        into: MutableList<AccessibilityNodeInfo>,
        cap: Int,
    ) {
        if (into.size >= cap) return
        into.add(node)
        for (index in 0 until node.childCount) {
            if (into.size >= cap) return
            val child = node.getChild(index) ?: continue
            collectNodes(child, into, cap)
        }
    }

    companion object {
        /** Hard traversal cap so a pathological tree can never hang a read. */
        const val MAX_TRAVERSAL_NODES = 2000
    }
}
