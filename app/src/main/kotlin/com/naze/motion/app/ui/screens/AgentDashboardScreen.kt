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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.motion.app.ui.components.NazeActionTimeline
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.NazeConnectionRow
import com.naze.motion.app.ui.components.NazeEmptyState
import com.naze.motion.app.ui.components.NazeLog
import com.naze.motion.app.ui.components.NazeProgress
import com.naze.motion.app.ui.components.NazeSection
import com.naze.motion.app.ui.components.NazeStatusLabel
import com.naze.motion.app.ui.components.NazeTextField
import com.naze.motion.app.ui.components.TimelineItemState
import com.naze.motion.app.ui.model.AgentUiState
import com.naze.motion.app.ui.model.MockData
import com.naze.motion.app.ui.model.TimelineEntry
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeTypography

/**
 * Agent dashboard. Input is the focal point, not a wall of cards.
 * Mock state only: no fake AI behavior, engine wiring comes in Phase 5.
 */
@Composable
fun AgentDashboardScreen(
    onStartTask: (String) -> Unit,
    onOpenHistory: () -> Unit,
) {
    var instruction by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text("What should I create?", style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
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
                    NazeConnectionRowCompact()
                    NazeButton(
                        text = "RUN",
                        onClick = { if (instruction.isNotBlank()) onStartTask(instruction) },
                        enabled = instruction.isNotBlank(),
                        isPrimary = true,
                        leadingIcon = Icons.Rounded.PlayArrow,
                    )
                }
            }
        }
        item {
            NazeSection(title = "Recent workflows") {
                if (MockData.recentWorkflows.isEmpty()) {
                    NazeEmptyState(
                        title = "No workflows yet",
                        description = "Describe something you want to create and Naze will turn it into a workflow.",
                        icon = Icons.Rounded.History,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MockData.recentWorkflows.forEach { (name, meta) ->
                            NazeCard(padding = 12.dp, onClick = onOpenHistory) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column {
                                        Text(name, style = NazeTypography.body.copy(color = NazeColors.textPrimary))
                                        Text(meta, style = NazeTypography.caption)
                                    }
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = NazeColors.textMuted,
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

@Composable
private fun NazeConnectionRowCompact() {
    NazeStatusLabel(
        label = "Connected",
        color = NazeColors.success,
        icon = Icons.Rounded.PlayArrow,
    )
}
