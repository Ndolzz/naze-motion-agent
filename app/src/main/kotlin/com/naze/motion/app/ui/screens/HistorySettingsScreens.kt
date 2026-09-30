package com.naze.motion.app.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Settings as SettingsIcon
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.naze.motion.app.agent.AgentRunUi
import com.naze.motion.app.agent.ApiKeyStore
import com.naze.motion.core.access.AccessibilityConnection
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.NazeEmptyState
import com.naze.motion.app.ui.components.NazeLog
import com.naze.motion.app.ui.components.NazeStatusLabel
import com.naze.motion.app.ui.components.NazeTextField
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeShapes
import com.naze.motion.app.ui.theme.NazeTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Compact activity log over persisted runs, not big cards. */
@Composable
fun HistoryScreen(
    runs: List<AgentRunUi>,
    onOpenDetail: (Long) -> Unit,
    onDeleteAll: (() -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("History", style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
                Spacer(Modifier.weight(1f))
                if (onDeleteAll != null && runs.isNotEmpty()) {
                    NazeButton(
                        text = "Clear all",
                        onClick = onDeleteAll,
                        leadingIcon = Icons.Rounded.DeleteOutline,
                    )
                }
            }
        }
        if (runs.isEmpty()) {
            item {
                NazeEmptyState(
                    title = "No runs yet",
                    description = "Finished agent runs appear here with their full technical log.",
                    icon = Icons.Rounded.History,
                )
            }
        }
        items(runs) { run ->
            NazeCard(padding = 12.dp, onClick = { onOpenDetail(run.id) }) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text(
                            run.instruction,
                            style = NazeTypography.body.copy(color = NazeColors.textPrimary),
                            maxLines = 1,
                        )
                        NazeStatusLabel(
                            label = run.outcome + " " + formatEnded(run.endedAtMs),
                            color = if (run.outcome == "Completed") NazeColors.success else NazeColors.error,
                            icon = if (run.outcome == "Completed") Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

/** Workflow detail from a persisted run: real stats and real log. */
@Composable
fun WorkflowDetailScreen(
    run: AgentRunUi,
    onBack: () -> Unit,
    onDeleteRun: (() -> Unit)? = null,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Column {
                Text(run.instruction, style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
                Spacer(Modifier.height(4.dp))
                NazeStatusLabel(
                    label = run.outcome,
                    color = when (run.outcome) {
                        "Completed" -> NazeColors.success
                        "Cancelled" -> NazeColors.textMuted
                        else -> NazeColors.error
                    },
                    icon = if (run.outcome == "Completed") Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
                )
            }
        }
        item {
            NazeCard {
                Column {
                    DetailRow("Duration", formatDuration(run.durationMs))
                    DetailRow("Actions", run.completedCount.toString() + " of " + run.actionCount + " completed")
                    DetailRow("Ended", formatEnded(run.endedAtMs))
                    DetailRow("Application", "Alight Motion")
                    if (run.reason != null) {
                        DetailRow("Reason", run.reason)
                    }
                }
            }
        }
        item {
            Column {
                Text("Technical log", style = NazeTypography.section, color = NazeColors.textPrimary)
                Spacer(Modifier.height(8.dp))
                if (run.logLines.isEmpty()) {
                    Text("No log events recorded.", style = NazeTypography.body, color = NazeColors.textMuted)
                } else {
                    NazeLog(run.logLines)
                }
            }
        }
        if (onDeleteRun != null) {
            item {
                NazeButton(
                    text = "Delete run",
                    onClick = onDeleteRun,
                    leadingIcon = Icons.Rounded.DeleteOutline,
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatEnded(endedAtMs: Long): String {
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(Date(endedAtMs))
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
 * Settings (Phase 12/14/15/16): API keys are entered here, inside the app,
 * and stay on this device. Keys are masked by default, stored in app
 * private storage, and used only for the selected provider. The Test
 * button verifies the current configuration with one real planning call
 * before saving. The Automation card shows the real accessibility service
 * connection state and can open the system accessibility settings.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val store = remember { ApiKeyStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var selectedId by remember { mutableStateOf(store.selectedId()) }
    var apiKey by remember(selectedId) { mutableStateOf(store.load(selectedId).apiKey) }
    var model by remember(selectedId) { mutableStateOf(store.load(selectedId).model) }
    var baseUrl by remember(selectedId) { mutableStateOf(store.load(selectedId).baseUrl) }
    var showKey by remember { mutableStateOf(false) }
    var savedTick by remember { mutableStateOf(0) }
    var testing by remember { mutableStateOf(false) }
    var testMessage by remember { mutableStateOf<String?>(null) }
    var testOk by remember { mutableStateOf(false) }
    var serviceConnected by remember { mutableStateOf(AccessibilityConnection.connected) }

    // Keep the service state fresh while Settings is visible, including
    // after the user returns from the system accessibility settings.
    LaunchedEffect(Unit) {
        while (true) {
            serviceConnected = AccessibilityConnection.connected
            delay(1000)
        }
    }

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
                        if (entry.keyHint != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                entry.keyHint!!,
                                style = NazeTypography.caption,
                                color = NazeColors.textMuted,
                            )
                        }
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
                                text = if (testing) "Testing" else "Test",
                                onClick = {
                                    val kind = entry.kind
                                    if (kind == null || testing) return@NazeButton
                                    testing = true
                                    testMessage = null
                                    scope.launch {
                                        val result = ApiKeyStore.testConfig(
                                            kind = kind,
                                            baseUrl = baseUrl.ifBlank { entry.defaultBaseUrl ?: "" },
                                            apiKey = apiKey,
                                            model = model,
                                        )
                                        testOk = result.isSuccess
                                        testMessage = result.fold(
                                            { it },
                                            { e -> e.message ?: "test failed" },
                                        )
                                        testing = false
                                    }
                                },
                                enabled = !testing && apiKey.isNotBlank() && model.isNotBlank(),
                                leadingIcon = Icons.Rounded.Bolt,
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
                        if (testMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            NazeStatusLabel(
                                label = testMessage!!,
                                color = if (testOk) NazeColors.success else NazeColors.error,
                                icon = if (testOk) Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
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
                    Spacer(Modifier.height(6.dp))
                    NazeStatusLabel(
                        label = if (serviceConnected) "Service connected" else "Service off",
                        color = if (serviceConnected) NazeColors.success else NazeColors.error,
                        icon = if (serviceConnected) Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
                    )
                    Spacer(Modifier.height(6.dp))
                    DetailRow("Action timeout", "5000 ms")
                    DetailRow("Retry limit", "2")
                    DetailRow("Verification mode", "Strict")
                    Spacer(Modifier.height(8.dp))
                    NazeButton(
                        text = "Open system accessibility settings",
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        },
                        leadingIcon = Icons.Rounded.SettingsIcon,
                    )
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
                    DetailRow("Version", "0.16.0")
                    DetailRow("Open source licenses", "View")
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
