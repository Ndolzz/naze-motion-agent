package com.naze.motion.app.agent

import android.content.Context
import android.content.Intent
import com.naze.motion.app.ui.components.TimelineItemState
import com.naze.motion.app.ui.model.AgentUiState
import com.naze.motion.core.access.AccessibilityConnection
import com.naze.motion.core.access.AccessibilityTargetResolver
import com.naze.motion.core.access.AndroidAccessibilityDriver
import com.naze.motion.core.adapter.AlightMotionAdapter
import com.naze.motion.core.adapter.TargetAdapterRegistry
import com.naze.motion.core.agent.AgentResult
import com.naze.motion.core.agent.MotionAgent
import com.naze.motion.core.ai.AiPlanner
import com.naze.motion.core.domain.Action
import com.naze.motion.core.domain.ActionPlan
import com.naze.motion.core.domain.AgentCancellationToken
import com.naze.motion.core.engine.ExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AgentRuntime (Phase 11 to 20): the real wiring between the UI and the
 * core pipeline. The dashboard starts a real run through MotionAgent over
 * AndroidAccessibilityDriver, AccessibilityTargetResolver, and
 * AlightMotionAdapter. The planner provider is built from ApiKeyStore.
 * Every finished run is persisted to the local Room database together with
 * its plan timeline and log (Phase 17), and single runs or the whole
 * history can be deleted (Phase 16). The execution console renders a live
 * timeline built from the validated plan and updated from the structured
 * engine log (Phase 14). Phase 19 adds a preflight check before every
 * run (service connected, target installed), and Phase 20 auto launches
 * the target app so the run always starts on a ready screen. Phase 22 applies the
 * configurable safety profile (action timeout cap, retry cap, recovery
 * bounds) from Settings to every run. Phase 24 resolves the target
 * application through TargetAdapterRegistry instead of a hardcoded name,
 * and watches the accessibility connection during a run: when the link
 * drops and later reconnects, the interrupted instruction is offered for
 * a one tap re-run.
 */
class AgentRuntime(context: Context) {

    private val appContext = context.applicationContext
    private val store = ApiKeyStore(context)
    private val safety = SafetySettingsStore(context)
    private val allowedApps = AllowedAppsStore(context)
    private val dao = HistoryDatabase.get(context).agentRunDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var token: AgentCancellationToken? = null

    @Volatile private var runLive = false
    @Volatile private var connectionLostDuringRun = false
    private var monitorJob: Job? = null

