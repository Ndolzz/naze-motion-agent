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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.naze.motion.app.agent.ApiKeyStore
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.NazeStatusLabel
import com.naze.motion.app.ui.components.NazeTextField
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
                            label = item.outcome + " " + item.time,
                            color = if (item.completed) NazeColors.success else NazeColors.error,
                            icon = if (item.completed) Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

/** Workflow detail with compact stats and timeline. */
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
                Spacer(Modifier.height(4.dp))
                NazeStatusLabel(label = "Completed", color = NazeColors.success, icon = Icons.Rounded.Check)
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
                        Icon(
                            Icons.Rounded.Check,
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

/**
 * Settings (Phase 12): API keys are entered here, inside the app, and stay
 * on this device. Keys are masked by default, stored in app private
 * storage, and used only for the selected provider. Multiple providers can
 * be configured at once; one is active.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val store = remember { ApiKeyStore(context.applicationContext) }
    var selectedId by remember { mutableStateOf(store.selectedId()) }
    var apiKey by remember(selectedId) { mutableStateOf(store.load(selectedId).apiKey) }
    var model by remember(selectedId) { mutableStateOf(store.load(selectedId).model) }
    var baseUrl by remember(selectedId) { mutableStateOf(store.load(selectedId).baseUrl) }
    var showKey by remember { mutableStateOf(false) }
    var savedTick by remember { mutableStateOf(0) }

    val entry = remember(selectedId) { ApiKeyStore.catalog.firstOrNull { it.id == selectedId } }
    val saved = remember(savedTick, selectedId) { store.hasKey(selectedId) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { Text("Settings", style = NazeTypography.pageTitle, color = NazeColors.textPrimary) }

        item {
            Column {
                Text("AI provider", style = NazeTypography.section, color = NazeColors.textPrimary)
                Spacer(Modifier.height(8.dp))
                ApiKeyStore.catalog.forEach { catalog ->
                    val selected = catalog.id == selectedId
                    val hasKey = catalog.kind != null && saved
                    NazeCard(padding = 12.dp, onClick = { selectedId = catalog.id }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                catalog.label,
                                style = NazeTypography.body.copy(
                                    color = if (selected) NazeColors.primary else NazeColors.textPrimary,
                                ),
                            )
                            when {
                                catalog.kind == null ->
                                    NazeStatusLabel(
                                        label = if (selected) "Active" else "On device",
                                        color = NazeColors.textMuted,
                                        icon = Icons.Rounded.History,
                                    )
                                hasKey ->
                                    NazeStatusLabel(
                                        label = if (selected) "Active" else "Key saved",
                                        color = NazeColors.success,
                                        icon = Icons.Rounded.Key,
                                    )
                                else ->
                                    NazeStatusLabel(
                                        label = "No key",
                                        color = NazeColors.warning,
                                        icon = Icons.Rounded.ErrorOutline,
                                    )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        if (entry != null && entry.kind != null) {
            item {
                NazeCard {
                    Column {
                        Text(entry.label, style = NazeTypography.section, color = NazeColors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "The key is stored on this device only and sent only to the configured endpoint.",
                            style = NazeTypography.caption,
                            color = NazeColors.textMuted,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            placeholder = {
                                Text("API key", style = NazeTypography.body, color = NazeColors.textMuted)
                            },
                            singleLine = true,
                            visualTransformation =
                                if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(
                                        if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = if (showKey) "Hide key" else "Show key",
                                        tint = NazeColors.textMuted,
                                    )
                                }
                            },
                            shape = NazeShapes.card,
                            textStyle = NazeTypography.body,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = NazeColors.surfaceElevated,
                                unfocusedContainerColor = NazeColors.surfaceElevated,
                                focusedBorderColor = NazeColors.primary,
                                unfocusedBorderColor = NazeColors.border,
                                cursorColor = NazeColors.primary,
                                focusedTextColor = NazeColors.textPrimary,
                                unfocusedTextColor = NazeColors.textPrimary,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        NazeTextField(
                            value = model,
                            onValueChange = { model = it },
                            placeholder = entry.defaultModel ?: "Model name",
                            minLines = 1,
                        )
                        Spacer(Modifier.height(8.dp))
                        NazeTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            placeholder = entry.defaultBaseUrl ?: "Base URL",
                            minLines = 1,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NazeButton(
                                text = "Save",
                                onClick = {
                                    store.save(selectedId, apiKey, model, baseUrl)
                                    store.setSelected(selectedId)
                                    savedTick = savedTick + 1
                                },
                                enabled = apiKey.isNotBlank() && model.isNotBlank(),
                                isPrimary = true,
                                leadingIcon = Icons.Rounded.Check,
                            )
                            NazeButton(
                                text = "Clear",
                                onClick = {
                                    store.clear(selectedId)
                                    apiKey = ""
                                    model = ""
                                    baseUrl = ""
                                    savedTick = savedTick + 1
                                },
                            )
                        }
                        if (saved) {
                            Spacer(Modifier.height(8.dp))
                            NazeStatusLabel(
                                label = "Key saved on this device",
                                color = NazeColors.success,
                                icon = Icons.Rounded.Key,
                            )
                        }
                    }
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
                    DetailRow("Version", "0.12.0")
                    DetailRow("Open source licenses", "View")
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
