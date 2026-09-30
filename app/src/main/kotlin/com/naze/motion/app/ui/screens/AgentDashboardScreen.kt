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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
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
import com.naze.motion.core.adapter.TargetAdapterRegistry
import com.naze.motion.core.adapter.VocabularyAuditEntry
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.NazeEmptyState
import com.naze.motion.app.ui.components.NazeSection
import com.naze.motion.app.ui.components.NazeStatusLabel
import com.naze.motion.app.ui.components.NazeTextField
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeTypography

/**
 * Agent dashboard (Phase 18 to 26). Input is the focal point, not a wall
 * of cards. The summary and the recent workflows list render real
 * persisted runs from the local history database; MockData is no longer
 * used here. When the preflight check refuses a run (service off or
 * target app missing), the reason is shown as a dismissible banner under
 * the RUN button instead of starting a doomed execution. Since Phase 20,
 * RUN asks for an explicit confirmation before the agent takes over the
 * device. Since Phase 21, a getting started guide is shown while the
 * accessibility service is not connected. Since Phase 25, a reconnect
 * banner appears when the accessibility link dropped during the last run
 * and has reconnected: it shows the interrupted instruction and offers a
 * one tap re-run. Since Phase 26, the target application is a per device
 * selection shown under the input: every known target from the registry
 * is listed and the selection is applied to every run. Since Phase 26,
 * the dashboard can also audit the selected target's UI vocabulary on
 * the real device, reporting found and not found elements per entry.
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
    reconnectOffer: String? = null,
    onRerun: (() -> Unit)? = null,
    onDismissReconnect: () -> Unit = {},
    targetName: String = "Alight Motion",
    targets: List<TargetAdapterRegistry.TargetApp> = emptyList(),
    selectedTargetPackage: String = "",
    onSelectTarget: (String) -> Unit = {},
    auditEntries: List<VocabularyAuditEntry>? = null,
    auditRunning: Boolean = false,
    auditError: String? = null,
    onAuditTarget: () -> Unit = {},
) {
    var instruction by remember { mutableStateOf("") }
    var confirmRun by remember { mutableStateOf(false) }

    val totalRuns = runs.size
    val completedRuns = runs.count { it.outcome == "Completed" }
    val successRate = if (totalRuns > 0) (completedRuns * 100) / totalRuns else 0
    val recent = runs.take(3)
    val foundCount = auditEntries?.count { it.found } ?: 0

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NazeStatusLabel(
                            label = "Connected",
                            color = NazeColors.success,
                            icon = Icons.Rounded.PlayArrow,
                        )
                        Spacer(Modifier.width(8.dp))
                        // Phase 26: the selected target application is
                        // shown next to the connection status.
                        Text(
                            targetName,
                            style = NazeTypography.caption,
                            color = NazeColors.textMuted,
                        )
                    }
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
                // Phase 25: the accessibility link dropped during the last
                // run and has reconnected, so the interrupted instruction
                // can be retried with one tap.
                if (reconnectOffer != null) {
                    Spacer(Modifier.height(8.dp))
                    NazeCard(padding = 12.dp) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = NazeColors.warning,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "The accessibility service disconnected during " +
                                        "the last run and has reconnected. The " +
                                        "instruction can be retried.",
                                    style = NazeTypography.caption,
                                    color = NazeColors.warning,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = onDismissReconnect) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Dismiss",
                                        tint = NazeColors.textMuted,
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                reconnectOffer,
                                style = NazeTypography.body.copy(
                                    color = NazeColors.textPrimary,
                                ),
                                maxLines = 2,
                            )
                            Spacer(Modifier.height(8.dp))
                            if (onRerun != null) {
                                NazeButton(
                                    text = "Run again",
                                    onClick = onRerun,
                                    isPrimary = true,
                                    leadingIcon = Icons.Rounded.Refresh,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (targets.isNotEmpty()) {
            // Phase 26: the target application the agent drives is a per
            // device selection. Every registry known target is listed;
            // tapping one persists the selection for every future run.
            item {
                NazeSection(title = "Target application") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        targets.forEach { app ->
                            val selected = app.packageName == selectedTargetPackage
                            NazeCard(
                                padding = 12.dp,
                                onClick = { onSelectTarget(app.packageName) },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            app.displayName,
                                            style = NazeTypography.body.copy(
                                                color = if (selected) {
                                                    NazeColors.primary
                                                } else {
                                                    NazeColors.textPrimary
                                                },
                                            ),
                                        )
                                        Text(
                                            app.packageName,
                                            style = NazeTypography.caption,
                                            color = NazeColors.textMuted,
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = NazeColors.success,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Phase 26: audit the selected target's UI vocabulary on the
            // real device. Every known element of the adapter is resolved
            // against the actual screen and reported per entry.
            item {
                NazeSection(title = "Vocabulary audit") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Checks every UI element the agent knows in " +
                                targetName + " against the real screen on this " +
                                "device. The target app is opened first.",
                            style = NazeTypography.caption,
                            color = NazeColors.textMuted,
                        )
                        if (auditError != null) {
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
                                        auditError,
                                        style = NazeTypography.caption,
                                        color = NazeColors.error,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        NazeButton(
                            text = if (auditRunning) "Auditing " + targetName + "..."
                            else "Audit " + targetName + " vocabulary",
                            onClick = onAuditTarget,
                            enabled = !auditRunning,
                            leadingIcon = Icons.Rounded.Search,
                        )
                        if (auditEntries != null) {
                            Text(
                                foundCount.toString() + " of " + auditEntries.size +
                                    " known elements found on the current screen.",
                                style = NazeTypography.caption,
                                color = NazeColors.textMuted,
                            )
                            NazeCard(padding = 12.dp) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    auditEntries.forEach { entry ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Icon(
                                                if (entry.found) Icons.Rounded.Check
                                                else Icons.Rounded.Close,
                                                contentDescription = null,
                                                tint = if (entry.found) {
                                                    NazeColors.success
                                                } else {
                                                    NazeColors.error
                                                },
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                entry.id,
                                                style = NazeTypography.caption,
                                                color = if (entry.found) {
                                                    NazeColors.textPrimary
                                                } else {
                                                    NazeColors.textMuted
                                                },
                                            )
                                        }
                                    }
                                }
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
                            text = "Install " + targetName + " and open it at least once.",
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
    // requires an explicit confirmation first. Phase 26: the confirmation
    // names the selected target application.
    if (confirmRun) {
        AlertDialog(
            onDismissRequest = { confirmRun = false },
            title = { Text("Start this run?") },
            text = {
                Text(
                    "Naze will open " + targetName + " and perform this instruction " +
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
