# Changelog

## [0.26.3] PHASE 26 HOTFIX
### Fixed
- The installed target detection now also scans the whole installed app list for the target vendor prefix (com.alightcreative for Alight Motion, com.lemon for CapCut), so any distribution variant of either app is found even when its exact package name is not in the known candidate list. "Not installed" preflight errors can no longer be caused by an unrecognized package variant.
### Changed
- app version 0.26.3.

## [0.26.2] PHASE 26 HOTFIX
### Fixed
- The installed app preflight now recognizes every known package variant of each target: Alight Motion (com.alightcreative.motion plus the direct download trial build com.alightcreative.motion.trial) and CapCut (com.lemon.lvoverseas plus the Chinese JianYing build com.lemon.lv). The registry exposes candidatesFor, adapters accept their variants in candidatePackages and match the foreground package and launches against all of them, and the runtime resolves the installed variant before every preflight, launch, and vocabulary audit. The allowlist accepts either the selected package or the installed variant, so "not installed" no longer appears when a non Play Store variant is the one actually installed.
### Changed
- app version 0.26.2.

package com.naze.motion.app.agentimport android.content.Contextimport android.content.Intentimport com.naze.motion.app.ui.components.TimelineItemStateimport com.naze.motion.app.ui.model.AgentUiStateimport com.naze.motion.core.access.AccessibilityConnectionimport com.naze.motion.core.access.AccessibilityTargetResolverimport com.naze.motion.core.access.AndroidAccessibilityDriverimport com.naze.motion.core.action.AutomationDriverimport com.naze.motion.core.adapter.AlightMotionAdapterimport com.naze.motion.core.adapter.CapCutAdapterimport com.naze.motion.core.adapter.TargetAdapterRegistryimport com.naze.motion.core.adapter.TargetApplicationAdapterimport com.naze.motion.core.adapter.TargetAuditorimport com.naze.motion.core.adapter.VocabularyAuditEntryimport com.naze.motion.core.agent.AgentResultimport com.naze.motion.core.agent.MotionAgentimport com.naze.motion.core.ai.AiPlannerimport com.naze.motion.core.domain.Actionimport com.naze.motion.core.domain.ActionPlanimport com.naze.motion.core.domain.AgentCancellationTokenimport com.naze.motion.core.engine.ExecutionEngineimport kotlinx.coroutines.CoroutineScopeimport kotlinx.coroutines.Dispatchersimport kotlinx.coroutines.Jobimport kotlinx.coroutines.SupervisorJobimport kotlinx.coroutines.cancelimport kotlinx.coroutines.delayimport kotlinx.coroutines.flow.MutableStateFlowimport kotlinx.coroutines.flow.collectLatestimport kotlinx.coroutines.launch
/**
 * AgentRuntime (Phase 11 to 26): the real wiring between the UI and the
 * core pipeline. The dashboard starts a real run through MotionAgent over
 * AndroidAccessibilityDriver, AccessibilityTargetResolver, and the adapter
 * of the selected target application. The planner provider is built from
 * ApiKeyStore. Every finished run is persisted to the local Room database
 * together with its plan timeline and log (Phase 17), and single runs or
 * the whole history can be deleted (Phase 16). The execution console
 * renders a live timeline built from the validated plan and updated from
 * the structured engine log (Phase 14). Phase 19 adds a preflight check
 * before every run (service connected, target installed), and Phase 20
 * auto launches the target app so the run always starts on a ready
 * screen. Phase 22 applies the configurable safety profile (action
 * timeout cap, retry cap, recovery bounds) from Settings to every run.
 * Phase 24 resolves the target application through TargetAdapterRegistry
 * and watches the accessibility connection during a run: when the link
 * drops and later reconnects, the interrupted instruction is offered for
 * a one tap re-run. Phase 26 makes the target application a per device
 * selection persisted through TargetSelectionStore: every run, preflight,
 * and launch follows the selection instead of a hardcoded package, a
 * finished run records which target it drove, and a run can be kept as a
 * saved workflow that restores its target on reuse. Phase 26 also adds
 * the on device vocabulary audit, which launches the selected target and
 * verifies every known UI element of its adapter against the real screen.
 */
