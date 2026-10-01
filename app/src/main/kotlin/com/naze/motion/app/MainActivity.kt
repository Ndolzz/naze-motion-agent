package com.naze.motion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.naze.motion.app.agent.AgentRuntime
import com.naze.motion.app.ui.NazeSplash
import com.naze.motion.app.ui.components.NazeDivider
import com.naze.motion.app.ui.components.NazeStatusLabel
import com.naze.motion.app.ui.screens.AgentDashboardScreen
import com.naze.motion.app.ui.screens.ExecutionScreen
import com.naze.motion.app.ui.screens.HistoryScreen
import com.naze.motion.app.ui.screens.SettingsScreen
import com.naze.motion.app.ui.screens.WorkflowDetailScreen
import com.naze.motion.app.ui.screens.WorkflowsScreen
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeMotionTheme
import com.naze.motion.app.ui.theme.NazeThemeMode
import com.naze.motion.app.ui.theme.NazeTypography
import com.naze.motion.core.adapter.TargetAdapterRegistry

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableStateOf(NazeThemeMode.DARK) }
            NazeMotionTheme(mode = themeMode) {
                NazeMotionApp(
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it },
                )
            }
        }
    }
}

/** Compact destinations reached from the workspace menu. */
private enum class Destination(val label: String) {
    AGENT("Agent"),
    WORKFLOWS("Workflows"),
    HISTORY("History"),
    SETTINGS("Settings"),
}

private fun workspaceLabel(destination: Destination?): String = when (destination) {
    Destination.AGENT -> "AGENT WORKSPACE"
    Destination.WORKFLOWS -> "SAVED WORKFLOWS"
    Destination.HISTORY -> "RUN HISTORY"
    Destination.SETTINGS -> "SETTINGS"
    null -> "EXECUTION"
}

private fun menuIconFor(destination: Destination): ImageVector = when (destination) {
    Destination.AGENT -> Icons.Rounded.PlayArrow
    Destination.WORKFLOWS -> Icons.Rounded.List
    Destination.HISTORY -> Icons.Rounded.DateRange
    Destination.SETTINGS -> Icons.Rounded.Settings
}

@Composable
fun NazeMotionApp(
    themeMode: NazeThemeMode = NazeThemeMode.DARK,
    onThemeModeChange: (NazeThemeMode) -> Unit = {},
) {
    var destination by remember { mutableStateOf(Destination.AGENT) }
    var detailId by remember { mutableStateOf<Long?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }
    var splashVisible by remember { mutableStateOf(true) }
    // Real runtime state drives every screen (Phase 11 to 26, unchanged).
    val context = LocalContext.current
    val runtime = remember { AgentRuntime(context.applicationContext) }
    val executionActive by runtime.executionActive.collectAsState()
    val agentState by runtime.uiState.collectAsState()
    val currentIndex by runtime.currentStep.collectAsState()
    val statusConnected by runtime.statusConnected.collectAsState()
    val taskName by runtime.taskName.collectAsState()
    val planSteps by runtime.planSteps.collectAsState()
    val logLines by runtime.logLines.collectAsState()
    val history by runtime.history.collectAsState()
    val preflightError by runtime.preflightError.collectAsState()
    val reconnectOffer by runtime.reconnectOffer.collectAsState()
    val selectedTarget by runtime.selectedTarget.collectAsState()
    val savedWorkflows by runtime.savedWorkflows.collectAsState()
    val vocabularyAudit by runtime.vocabularyAudit.collectAsState()
    val auditRunning by runtime.auditRunning.collectAsState()
    val auditError by runtime.auditError.collectAsState()

    DisposableEffect(Unit) {
        onDispose { runtime.shutdown() }
    }

    // Android Back priority (PART 1): menu -> about dialog -> detail
    // screen -> default exit. The app no longer closes outright while
    // the user is inside a nested screen or an open overlay.
    BackHandler(enabled = detailId != null) { detailId = null }
    BackHandler(enabled = aboutOpen) { aboutOpen = false }
    BackHandler(enabled = menuOpen) { menuOpen = false }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NazeTopBar(
                title = "Naze Motion",
                subtitle = if (executionActive) "EXECUTION" else workspaceLabel(destination),
                statusConnected = statusConnected,
                onCloseExecution = if (executionActive) {
                    { runtime.closeExecution() }
                } else null,
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onNavigate = { destination = it; detailId = null },
                onOpenAbout = { aboutOpen = true },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
            )
            NazeDivider()
            Box(modifier = Modifier.weight(1f)) {
                // PART 1: subtle screen transitions (fade + small slide).
                val navKey = when {
                    executionActive -> "execution"
                    detailId != null -> "detail-" + detailId
                    else -> destination.name
                }
                AnimatedContent(
                    targetState = navKey,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 16 })
                            .togetherWith(fadeOut(tween(150)))
                    },
                ) { _ ->
                    when {
                        executionActive -> ExecutionScreen(
                            taskName = taskName,
                            state = agentState,
                            currentIndex = currentIndex,
                            timeline = planSteps,
                            logEntries = logLines,
                            onStopAgent = { runtime.stop() },
                        )
                        detailId != null && history.firstOrNull { it.id == detailId } != null ->
                            WorkflowDetailScreen(
                                run = history.first { it.id == detailId },
                                onBack = { detailId = null },
                                onDeleteRun = {
                                    runtime.deleteRun(detailId!!)
                                    detailId = null
                                },
                                onRunAgain = { runtime.start(it) },
                            )
                        destination == Destination.AGENT -> AgentDashboardScreen(
                            runs = history,
                            onStartTask = { runtime.start(it) },
                            onOpenDetail = { detailId = it },
                            onOpenHistory = { destination = Destination.HISTORY },
                            preflightError = preflightError,
                            onDismissPreflight = { runtime.dismissPreflight() },
                            serviceConnected = statusConnected,
                            reconnectOffer = reconnectOffer,
                            onRerun = { runtime.rerunLast() },
                            onDismissReconnect = { runtime.dismissReconnectOffer() },
                            targetName = TargetAdapterRegistry.displayNameFor(selectedTarget),
                            targets = TargetAdapterRegistry.knownTargets,
                            selectedTargetPackage = selectedTarget,
                            onSelectTarget = { runtime.selectTarget(it) },
                            auditEntries = vocabularyAudit,
                            auditRunning = auditRunning,
                            auditError = auditError,
                            onAuditTarget = { runtime.auditSelectedTarget() },
                        )
                        destination == Destination.WORKFLOWS -> WorkflowsScreen(
                            saved = savedWorkflows,
                            runs = history,
                            onRunWorkflow = { runtime.startSavedWorkflow(it) },
                            onDeleteWorkflow = { runtime.deleteWorkflow(it) },
                            onSaveWorkflow = { runtime.saveWorkflowFromRun(it) },
                        )
                        destination == Destination.HISTORY -> HistoryScreen(
                            runs = history,
                            onOpenDetail = { detailId = it },
                            onDeleteAll = { runtime.clearHistory() },
                        )
                        destination == Destination.SETTINGS -> SettingsScreen()
                    }
                }
            }
        }
        if (aboutOpen) NazeAboutDialog(onClose = { aboutOpen = false })
        if (splashVisible) NazeSplash(onFinished = { splashVisible = false })
    }
}

