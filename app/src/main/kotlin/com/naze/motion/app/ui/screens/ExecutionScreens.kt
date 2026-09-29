package com.naze.motion.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.motion.app.ui.components.NazeActionTimeline
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.NazeLog
import com.naze.motion.app.ui.components.NazeProgress
import com.naze.motion.app.ui.components.NazeSection
import com.naze.motion.app.ui.components.NazeStatusLabel
import com.naze.motion.app.ui.model.AgentUiState
import com.naze.motion.app.ui.model.MockData
import com.naze.motion.app.ui.model.TimelineEntry
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeTypography

/**
 * Plan review screen. Nothing runs until the user confirms.
 */
@Composable
fun PlanningScreen(
    instruction: String,
    onRun: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(instruction, style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
        Spacer(Modifier.height(16.dp))
        NazeSection(title = "Plan") {
            NazeCard {
                NazeActionTimeline(
                    MockData.plan.map { label -> TimelineEntry(label, com.naze.motion.app.ui.components.TimelineItemState.PENDING) }.map { it.label to it.state }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Estimated actions: 6", style = NazeTypography.caption)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NazeButton(text = "Cancel", onClick = onCancel)
            NazeButton(text = "Run", onClick = onRun, isPrimary = true, leadingIcon = Icons.Rounded.Stop.takeIf { false })
        }
    }
}

/**
 * Precision Execution Console. Current action emphasized, completed muted,
 * pending subtle. Stop Agent always visible.
 */
@Composable
fun ExecutionScreen(
    taskName: String,
    state: AgentUiState,
    currentIndex: Int,
    onStopAgent: () -> Unit,
) {
    var logExpanded by remember { mutableStateOf(false) }
    val timeline = MockData.timeline(currentIndex)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text("Running workflow", style = NazeTypography.caption)
            Text(taskName, style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
        }
        item {
            NazeCard {
                Column {
                    timeline.forEach { entry ->
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        ) {
                            Text(
                                entry.label,
                                style = NazeTypography.body.copy(
                                    color = when (entry.state) {
                                        com.naze.motion.app.ui.components.TimelineItemState.SUCCESS -> NazeColors.textMuted
                                        com.naze.motion.app.ui.components.TimelineItemState.ACTIVE -> NazeColors.textPrimary
                                        else -> NazeColors.textDisabled
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
        item {
            NazeCard {
                Column {
                    Text("STATUS", style = NazeTypography.technical, color = NazeColors.textDisabled)
                    Spacer(Modifier.height(4.dp))
                    NazeStatusLabel(
                        label = state.statusLabel,
                        color = state.statusColor,
                        icon = state.statusIcon,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Current action", style = NazeTypography.caption, color = NazeColors.textMuted)
                    Text(timeline.getOrNull(currentIndex)?.label ?: "Finishing", style = NazeTypography.body.copy(color = NazeColors.textPrimary))
                }
            }
        }
        item {
            NazeSection(title = "Technical log") {
                if (logExpanded) {
                    NazeLog(MockData.log)
                } else {
                    NazeButton(text = "Expand log", onClick = { logExpanded = true })
                }
            }
        }
        item {
            NazeButton(
                text = "STOP AGENT",
                onClick = onStopAgent,
                modifier = Modifier.fillMaxWidth(),
                isPrimary = true,
                leadingIcon = Icons.Rounded.Stop,
            )
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

/** Recovery UI: small progress, attempt counter, no big animations. */
@Composable
fun RecoveryPanel(attempt: Int, maxAttempts: Int) {
    NazeCard {
        Column {
            NazeStatusLabel(label = "RECOVERING", color = NazeColors.warning, icon = Icons.Rounded.Refresh)
            Spacer(Modifier.height(8.dp))
            NazeProgress("Refreshing screen state. Attempt " + attempt + " of " + maxAttempts)
        }
    }
}

/** Error UI: always explains the reason. Never hides failure causes. */
@Composable
fun ErrorPanel(
    actionLabel: String,
    reason: String,
    recoveryNote: String,
    onRetry: () -> Unit,
    onEndTask: () -> Unit,
) {
    NazeCard {
        Column {
            Text("Action failed", style = NazeTypography.section, color = NazeColors.error)
            Spacer(Modifier.height(8.dp))
            Text("The " + actionLabel + " control could not be found.", style = NazeTypography.body)
            Spacer(Modifier.height(12.dp))
            Text("Attempted", style = NazeTypography.caption, color = NazeColors.textMuted)
            Text(actionLabel, style = NazeTypography.body.copy(color = NazeColors.textPrimary))
            Spacer(Modifier.height(12.dp))
            Text("Reason", style = NazeTypography.caption, color = NazeColors.textMuted)
            Text(reason, style = NazeTypography.body)
            Spacer(Modifier.height(12.dp))
            Text("Recovery", style = NazeTypography.caption, color = NazeColors.textMuted)
            Text(recoveryNote, style = NazeTypography.body)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NazeButton(text = "Retry", onClick = onRetry)
                NazeButton(text = "End Task", onClick = onEndTask)
            }
        }
    }
}
