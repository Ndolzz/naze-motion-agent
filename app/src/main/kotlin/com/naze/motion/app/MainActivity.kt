package com.naze.motion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naze.motion.app.agent.AgentRuntime
import com.naze.motion.app.ui.components.NazeDivider
import com.naze.motion.app.ui.components.NazeStatusLabel
import com.naze.motion.app.ui.model.AgentUiState
import com.naze.motion.app.ui.screens.AgentDashboardScreen
import com.naze.motion.app.ui.screens.ExecutionScreen
import com.naze.motion.app.ui.screens.HistoryScreen
import com.naze.motion.app.ui.screens.SettingsScreen
import com.naze.motion.app.ui.screens.WorkflowDetailScreen
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeMotionTheme
import com.naze.motion.app.ui.theme.NazeTypography

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NazeMotionTheme {
                NazeMotionApp()
            }
        }
    }
}

/** Compact destinations. Top bar plus compact navigation. */
private enum class Destination(val label: String) {
    AGENT("Agent"),
    WORKFLOWS("Workflows"),
    HISTORY("History"),
    SETTINGS("Settings"),
}

@Composable
fun NazeMotionApp() {
    var destination by remember { mutableStateOf(Destination.AGENT) }
    var detailId by remember { mutableStateOf<Long?>(null) }
    // Phase 11 to 18: real runtime state replaces the mock execution preview.
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

    DisposableEffect(Unit) {
        onDispose { runtime.shutdown() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NazeTopBar(
            title = "Naze Motion",
            statusConnected = statusConnected,
            onCloseExecution = if (executionActive) {
                { runtime.closeExecution() }
            } else null,
        )
        NazeDivider()
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Destination.entries.forEach { dest ->
                NavItem(
                    label = dest.label,
                    selected = destination == dest && !executionActive,
                    onClick = { destination = dest; detailId = null },
                )
            }
        }
        NazeDivider()

        Box(modifier = Modifier.weight(1f)) {
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
                    )
                destination == Destination.AGENT -> AgentDashboardScreen(
                    runs = history,
                    onStartTask = { runtime.start(it) },
                    onOpenDetail = { detailId = it },
                    onOpenHistory = { destination = Destination.HISTORY },
                )
                destination == Destination.HISTORY || destination == Destination.WORKFLOWS ->
                    HistoryScreen(
                        runs = history,
                        onOpenDetail = { detailId = it },
                        onDeleteAll = { runtime.clearHistory() },
                    )
                destination == Destination.SETTINGS -> SettingsScreen()
            }
        }
    }
}

/** Nav item inside RowScope so weight resolves correctly. */
@Composable
private fun RowScope.NavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = NazeTypography.caption.copy(
            color = if (selected) NazeColors.primary else NazeColors.textMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        ),
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(12.dp),
    )
}

@Composable
private fun NazeTopBar(
    title: String,
    statusConnected: Boolean,
    onCloseExecution: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
        Spacer(Modifier.weight(1f))
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
    }
}
