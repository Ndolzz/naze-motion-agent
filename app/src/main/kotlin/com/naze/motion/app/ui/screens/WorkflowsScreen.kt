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
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.motion.app.agent.AgentRunUi
import com.naze.motion.app.agent.SavedWorkflowUi
import com.naze.motion.core.adapter.TargetAdapterRegistry
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.NazeEmptyState
import com.naze.motion.app.ui.components.NazeSection
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeTypography

/**
 * Workflows tab (Phase 26): the saved workflow library. A saved workflow
 * keeps an instruction together with the target application it was saved
 * for, so running it again restores both: the target selection switches
 * to the saved target and the run starts immediately after the usual
 * confirmation. Finished runs are saved from here with one tap, and a
 * saved workflow can be deleted when it is no longer useful. Saved
 * workflows are disposable local convenience data, not cloud documents.
 */
@Composable
fun WorkflowsScreen(
    saved: List<SavedWorkflowUi>,
    runs: List<AgentRunUi>,
    onRunWorkflow: (Long) -> Unit,
    onDeleteWorkflow: (Long) -> Unit,
    onSaveWorkflow: (Long) -> Unit,
) {
    val savable = runs.filter { it.outcome != "Cancelled" }.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            NazeSection(title = "Saved workflows") {
                if (saved.isEmpty()) {
                    NazeEmptyState(
                        title = "No saved workflows",
                        description = "Save a finished run below and it appears here " +
                            "for one tap reuse, together with the target " +
                            "application it was saved for.",
                        icon = Icons.Rounded.Bookmarks,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        saved.forEach { workflow ->
                            NazeCard(padding = 12.dp) {
                                Column {
                                    Text(
                                        workflow.instruction,
                                        style = NazeTypography.body.copy(
                                            color = NazeColors.textPrimary,
                                        ),
                                        maxLines = 2,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Target: " +
                                            TargetAdapterRegistry.displayNameFor(
                                                workflow.targetPackage,
                                            ),
                                        style = NazeTypography.caption,
                                        color = NazeColors.textMuted,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        // Phase 26: running a saved workflow
                                        // restores its target application
                                        // and starts the run.
                                        NazeButton(
                                            text = "Run",
                                            onClick = { onRunWorkflow(workflow.id) },
                                            isPrimary = true,
                                            leadingIcon = Icons.Rounded.PlayArrow,
                                        )
                                        NazeButton(
                                            text = "Delete",
                                            onClick = { onDeleteWorkflow(workflow.id) },
                                            leadingIcon = Icons.Rounded.Delete,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            // Phase 26: every finished run can be kept as a saved workflow
            // with one tap, straight from the recent history.
            NazeSection(title = "Save from recent runs") {
                if (savable.isEmpty()) {
                    Text(
                        "No finished runs yet. Runs appear here after the first " +
                            "real execution.",
                        style = NazeTypography.body,
                        color = NazeColors.textMuted,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        savable.forEach { run ->
                            NazeCard(padding = 12.dp) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            run.instruction,
                                            style = NazeTypography.body.copy(
                                                color = NazeColors.textPrimary,
                                            ),
                                            maxLines = 1,
                                        )
                                        Text(
                                            TargetAdapterRegistry.displayNameFor(
                                                run.targetPackage,
                                            ) + " - " + run.outcome,
                                            style = NazeTypography.caption,
                                            color = NazeColors.textMuted,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    NazeButton(
                                        text = "Save",
                                        onClick = { onSaveWorkflow(run.id) },
                                        leadingIcon = Icons.Rounded.Save,
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
}
