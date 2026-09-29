package com.naze.motion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var detailName by remember { mutableStateOf<String?>(null) }
    // Mock execution preview state. Real wiring arrives with the execution engine.
    var executionActive by remember { mutableStateOf(false) }
    var agentState by remember { mutableStateOf<AgentUiState>(AgentUiState.Idle) }
    var currentIndex by remember { mutableStateOf(2) }
    var onStop by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        NazeTopBar(
            title = "Naze Motion",
            statusConnected = true,
            onCloseExecution = if (executionActive) {
                { executionActive = false; agentState = AgentUiState.Cancelled }
            } else null,
        )
        NazeDivider()
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Destination.entries.forEach { dest ->
                Text(
                    text = dest.label,
                    style = NazeTypography.caption.copy(
                        color = if (destination == dest && !executionActive) NazeColors.primary else NazeColors.textMuted,
                        fontWeight = if (destination == dest) androidx.compose.ui.text.font.FontWeight.SemiBold
                        else androidx.compose.ui.text.font.FontWeight.Normal,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp)
                        .androidx.compose.foundation.clickable { destination = dest },
                )
            }
        }
        NazeDivider()

        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
            when {
                executionActive -> ExecutionScreen(
                    taskName = "Cinematic Intro",
                    state = agentState,
                    currentIndex = currentIndex,
                    onStopAgent = { executionActive = false; agentState = AgentUiState.Cancelled },
                )
                destination == Destination.AGENT -> AgentDashboardScreen(
                    onStartTask = { executionActive = true; agentState = AgentUiState.Executing },
                    onOpenHistory = { destination = Destination.HISTORY },
                )
                destination == Destination.HISTORY && detailName != null -> WorkflowDetailScreen(
                    name = detailName!!,
                    onBack = { detailName = null },
                )
                destination == Destination.HISTORY -> HistoryScreen(onOpenDetail = { detailName = it })
                destination == Destination.SETTINGS -> SettingsScreen()
                destination == Destination.WORKFLOWS -> HistoryScreen(onOpenDetail = { detailName = it })
            }
        }
    }
}

@Composable
private fun NazeTopBar(
    title: String,
    statusConnected: Boolean,
    onCloseExecution: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
