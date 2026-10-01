package com.naze.motion.app.agent

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.naze.motion.app.ui.model.AgentUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

/**
 * AgentRuntimeHolder (Phase 27): the most recently constructed AgentRuntime.
 * The overlay service runs in the same process as the runtime, so it can
 * mirror the runtime state flows without any IPC.
 */
object AgentRuntimeHolder {
    @Volatile var runtime: AgentRuntime? = null
}

/**
 * RunOverlayController (Phase 27): starts the floating progress card. The
 * call is always safe: without the overlay permission the service opens
 * the system grant screen once and stops, and the run is never blocked.
 */
object RunOverlayController {
    fun start(context: Context) {
        runCatching {
            context.startService(Intent(context, RunOverlayService::class.java))
        }
    }
}

/**
 * RunOverlayService (Phase 27): a small draggable floating card drawn over
 * the target application while a run is live. It mirrors the live plan
 * timeline and the structured log of the engine, so the user can watch the
 * run progress without leaving the target app. The card disappears a few
 * seconds after the run ends.
 */
class RunOverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var windowManager: WindowManager? = null
    private var card: LinearLayout? = null
    private var titleView: TextView? = null
    private var stepView: TextView? = null
    private var logView: TextView? = null
    private var attached = false
    private var finished = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        if (!Settings.canDrawOverlays(this)) {
            // One time grant flow: open the system screen, then stop. The
            // next run shows the card without further setup.
            runCatching {
                val grant = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_LINK,
                    Uri.parse("package:" + packageName),
                )
                grant.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(grant)
            }
            stopSelf()
            return START_NOT_STICKY
        }
        if (!attached) attachCard(wm)
        watchRuntime()
        return START_NOT_STICKY
    }

    private fun attachCard(wm: WindowManager) {
        val density = resources.displayMetrics.density
        val pad = (12 * density).toInt()
        val background = GradientDrawable().apply {
            setColor(Color.parseColor("#EE10151C"))
            cornerRadius = 16 * density
        }
        val title = TextView(this).apply {
            text = "Naze Motion"
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        titleView = title
        val step = TextView(this).apply {
            text = "PLANNING"
            setTextColor(Color.parseColor("#FF9FE8C2"))
            textSize = 13f
        }
        stepView = step
        val log = TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#CCE6E8EC"))
            textSize = 11f
        }
        logView = log
        val cancel = Button(this).apply {
            text = "Stop run"
            setAllCaps(false)
        }
        cancel.setOnClickListener { AgentRuntimeHolder.runtime?.stop() }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad / 2)
            setBackgroundDrawable(background)
            addView(title)
            addView(step)
            addView(log)
            addView(cancel)
        }
        card = root

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * density).toInt()
            y = (64 * density).toInt()
        }

        // Drag by the header so the card never covers a control the run
        // is about to tap.
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        title.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downX).toInt()
                    params.y = startY + (event.rawY - downY).toInt()
                    runCatching { wm.updateViewLayout(root, params) }
                    true
                }
                else -> false
            }
        }

        runCatching { wm.addView(root, params) }
        attached = true
    }

    private fun watchRuntime() {
        scope.launch {
            var runtime = AgentRuntimeHolder.runtime
            while (isActive && runtime == null) {
                delay(200)
                runtime = AgentRuntimeHolder.runtime
            }
            val rt = runtime ?: return@launch
            launch { rt.taskName.collectLatest { render(rt) } }
            launch { rt.planSteps.collectLatest { render(rt) } }
            launch { rt.logLines.collectLatest { render(rt) } }
            launch { rt.currentStep.collectLatest { render(rt) } }
            launch { rt.uiState.collectLatest { render(rt) } }
            launch {
                rt.executionActive.collectLatest { active ->
                    if (!active) finishSoon(rt)
                }
            }
        }
    }

    private fun render(rt: AgentRuntime) {
        if (finished) return
        val title = titleView ?: return
        val stepView = stepView ?: return
        val logView = logView ?: return
        title.text = rt.taskName.value.ifBlank { "Naze Motion" }
        val steps = rt.planSteps.value
        val current = rt.currentStep.value
        val state = rt.uiState.value
        val stepLine = when {
            state is AgentUiState.Failed -> "FAILED: " + state.reason
            state is AgentUiState.Completed -> "COMPLETED (" + steps.count { it.second == com.naze.motion.app.ui.components.TimelineItemState.SUCCESS } + "/" + steps.size + " steps)"
            steps.isEmpty() -> state.statusLabel
            else -> "Step " + (current + 1) + "/" + steps.size + " — " + (steps.getOrNull(current)?.first ?: "")
        }
        stepView.text = stepLine
        logView.text = rt.logLines.value.takeLast(5)
            .joinToString("\n") { it.first + " " + it.second }
    }

    private fun finishSoon(rt: AgentRuntime) {
        if (finished) return
        finished = true
        render(rt)
        scope.launch {
            delay(6000)
            detach()
            stopSelf()
        }
    }

    private fun detach() {
        val wm = windowManager ?: return
        val root = card ?: return
        runCatching { wm.removeView(root) }
        card = null
        attached = false
    }

    override fun onDestroy() {
        detach()
        scope.cancel()
        super.onDestroy()
    }
}