class AgentRuntime(context: Context) {

    private val appContext = context.applicationContext
    private val store = ApiKeyStore(context)
    private val safety = SafetySettingsStore(context)
    private val allowedApps = AllowedAppsStore(context)
    private val targetSelection = TargetSelectionStore(context)
    private val database = HistoryDatabase.get(context)
    private val dao = database.agentRunDao()
    private val savedDao = database.savedWorkflowDao()
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

    // Phase 26: the selected target application, the saved workflows,
    // and the latest on device vocabulary audit of the selected target.
    val selectedTarget = MutableStateFlow(targetSelection.load())
    val savedWorkflows = MutableStateFlow<List<SavedWorkflowUi>>(emptyList())
    val vocabularyAudit = MutableStateFlow<List<VocabularyAuditEntry>?>(null)
    val auditRunning = MutableStateFlow(false)
    val auditError = MutableStateFlow<String?>(null)

    init {
        statusConnected.value = AccessibilityConnection.connected
        scope.launch {
            dao.observeRuns().collectLatest { runs ->
                history.value = runs.map { it.toUi() }
            }
        }
        scope.launch {
            savedDao.observeAll().collectLatest { workflows ->
                savedWorkflows.value = workflows.map { workflow ->
                    SavedWorkflowUi(
                        id = workflow.id,
                        instruction = workflow.instruction,
                        targetPackage = workflow.targetPackage,
                        createdAtMs = workflow.createdAtMs,
                    )
                }
            }
        }
    }

    /**
     * Phase 26: changes the target application the agent drives. The
     * store refuses packages the registry does not know, so the selection
     * can never point at an adapter that does not exist.
     */
    fun selectTarget(packageName: String): Boolean {
        if (!targetSelection.save(packageName)) return false
    
    selectedTarget.value = packageName
        return true
    }

    /**
     * Phase 26: keeps a finished instruction for one tap reuse, together
     * with the target application it drove. Returns false when there is
     * nothing worth saving.
     */
    fun saveWorkflowFromRun(id: Long): Boolean {
        val run = history.value.firstOrNull { it.id == id } ?: return false
        val clean = run.instruction.trim()
        if (clean.isEmpty()) return false
        scope.launch {
            savedDao.insert(
                SavedWorkflowEntity(
                    instruction = clean,
                    targetPackage = run.targetPackage,
                    createdAtMs = System.currentTimeMillis(),
                )
            )
        }
        return true
    }

    /** Phase 26: deletes one saved workflow by id. */
    fun deleteWorkflow(id: Long) {
        scope.launch { savedDao.deleteById(id) }
    }

    /**
     * Phase 26: starts a saved workflow, restoring the target
     * application it was saved for before the run begins. Returns false
     * when the workflow no longer exists or a run is already active.
     */
    fun startSavedWorkflow(id: Long): Boolean {
        if (executionActive.value) return false
        val workflow = savedWorkflows.value.firstOrNull { it.id == id }
            ?: return false
        selectTarget(workflow.targetPackage)
        start(workflow.instruction)
        return true
    }