    val executionActive = MutableStateFlow(false)
    val uiState = MutableStateFlow<AgentUiState>(AgentUiState.Idle)
    val currentStep = MutableStateFlow(0)
    val statusConnected = MutableStateFlow(false)
    val taskName = MutableStateFlow("")
    val logLines = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val planSteps = MutableStateFlow<List<Pair<String, TimelineItemState>>>(emptyList())
    val history = MutableStateFlow<List<AgentRunUi>>(emptyList())
    val preflightError = MutableStateFlow<String?>(null)
    val reconnectOffer = MutableStateFlow<String?>(null)

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
        // Phase 19: refuse the run early instead of failing halfway
        // through execution when the environment is not ready.
        val blocked = preflight()
        if (blocked != null) {
            preflightError.value = blocked
            return
        }
        preflightError.value = null
        reconnectOffer.value = null
        // Phase 20: bring the target app to the front so the run starts
        // on a ready screen. Launching an already open app simply focuses
        // it, so this is always safe to call after preflight.
        val launch = appContext.packageManager.getLaunchIntentForPackage(TARGET_PACKAGE)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { appContext.startActivity(launch) }
        }
        val cancellationToken = AgentCancellationToken()
        token = cancellationToken
        executionActive.value = true
        runLive = true
        connectionLostDuringRun = false
        taskName.value = clean
        uiState.value = AgentUiState.Planning
        currentStep.value = 0
        logLines.value = emptyList()
        planSteps.value = emptyList()
        statusConnected.value = AccessibilityConnection.connected

        // Phase 24: while the run is live, watch the accessibility link.
        // A drop is recorded so a later reconnect can offer a re-run.
        monitorJob = scope.launch {
            while (runLive) {
                val live = AccessibilityConnection.connected
                if (!live) {
                    connectionLostDuringRun = true
                }
                statusConnected.value = live
                delay(CONNECTION_POLL_MS)
            }
        }

        scope.launch {
            val driver = AndroidAccessibilityDriver()
            val resolver = AccessibilityTargetResolver(driver)
            val adapter = AlightMotionAdapter(driver)
            // Phase 22: the configurable safety profile caps action
            // timeout and retries and drives bounded recovery.
            val engine = ExecutionEngine(profile = safety.load())
            var stepIndex = 0
            engine.log().onEvent { event ->
                logLines.value = logLines.value + (formatTime(event.timestampMs) to event.type)
                when (event.type) {
                    "ACTION_COMPLETED" -> {
                        markStep(stepIndex, TimelineItemState.SUCCESS)
                        stepIndex = stepIndex + 1
                        currentStep.value = stepIndex
                        markStep(stepIndex, TimelineItemState.ACTIVE)
                    }
                    "RECOVERY_STARTED" ->
                        markStep(stepIndex, TimelineItemState.RECOVERING)
                    "RECOVERY_COMPLETED" ->
                        markStep(stepIndex, TimelineItemState.ACTIVE)
                    "EXECUTION_FAILED" -> markStep(stepIndex, TimelineItemState.FAILED)
                }
            }
            val agent = MotionAgent(AiPlanner(store.activeProvider()), adapter, engine)
            val result = agent.run(clean, driver, resolver, cancellationToken) { plan ->
                planSteps.value = plan.actions.mapIndexed { index, action ->
                    describe(action) to
                        if (index == 0) TimelineItemState.ACTIVE
                        else TimelineItemState.PENDING
                }
            }
            applyResult(clean, result)
            runLive = false
            monitorJob?.cancel()
            monitorJob = null
            statusConnected.value = AccessibilityConnection.connected
            // Phase 24: when the accessibility link dropped mid-run and
            // the service is connected again, keep the instruction and
            // tell the user the run can be retried with one tap.
            if (connectionLostDuringRun && AccessibilityConnection.connected) {
                reconnectOffer.value = clean
                preflightError.value =
                    "The accessibility service disconnected during the run and " +
                        "has reconnected. The run can be retried with the same " +
                        "instruction using Run again."
            }
        }
    }

    /**
     * Phase 19 preflight: returns a human readable reason when the run
     * must be refused, or null when the environment is ready. Checks that
     * MotionAccessibilityService is connected and that the target
     * application is installed on this device.
     */
    private fun preflight(): String? {
        statusConnected.value = AccessibilityConnection.connected
        if (!AccessibilityConnection.connected) {
            return "Motion accessibility service is off. " +
                "Enable it in system accessibility settings, then try again."
        }
        // Phase 24: the target name comes from the registry, so a second
        // target application never needs hardcoded strings here.
        val targetLabel = TargetAdapterRegistry.displayNameFor(TARGET_PACKAGE)
        val targetInstalled = runCatching {
            appContext.packageManager.getPackageInfo(TARGET_PACKAGE, 0)
        }.isSuccess
        if (!targetInstalled) {
            return targetLabel + " is not installed on this device, " +
                "so the run has nowhere to execute."
        }
        // Phase 23: the target must be on the allowed apps list.
        if (!allowedApps.isAllowed(TARGET_PACKAGE)) {
            return targetLabel + " is not on the allowed applications " +
                "list. Allow it in Settings to run."
        }
        return null
    }

    /** Dismisses the preflight banner shown by the dashboard (Phase 19). */
    fun dismissPreflight() {
        preflightError.value = null
    }

    /**
     * Phase 24: runs the instruction from the pending reconnect offer,
     * when one exists. Returns false when there is nothing to re-run or a
     * run is already active.
     */
    fun rerunLast(): Boolean {
        val instruction = reconnectOffer.value ?: return false
        if (executionActive.value) return false
        reconnectOffer.value = null
        start(instruction)
        return true
    }

    /** Phase 24: drops a pending reconnect offer without re-running. */
    fun dismissReconnectOffer() {
        reconnectOffer.value = null
    }

    /** Refreshes the cached accessibility connection flag (Phase 16). */
    fun refreshConnection() {
        statusConnected.value = AccessibilityConnection.connected
    }

    /** Deletes one persisted run by id (Phase 16). */
    fun deleteRun(id: Long) {
        scope.launch { dao.deleteById(id) }
    }

    /** Deletes every persisted run (Phase 16). */
    fun clearHistory() {
        scope.launch { dao.clearAll() }
    }

    private fun markStep(index: Int, state: TimelineItemState) {
        val steps = planSteps.value
        if (index < 0 || index >= steps.size) return
        planSteps.value = steps.toMutableList().also { it[index] = it[index].first to state }
    }

    /** Short human readable label for one planned action. */
    private fun describe(action: Action): String {
        val target = action.target
        val detail = when {
            target?.normalizedText != null -> " " + target.normalizedText
            target?.text != null -> " " + target.text
            target?.contentDescription != null -> " " + target.contentDescription
            action.parameters.containsKey("packageName") ->
                " " + action.parameters["packageName"]
            action.parameters.containsKey("durationMs") ->
                " " + action.parameters["durationMs"] + "ms"
            else -> ""
        }
        return action.type.name + detail
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
                uiState.value = AgentUiState.Failed(
                    result.summary.error?.message ?: "execution failed",
                )
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
                uiState.value = AgentUiState.Failed(
                    "target app unavailable: " + result.packageName,
                )
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
                planText = planSteps.value.joinToString("\n") { it.first + "|" + it.second.name },
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
        runLive = false
        monitorJob?.cancel()
        monitorJob = null
        executionActive.value = false
        uiState.value = AgentUiState.Cancelled
    }

    fun shutdown() {
        token?.cancel()
        runLive = false
        monitorJob?.cancel()
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
        planSteps = planText.split('\n')
            .filter { it.isNotBlank() }
            .map { line ->
                val parts = line.split('|', limit = 2)
                if (parts.size == 2) {
                    parts[0] to (runCatching { TimelineItemState.valueOf(parts[1]) }
                        .getOrDefault(TimelineItemState.PENDING))
                } else {
                    line to TimelineItemState.PENDING
                }
            },
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

    companion object {
        /**
         * Resolved through the registry (Phase 24): adding a second target
         * application later only changes the registry, not this runtime.
         */
        private const val TARGET_PACKAGE = TargetAdapterRegistry.ALIGHT_MOTION_PACKAGE
        private const val CONNECTION_POLL_MS = 500L
    }
}
