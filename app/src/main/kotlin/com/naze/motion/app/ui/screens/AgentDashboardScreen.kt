package com.naze.motion.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.motion.app.agent.AgentRunUi
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.NazeEmptyState
import com.naze.motion.app.ui.components.NazeSection
import com.naze.motion.app.ui.components.NazeStatusLabel
import com.naze.motion.app.ui.components.NazeTextField
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeTypography

/**
 * Agent dashboard (Phase 18 to 21). Input is the focal point, not a wall of
 * cards. The summary and the recent workflows list render real persisted
 * runs from the local history database; MockData is no longer used here.
 * When the preflight check refuses a run (service off or target app
 * missing), the reason is shown as a dismissible banner under the RUN
 * button instead of starting a doomed execution. Since Phase 20, RUN asks
 * for an explicit confirmation before the agent takes over the device.
 * Since Phase 21, a getting started guide is shown while the
 * accessibility service is not connected.
 */
@Composable
fun AgentDashboardScreen(
    runs: List<AgentRunUi>,
    onStartTask: (String) -> Unit,
    onOpenDetail: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    preflightError: String? = null,
    onDismissPreflight: () -> Unit = {},
    serviceConnected: Boolean = true,
) {
    var instruction by remember { mutableStateOf("") }
    var confirmRun by remember { mutableStateOf(false) }

    val totalRuns = runs.size
    val completedRuns = runs.count { it.outcome == "Completed" }
    val successRate = if (totalRuns > 0) (completedRuns * 100) / totalRuns else 0
    val recent = runs.take(3)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                "What should I create?",
                style = NazeTypography.pageTitle,
                color = NazeColors.textPrimary,
            )
        }
        item {
            Column {
                NazeTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    placeholder = "Describe your workflow",
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    NazeStatusLabel(
                        label = "Connected",
                        color = NazeColors.success,
                        icon = Icons.Rounded.PlayArrow,
                    )
                    NazeButton(
                        text = "RUN",
                        onClick = { if (instruction.isNotBlank()) confirmRun = true },
                        enabled = instruction.isNotBlank(),
                        isPrimary = true,
                        leadingIcon = Icons.Rounded.PlayArrow,
                    )
                }
                if (preflightError != null) {
                    Spacer(Modifier.height(8.dp))
                    NazeCard(padding = 12.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = NazeColors.error,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                preflightError,
                                style = NazeTypography.caption,
                                color = NazeColors.error,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = onDismissPreflight) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Dismiss",
                                    tint = NazeColors.textMuted,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (!serviceConnected) {
            // Phase 21: getting started guide shown while the service is
            // not connected, so a new user knows exactly what to enable.
            item {
                NazeSection(title = "Getting started") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GettingStartedRow(
                            index = 1,
                            text = "Open Settings, find Naze Motion under installed " +
                                "services, and grant accessibility permission.",
                        )
                        GettingStartedRow(
                            index = 2,
                            text = "Install Alight Motion and open it at least once.",
                        )
                        GettingStartedRow(
                            index = 3,
                            text = "Come back here, describe your workflow, and press RUN.",
                        )
                    }
                }
            }
        }
        item {
            NazeSection(title = "Run summary") {
                if (totalRuns == 0) {
                    Text(
                        "No runs recorded yet. Statistics appear after the first real execution.",
                        style = NazeTypography.body,
                        color = NazeColors.textMuted,
                    )
                } else {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        ) {
                            Text(
                                "Total runs",
                                style = NazeTypography.body,
                                color = NazeColors.textMuted,
                            )
                            Text(
                                totalRuns.toString(),
                                style = NazeTypography.body.copy(color = NazeColors.textPrimary),
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        ) {
                            Text(
                                "Completed",
                                style = NazeTypography.body,
                                color = NazeColors.textMuted,
                            )
                            Text(
                                completedRuns.toString(),
                                style = NazeTypography.body.copy(color = NazeColors.textPrimary),
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        ) {
                            Text(
                                "Success rate",
                                style = NazeTypography.body,
                                color = NazeColors.textMuted,
                            )
                            Text(
                                successRate.toString() + "%",
                                style = NazeTypography.body.copy(
                                    color = if (successRate >= 50) {
                                        NazeColors.success
                                    } else {
                                        NazeColors.warning
                                    },
                                ),
                            )
                        }
                    }
                }
            }
        }
        item {
            NazeSection(title = "Recent workflows") {
                if (recent.isEmpty()) {
                    NazeEmptyState(
                        title = "No workflows yet",
                        description = "Describe something you want to create and Naze will turn it into a workflow.",
                        icon = Icons.Rounded.History,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recent.forEach { run ->
                            NazeCard(padding = 12.dp, onClick = { onOpenDetail(run.id) }) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column {
                                        Text(
                                            run.instruction,
                                            style = NazeTypography.body.copy(color = NazeColors.textPrimary),
                                            maxLines = 1,
                                        )
                                        Text(
                                            run.completedCount.toString() + " of " +
                                                run.actionCount + " actions",
                                            style = NazeTypography.caption,
                                            color = NazeColors.textMuted,
                                        )
                                    }
                                    NazeStatusLabel(
                                        label = run.outcome,
                                        color = if (run.outcome == "Completed") {
                                            NazeColors.success
                                        } else {
                                            NazeColors.error
                                        },
                                        icon = if (run.outcome == "Completed") {
                                            Icons.Rounded.Check
                                        } else {
                                            Icons.Rounded.ErrorOutline
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }

    // Phase 20: starting a run takes over the device, so it always
    // requires an explicit confirmation first.
    if (confirmRun) {
        AlertDialog(
            onDismissRequest = { confirmRun = false },
            title = { Text("Start this run?") },
            text = {
                Text(
                    "Naze will open Alight Motion and perform this instruction " +
                        "automatically. Keep the device still and press Emergency " +
                        "Stop at any time to cancel.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRun = false
                        if (instruction.isNotBlank()) onStartTask(instruction)
                    },
                ) { Text("Start run", color = NazeColors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRun = false }) { Text("Cancel") }
            },
        )
    }
}

/** One numbered getting started step (Phase 21). */
@Composable
private fun GettingStartedRow(index: Int, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            index.toString() + ".",
            style = NazeTypography.body.copy(color = NazeColors.primary),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = NazeTypography.body,
            color = NazeColors.textMuted,
            modifier = Modifier.weight(1f),
        )
    }
}
