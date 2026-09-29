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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.NazeEmptyState
import com.naze.motion.app.ui.components.NazeStatusLabel
import com.naze.motion.app.ui.model.MockData
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeTypography

/** Compact activity log, not big cards. */
@Composable
fun HistoryScreen(onOpenDetail: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { Text("History", style = NazeTypography.pageTitle, color = NazeColors.textPrimary) }
        items(MockData.history) { item ->
            NazeCard(padding = 12.dp, onClick = { onOpenDetail(item.name) }) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text(item.name, style = NazeTypography.body.copy(color = NazeColors.textPrimary))
                        NazeStatusLabel(
                            label = item.outcome + " · " + item.time,
                            color = if (item.completed) NazeColors.success else NazeColors.error,
                            icon = androidx.compose.material.icons.Icons.Rounded.Check.takeIf { item.completed }
                                ?: androidx.compose.material.icons.Icons.Rounded.ErrorOutline,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

/** Workflow detail with compact stats and expandable technical info. */
@Composable
fun WorkflowDetailScreen(name: String, onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Column {
                Text(name, style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
                NazeStatusLabel("Completed", NazeColors.success, androidx.compose.material.icons.Icons.Rounded.Check)
            }
        }
        item {
            NazeCard {
                Column {
                    DetailRow("Duration", "01:42")
                    DetailRow("Actions", "8")
                    DetailRow("Application", "Alight Motion")
                }
            }
        }
        item {
            Column {
                Text("Timeline", style = NazeTypography.section, color = NazeColors.textPrimary)
                Spacer(Modifier.height(8.dp))
                MockData.plan.forEach { step ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        androidx.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Rounded.Check,
                            contentDescription = "completed",
                            tint = NazeColors.success,
                        )
                        Spacer(Modifier.padding(4.dp))
                        Text(step, style = NazeTypography.body)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Text(label, style = NazeTypography.body, color = NazeColors.textMuted)
        Text(value, style = NazeTypography.body.copy(color = NazeColors.textPrimary))
    }
}

/** Settings: simple sections, API key never shown as plaintext. */
@Composable
fun SettingsScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { Text("Settings", style = NazeTypography.pageTitle, color = NazeColors.textPrimary) }
        item {
            NazeCard {
                Column {
                    Text("AI", style = NazeTypography.section, color = NazeColors.textPrimary)
                    DetailRow("Provider", "Not configured")
                    DetailRow("Model", "Not configured")
                    DetailRow("API configuration", "Managed securely")
                }
            }
        }
        item {
            NazeCard {
                Column {
                    Text("Automation", style = NazeTypography.section, color = NazeColors.textPrimary)
                    DetailRow("Accessibility Service", "Off")
                    DetailRow("Action timeout", "5000 ms")
                    DetailRow("Retry limit", "2")
                    DetailRow("Verification mode", "Strict")
                }
            }
        }
        item {
            NazeCard {
                Column {
                    Text("Safety", style = NazeTypography.section, color = NazeColors.textPrimary)
                    DetailRow("Require confirmation", "On")
                    DetailRow("Emergency stop", "Always available")
                    DetailRow("Allowed applications", "Alight Motion")
                }
            }
        }
        item {
            NazeCard {
                Column {
                    Text("About", style = NazeTypography.section, color = NazeColors.textPrimary)
                    DetailRow("Version", "0.1.0")
                    DetailRow("Open source licenses", "View")
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
