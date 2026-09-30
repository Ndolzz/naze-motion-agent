package com.naze.motion.app.agent

import android.content.Context
import com.naze.motion.app.ui.model.AgentUiState
import com.naze.motion.core.access.AccessibilityConnection
import com.naze.motion.core.access.AccessibilityTargetResolver
import com.naze.motion.core.access.AndroidAccessibilityDriver
import com.naze.motion.core.adapter.AlightMotionAdapter
import com.naze.motion.core.agent.AgentResult
import com.naze.motion.core.agent.MotionAgent
import com.naze.motion.core.ai.AiPlanner
import com.naze.motion.core.domain.AgentCancellationToken
import com.naze.motion.core.engine.ExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AgentRuntime (Phase 11/12/13): the real wiring between the UI and the
 * core pipeline. The dashboard starts a real run through MotionAgent over
 * AndroidAccessibilityDriver, AccessibilityTargetResolver, and
 * AlightMotionAdapter. The planner provider is built from ApiKeyStore.
 * Every finished run is persisted to the local Room database and History
 * screens render from that real data (Phase 13).
 */
class AgentRuntime(context: Context) {

    private val store = ApiKeyStore(context)
    private val dao = HistoryDatabase.get(context).agentRunDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var token: AgentCancellationToken? = null

    val executionActive = MutableStateFlow(false)
    val uiState = MutableStateFlow<AgentUiState>(AgentUiState.Idle)
    val currentStep = MutableStateFlow(0)
    val statusConnected = MutableStateFlow(false)
    val taskName = MutableStateFlow("")
    val logLines = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val history = MutableStateFlow<List<AgentRunUi>>(emptyList())

    init {
        statusConnected.value = AccessibilityConnection.connected
        scope.launch {
            dao.observeRuns().collectLatest { runs ->
                history.value = runs.map { it.toUi() }
            }
        }
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
            val agent = MotionAgent(AiPlanner(store.activeProvider()), adapter, engine)
            val result = agent.run(clean, driver, resolver, cancellationToken)
            applyResult(clean, result)
            statusConnected.value = AccessibilityConnection.connected
        }
    }

    private suspend fun applyResult(instruction: String, result: AgentResult) {
        val outcome: String
        val reason: String?
        val actionCount: Int
        val completedCount: Int
        val durationMs: Long
        when (result) {
            is AgentResult.Completed -> {
                currentStep.value = result.summary.completedActionIds.size
                uiState.value = AgentUiState.Completed
                outcome = "Completed"
                reason = null
                actionCount = result.summary.completedActionIds.size
                completedCount = result.summary.completedActionIds.size
                durationMs = result.summary.durationMs
            }
            is AgentResult.Failed -> {
                uiState.value = AgentUiState.Failed(result.summary.error?.message ?: "execution failed")
                outcome = "Failed"
                reason = result.summary.error?.message
                actionCount = result.summary.completedActionIds.size + 1
                completedCount = result.summary.completedActionIds.size
                durationMs = result.summary.durationMs
            }
            is AgentResult.Cancelled -> {
                uiState.value = AgentUiState.Cancelled
                outcome = "Cancelled"
                reason = result.summary.error?.message
                actionCount = result.summary.completedActionIds.size
                completedCount = result.summary.completedActionIds.size
                durationMs = result.summary.durationMs
            }
            is AgentResult.PlanningFailed -> {
                uiState.value = AgentUiState.Failed(result.error.message)
                outcome = "Failed"
                reason = "planning: " + result.error.message
                actionCount = 0
                completedCount = 0
                durationMs = 0
            }
            is AgentResult.TargetUnavailable -> {
                uiState.value = AgentUiState.Failed("target app unavailable: " + result.packageName)
                outcome = "Failed"
                reason = "target app unavailable: " + result.packageName
                actionCount = 0
                completedCount = 0
                durationMs = 0
            }
            is AgentResult.InvalidInstruction -> {
                uiState.value = AgentUiState.Failed(result.reason)
                outcome = "Failed"
                reason = result.reason
                actionCount = 0
                completedCount = 0
                durationMs = 0
            }
        }
        dao.insert(
            AgentRunEntity(
                instruction = instruction,
                outcome = outcome,
                reason = reason,
                actionCount = actionCount,
                completedCount = completedCount,
                durationMs = durationMs,
                endedAtMs = System.currentTimeMillis(),
                logText = logLines.value.joinToString("\n") { it.first + "|" + it.second },
            )
        )
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

    private fun AgentRunEntity.toUi(): AgentRunUi = AgentRunUi(
        id = id,
        instruction = instruction,
        outcome = outcome,
        reason = reason,
        actionCount = actionCount,
        completedCount = completedCount,
        durationMs = durationMs,
        endedAtMs = endedAtMs,
        logLines = logText.split('\n')
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split('|', limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else "" to line
            },
    )

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000) % 86_400
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds / 60) % 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }
}