    /**
     * Phase 26: audits the vocabulary of the selected target adapter on
     * the real device. Launches the target, gives the screen a moment to
     * settle, then resolves every known UI element and reports found or
     * not found per entry. The service must be connected and the target
     * installed; failures are reported through auditError.
     */
    fun auditSelectedTarget() {
        if (auditRunning.value) return
        if (executionActive.value) {
            auditError.value = "A run is active. Wait for it to finish before auditing."
            return
        }
        if (!AccessibilityConnection.connected) {
            auditError.value = "Motion accessibility service is off. " +
                "Enable it in system accessibility settings, then try again."
            return
        }
        val target = selectedTarget.value
        val targetLabel = TargetAdapterRegistry.displayNameFor(target)
        val targetInstalled = runCatching {
            appContext.packageManager.getPackageInfo(target, 0)
        }.isSuccess
        if (!targetInstalled) {
            auditError.value = targetLabel + " is not installed on this device, " +
                "so there is no screen to audit."
            return
        }
        auditError.value = null
        vocabularyAudit.value = null
        auditRunning.value = true
        scope.launch {
            try {
                val launch = appContext.packageManager.getLaunchIntentForPackage(target)
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { appContext.startActivity(launch) }
                }
                delay(AUDIT_SETTLE_MS)
                val driver = AndroidAccessibilityDriver()
                val adapter = adapterFor(target, driver)
                if (adapter == null) {
                    vocabularyAudit.value = null
                    auditError.value = "Unknown target application selected."
                } else {
                    vocabularyAudit.value = TargetAuditor.audit(adapter)
                }
            } catch (t: Throwable) {
                vocabularyAudit.value = null
                auditError.value = "Audit failed: " + (t.message ?: t.toString())
            } finally {
                auditRunning.value = false
            }
        }
    }

    /** Phase 26: dismisses the last audit error banner. */
    fun dismissAuditError() {
        auditError.value = null
    }

    fun start(instruction: String) {
        if (executionActive.value) return
        val clean = instruction.trim()
        if (clean.isEmpty()) return
        // Phase 26: the run drives the selected target application, not a
        // hardcoded package. The selection is validated by the store and
        // the registry, so preflight and launch follow it safely.
        val target = selectedTarget.value
        // Phase 19: refuse the run early instead of failing halfway
        // through execution when the environment is not ready.
        val blocked = preflight(target)
        if (blocked != null) {
            preflightError.value = blocked
            return
        }
        preflightError.value = null
        reconnectOffer.value = null
        // Phase 20: bring the target app to the front so the run starts
        // on a ready screen. Launching an already open app simply focuses
        // it, so this is always safe to call after preflight.
        val launch = appContext.packageManager.getLaunchIntentForPackage(target)
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
            val adapter = adapterFor(target, driver)
            if (adapter == null) {
                executionActive.value = false
                runLive = false
                monitorJob?.cancel()
                monitorJob = null
                uiState.value = AgentUiState.Idle
                preflightError.value = "Unknown target application selected."
                return@launch
            }
            val resolver = AccessibilityTargetResolver(driver)
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
                    describe
(action) to
                        if (index == 0) TimelineItemState.ACTIVE
                        else TimelineItemState.PENDING
                }
            }
            applyResult(clean, target, result)
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
     * Phase 26: the adapter of the selected target application, built on
     * the driver of the current run or audit. Unknown packages have no
     * adapter by definition; callers handle the null.
     */
    private fun adapterFor(
        packageName: String,
        driver: AutomationDriver,
    ): TargetApplicationAdapter? = when (packageName) {
        TargetAdapterRegistry.ALIGHT_MOTION_PACKAGE -> AlightMotionAdapter(driver)
        TargetAdapterRegistry.CAPCUT_PACKAGE -> CapCutAdapter(driver)
        else -> null
    }

    /**
     * Phase 19 preflight: returns a human readable reason when the run
     * must be refused, or null when the environment is ready. Checks that
     * MotionAccessibilityService is connected, that the target application
     * is installed on this device, and that it is allowed to run.
     */
    private fun preflight(target: String): String? {
        statusConnected.value = AccessibilityConnection.connected
        if (!AccessibilityConnection.connected) {
            return "Motion accessibility service is off. " +
                "Enable it in system accessibility settings, then try again."
        }
        val targetLabel = TargetAdapterRegistry.displayNameFor(target)
        val targetInstalled = runCatching {
            appContext.packageManager.getPackageInfo(target, 0)
        }.isSuccess
        if (!targetInstalled) {
            return targetLabel + " is not installed on this device, " +
                "so the run has nowhere to execute."
        }
        // Phase 23: the target must be on the allowed apps list.
        if (!allowedApps.isAllowed(target)) {
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

    private fun markStep(index
: Int, state: TimelineItemState) {
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

    private suspend fun applyResult(
        instruction: String,
        targetPackage: String,
        result: AgentResult,
    ) {
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
                targetPackage =
 targetPackage,
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
        targetPackage = targetPackage,
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
        private const val CONNECTION_POLL_MS = 500L
        /** Give the launched target a moment to settle before auditing. */
        private const val AUDIT_SETTLE_MS = 1_500L
    }
}
