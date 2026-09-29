package com.naze.motion.app.agent

import com.naze.motion.app.ui.model.AgentUiState
import com.naze.motion.core.access.AccessibilityConnection
import com.naze.motion.core.access.AccessibilityTargetResolver
import com.naze.motion.core.access.AndroidAccessibilityDriver
import com.naze.motion.core.adapter.AlightMotionAdapter
import com.naze.motion.core.agent.AgentResult
import com.naze.motion.core.agent.LocalTemplateProvider
import com.naze.motion.core.agent.MotionAgent
import com.naze.motion.core.ai.AiPlanner
import com.naze.motion.core.domain.AgentCancellationToken
import com.naze.motion.core.engine.ExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * AgentRuntime (Phase 11): the real wiring between the UI and the core
 * pipeline. Replaces the Phase 10 mock state: the dashboard starts a real
 * run through MotionAgent over AndroidAccessibilityDriver,
 * AccessibilityTargetResolver, AlightMotionAdapter, and AiPlanner backed by
 * LocalTemplateProvider. The structured engine log streams live into the
 * execution console.
 */
class AgentRuntime {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var token: AgentCancellationToken? = null

    val executionActive = MutableStateFlow(false)
    val uiState = MutableStateFlow<AgentUiState>(AgentUiState.Idle)
    val currentStep = MutableStateFlow(0)
    val statusConnected = MutableStateFlow(false)
    val taskName = MutableStateFlow("")
    val logLines = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    init {
        statusConnected.value = AccessibilityConnection.connected
    }

    fun start(instruction: String) {
        if (executionActive.value) return
        val clean = instruction.trim()
        if (clean.isEmpty()) return
        val cancellationToken = AgentCancellationToken()
        token = cancellationToken
        executionActive.value = true
        taskName.value = clean
        uiState.value = AgentUiState.Planning
        currentStep.value = 0
        logLines.value = emptyList()
        statusConnected.value = AccessibilityConnection.connected

        scope.launch {
            val driver = AndroidAccessibilityDriver()
            val resolver = AccessibilityTargetResolver(driver)
            val adapter = AlightMotionAdapter(driver)
            val engine = ExecutionEngine()
            engine.log().onEvent { event ->
                logLines.value = logLines.value + (formatTime(event.timestampMs) to event.type)
            }
            val agent = MotionAgent(AiPlanner(LocalTemplateProvider()), adapter, engine)
            val result = agent.run(clean, driver, resolver, cancellationToken)
            applyResult(result)
            statusConnected.value = AccessibilityConnection.connected
        }
    }

    private fun applyResult(result: AgentResult) {
        when (result) {
            is AgentResult.Completed -> {
                currentStep.value = result.summary.completedActionIds.size
                uiState.value = AgentUiState.Completed
            }
            is AgentResult.Failed ->
                uiState.value = AgentUiState.Failed(result.summary.error?.message ?: "execution failed")
            is AgentResult.Cancelled ->
                uiState.value = AgentUiState.Cancelled
            is AgentResult.PlanningFailed ->
                uiState.value = AgentUiState.Failed(result.error.message)
            is AgentResult.TargetUnavailable ->
                uiState.value = AgentUiState.Failed("target app unavailable: " + result.packageName)
            is AgentResult.InvalidInstruction ->
                uiState.value = AgentUiState.Failed(result.reason)
        }
    }

    /** Emergency Stop (NMA-SEC-008/009): cancel the run but keep the console open. */
    fun stop() {
        token?.cancel()
    }

    /** Close button: cancel if still running and dismiss the console. */
    fun closeExecution() {
        token?.cancel()
        executionActive.value = false
        uiState.value = AgentUiState.Cancelled
    }

    fun shutdown() {
        token?.cancel()
        scope.cancel()
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000) % 86_400
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds / 60) % 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