/**
 * Workspace top bar: product name, current workspace label, live
 * connection status, and a single overflow menu replacing the old four
 * tab row. Icons come from one family; no emoji.
 */
@Composable
private fun NazeTopBar(
    title: String,
    subtitle: String,
    statusConnected: Boolean,
    onCloseExecution: (() -> Unit)?,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onNavigate: (Destination) -> Unit,
    onOpenAbout: () -> Unit,
    themeMode: NazeThemeMode,
    onThemeModeChange: (NazeThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
            Text(subtitle, style = NazeTypography.label, color = NazeColors.textMuted)
        }
        NazeStatusLabel(
            label = if (statusConnected) "Connected" else "Disconnected",
            color = if (statusConnected) NazeColors.success else NazeColors.error,
            icon = if (statusConnected) Icons.Rounded.PlayArrow else Icons.Rounded.Close,
        )
        if (onCloseExecution != null) {
            IconButton(onClick = onCloseExecution) {
                Icon(Icons.Rounded.Close, contentDescription = "Close execution")
            }
        }
        Box {
            IconButton(onClick = { onMenuOpenChange(true) }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Open workspace menu")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
                Destination.entries.forEach { dest ->
                    DropdownMenuItem(
                        text = { Text(dest.label) },
                        leadingIcon = { Icon(menuIconFor(dest), contentDescription = null) },
                        onClick = {
                            onMenuOpenChange(false)
                            onNavigate(dest)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("About") },
                    leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                    onClick = {
                        onMenuOpenChange(false)
                        onOpenAbout()
                    },
                )
                themeMenuItem("Theme: Dark", NazeThemeMode.DARK, themeMode, onMenuOpenChange, onThemeModeChange)
                themeMenuItem("Theme: Light", NazeThemeMode.LIGHT, themeMode, onMenuOpenChange, onThemeModeChange)
                themeMenuItem("Theme: System", NazeThemeMode.SYSTEM, themeMode, onMenuOpenChange, onThemeModeChange)
            }
        }
    }
}

@Composable
private fun themeMenuItem(
    label: String,
    mode: NazeThemeMode,
    current: NazeThemeMode,
    onMenuOpenChange: (Boolean) -> Unit,
    onThemeModeChange: (NazeThemeMode) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = {
            if (current == mode) Icon(Icons.Rounded.Check, contentDescription = null)
        },
        onClick = {
            onMenuOpenChange(false)
            onThemeModeChange(mode)
        },
    )
}

/** About dialog with the studio mark and the live package version. */
@Composable
private fun NazeAboutDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val version = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "unknown"
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = {
            TextButton(onClick = onClose) { Text("Close") }
        },
        title = { Text("Naze Motion") },
        text = {
            Column {
                Image(
                    painter = painterResource(R.drawable.naze_mark),
                    contentDescription = "Naze Motion mark",
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Precision motion agent for Alight Motion and CapCut.",
                    style = NazeTypography.body,
                )
                Spacer(Modifier.height(8.dp))
                Text("Version " + version, style = NazeTypography.caption)
            }
        },
    )
}
