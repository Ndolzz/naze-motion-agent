package com.naze.motion.app.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings as SettingsIcon
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.core.content.FileProvider
import com.naze.motion.app.agent.AgentRunUi
import com.naze.motion.app.agent.AllowedAppsStore
import com.naze.motion.app.agent.ApiKeyStore
import com.naze.motion.app.agent.ConfigPorter
import com.naze.motion.app.agent.SafetySettingsStore
import com.naze.motion.core.access.AccessibilityConnection
import com.naze.motion.app.ui.components.NazeActionTimeline
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.NazeEmptyState
import com.naze.motion.app.ui.components.NazeLog
import com.naze.motion.app.ui.components.NazeStatusLabel
import com.naze.motion.app.ui.components.NazeTextField
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeShapes
import com.naze.motion.app.ui.theme.NazeTypography
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Compact activity log over persisted runs, with a simple search filter. */
@Composable
fun HistoryScreen(
    runs: List<AgentRunUi>,
    onOpenDetail: (Long) -> Unit,
    onDeleteAll: (() -> Unit)? = null,
) {
    var confirmClear by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val visible = remember(runs, query) {
        if (query.isBlank()) runs
        else runs.filter { it.instruction.contains(query.trim(), ignoreCase = true) }
    }

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
                        onClick = { confirmClear = true },
                        leadingIcon = Icons.Rounded.DeleteOutline,
                    )
                }
            }
        }
        if (runs.isNotEmpty()) {
            item {
                NazeTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search instruction",
                    minLines = 1,
                )
            }
        }
        if (visible.isEmpty()) {
            item {
                if (runs.isEmpty()) {
                    NazeEmptyState(
                        title = "No runs yet",
                        description = "Finished agent runs appear here with their full technical log.",
                        icon = Icons.Rounded.History,
                    )
                } else {
                    NazeEmptyState(
                        title = "No matches",
                        description = "No persisted run matches the search.",
                        icon = Icons.Rounded.Search,
                    )
                }
            }
        }
        items(visible) { run ->
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
        item { Spacer(Modifier.height(32.dp)) }
    }

    if (confirmClear && onDeleteAll != null) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all runs?") },
            text = {
                Text(
                    "Every persisted run and its technical log will be deleted " +
                        "from this device. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onDeleteAll()
                    },
                ) { Text("Clear all", color = NazeColors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

/** Workflow detail from a persisted run: real plan timeline, stats, and log. */
@Composable
fun WorkflowDetailScreen(
    run: AgentRunUi,
    onBack: () -> Unit,
    onDeleteRun: (() -> Unit)? = null,
    onRunAgain: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Column {
                Text(
                    run.instruction,
                    style = NazeTypography.pageTitle,
                    color = NazeColors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                NazeStatusLabel(
                    label = run.outcome,
                    color = when (run.outcome) {
                        "Completed" -> NazeColors.success
                        "Cancelled" -> NazeColors.textMuted
                        else -> NazeColors.error
                    },
                    icon = if (run.outcome == "Completed") {
                        Icons.Rounded.Check
                    } else {
                        Icons.Rounded.ErrorOutline
                    },
                )
            }
        }
        item {
            NazeCard {
                Column {
                    Text(
                        "Plan timeline",
                        style = NazeTypography.section,
                        color = NazeColors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (run.planSteps.isEmpty()) {
                        Text(
                            "No plan steps recorded for this run.",
                            style = NazeTypography.body,
                            color = NazeColors.textMuted,
                        )
                    } else {
                        NazeActionTimeline(items = run.planSteps)
                    }
                }
            }
        }
        item {
            NazeCard {
                Column {
                    DetailRow("Duration", formatDuration(run.durationMs))
                    DetailRow(
                        "Actions",
                        run.completedCount.toString() + " of " + run.actionCount + " completed",
                    )
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
                Text(
                    "Technical log",
                    style = NazeTypography.section,
                    color = NazeColors.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                if (run.logLines.isEmpty()) {
                    Text(
                        "No log events recorded.",
                        style = NazeTypography.body,
                        color = NazeColors.textMuted,
                    )
                } else {
                    NazeLog(run.logLines)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Phase 21: rerun this run's instruction with one tap.
                if (onRunAgain != null) {
                    NazeButton(
                        text = "Run again",
                        onClick = { onRunAgain(run.instruction) },
                        isPrimary = true,
                        leadingIcon = Icons.Rounded.Refresh,
                    )
                }
                // Phase 20: export the run log as a text file and hand it
                // to the system share sheet.
                NazeButton(
                    text = "Export log",
                    onClick = { runCatching { exportRunLog(context, run) } },
                    leadingIcon = Icons.Rounded.Share,
                )
                if (onDeleteRun != null) {
                    NazeButton(
                        text = "Delete run",
                        onClick = { confirmDelete = true },
                        leadingIcon = Icons.Rounded.DeleteOutline,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }

    if (confirmDelete && onDeleteRun != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this run?") },
            text = {
                Text(
                    "The run and its technical log will be removed from this device. " +
                        "This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteRun()
                    },
                ) { Text("Delete", color = NazeColors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * Phase 20 export: writes the full run report (instruction, outcome,
 * stats, plan timeline, and technical log) to a text file in the app
 * cache and shares it through the system sheet via FileProvider. The
 * file lives in app private cache storage; only the share target
 * receives a temporary read grant.
 */
private fun exportRunLog(context: Context, run: AgentRunUi) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "naze-run-" + run.id + ".txt")
    file.writeText(buildRunLogText(run))
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Naze Motion run " + run.id + " log")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(share, "Share run log"))
}

/** Plain text report of one persisted run (Phase 20). */
private fun buildRunLogText(run: AgentRunUi): String {
    val builder = StringBuilder()
    builder.appendLine("Naze Motion run report")
    builder.appendLine()
    builder.appendLine("Instruction: " + run.instruction)
    builder.appendLine("Outcome: " + run.outcome)
    if (run.reason != null) {
        builder.appendLine("Reason: " + run.reason)
    }
    builder.appendLine(
        "Actions: " + run.completedCount + " of " + run.actionCount + " completed",
    )
    builder.appendLine("Duration: " + formatDuration(run.durationMs))
    builder.appendLine("Ended: " + formatEnded(run.endedAtMs))
    builder.appendLine()
    builder.appendLine("Plan timeline:")
    if (run.planSteps.isEmpty()) {
        builder.appendLine("  (no plan steps recorded)")
    } else {
        run.planSteps.forEach { (label, state) ->
            builder.appendLine("  [" + state.name + "] " + label)
        }
    }
    builder.appendLine()
    builder.appendLine("Technical log:")
    if (run.logLines.isEmpty()) {
        builder.appendLine("  (no log events recorded)")
    } else {
        run.logLines.forEach { (time, type) ->
            builder.appendLine("  " + time + " " + type)
        }
    }
    return builder.toString()
}

/**
 * Phase 25 export: hands the configuration JSON to the system share
 * sheet as plain text. The document contains API keys, so the user
 * always picks the destination explicitly here.
 */
private fun shareConfig(context: Context, json: String) {
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Naze Motion configuration")
        putExtra(Intent.EXTRA_TEXT, json)
    }
    context.startActivity(Intent.createChooser(share, "Share configuration"))
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
 * Settings (Phase 12 to 25): API keys are entered here, inside the app,
 * and stay on this device. Keys are masked by default, stored in app
 * private storage, and used only for the selected provider. The Test
 * button verifies the current configuration with one real planning call
 * before saving. The Automation card shows the real accessibility
 * service connection state, edits the configurable safety limits
 * (Phase 22), and can open the system accessibility settings. The
 * allowed applications card (Phase 23) edits the allowlist the runtime
 * enforces before every run. The Backup card (Phase 25) exports the
 * full configuration as one JSON document through the share sheet and
 * imports a pasted document back through the same stores.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val store = remember { ApiKeyStore(context.applicationContext) }
    val safety = remember { SafetySettingsStore(context.applicationContext) }
    val appsStore = remember { AllowedAppsStore(context.applicationContext) }
    val porter = remember { ConfigPorter(context.applicationContext) }
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
    var profile by remember { mutableStateOf(safety.load()) }
    var timeoutText by remember { mutableStateOf(profile.actionTimeoutMs.toString()) }
    var attemptsText by remember { mutableStateOf(profile.maxAttempts.toString()) }
    var recoveryText by remember { mutableStateOf(profile.recoveryMaxAttempts.toString()) }
    var backoffText by remember { mutableStateOf(profile.recoveryBackoffBaseMs.toString()) }
    var safetyMessage by remember { mutableStateOf<String?>(null) }
    var safetyOk by remember { mutableStateOf(false) }
    var allowedList by remember { mutableStateOf(appsStore.load()) }
    var newPackage by remember { mutableStateOf("") }
    var appMessage by remember { mutableStateOf<String?>(null) }
    var appOk by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    var backupOk by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            serviceConnected = AccessibilityConnection.connected
            delay(1000)
        }
    }

    fun refreshSafetyTexts() {
        profile = safety.load()
        timeoutText = profile.actionTimeoutMs.toString()
        attemptsText = profile.maxAttempts.toString()
        recoveryText = profile.recoveryMaxAttempts.toString()
        backoffText = profile.recoveryBackoffBaseMs.toString()
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
                Text(
                    "AI provider",
                    style = NazeTypography.section,
                    color = NazeColors.textPrimary,
                )
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
                                    color = if (selected) {
                                        NazeColors.primary
                                    } else {
                                        NazeColors.textPrimary
                                    },
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
                        Text(
                            entry.label,
                            style = NazeTypography.section,
                            color = NazeColors.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "The key is stored on this device only and sent only " +
                                "to the configured endpoint.",
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
                                Text(
                                    "API key",
                                    style = NazeTypography.body,
                                    color = NazeColors.textMuted,
                                )
                            },
                            singleLine = true,
                            visualTransformation =
                                if (showKey) VisualTransformation.None
                                else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(
                                        if (showKey) {
                                            Icons.Rounded.VisibilityOff
                                        } else {
                                            Icons.Rounded.Visibility
                                        },
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
                                            baseUrl = baseUrl.ifBlank {
                                                entry.defaultBaseUrl ?: ""
                                            },
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
                                icon = if (testOk) {
                                    Icons.Rounded.Check
                                } else {
                                    Icons.Rounded.ErrorOutline
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
                    Text(
                        "Automation",
                        style = NazeTypography.section,
                        color = NazeColors.textPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    NazeStatusLabel(
                        label = if (serviceConnected) "Service connected" else "Service off",
                        color = if (serviceConnected) {
                            NazeColors.success
                        } else {
                            NazeColors.error
                        },
                        icon = if (serviceConnected) {
                            Icons.Rounded.Check
                        } else {
                            Icons.Rounded.ErrorOutline
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            val open = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(open) }
                        },
                    ) {
                        Text(
                            "Open system accessibility settings",
                            color = NazeColors.primary,
                            style = NazeTypography.caption,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // Phase 22: configurable safety limits, stored on this
                    // device and applied to every run.
                    Text(
                        "Safety limits",
                        style = NazeTypography.section,
                        color = NazeColors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Upper bounds for the next run: no action can wait " +
                            "longer or retry more than these limits.",
                        style = NazeTypography.caption,
                        color = NazeColors.textMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    NazeTextField(
                        value = timeoutText,
                        onValueChange = { timeoutText = it },
                        placeholder = "Action timeout in ms",
                        minLines = 1,
                    )
                    Spacer(Modifier.height(8.dp))
                    NazeTextField(
                        value = attemptsText,
                        onValueChange = { attemptsText = it },
                        placeholder = "Retry limit per action",
                        minLines = 1,
                    )
                    Spacer(Modifier.height(8.dp))
                    NazeTextField(
                        value = recoveryText,
                        onValueChange = { recoveryText = it },
                        placeholder = "Recovery attempts",
                        minLines = 1,
                    )
                    Spacer(Modifier.height(8.dp))
                    NazeTextField(
                        value = backoffText,
                        onValueChange = { backoffText = it },
                        placeholder = "Recovery backoff base in ms",
                        minLines = 1,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NazeButton(
                            text = "Save limits",
                            onClick = {
                                val parsed = runCatching {
                                    safety.save(
                                        timeoutText.trim().toLong(),
                                        attemptsText.trim().toInt(),
                                        recoveryText.trim().toInt(),
                                        backoffText.trim().toLong(),
                                    )
                                }
                                parsed.fold(
                                    {
                                        refreshSafetyTexts()
                                        safetyOk = true
                                        safetyMessage = "Safety limits saved"
                                    },
                                    { e ->
                                        safetyOk = false
                                        safetyMessage = e.message ?: "invalid safety value"
                                    },
                                )
                            },
                            isPrimary = true,
                            leadingIcon = Icons.Rounded.Check,
                        )
                        NazeButton(
                            text = "Reset limits",
                            onClick = {
                                safety.reset()
                                refreshSafetyTexts()
                                safetyOk = true
                                safetyMessage = "Safety limits reset to defaults"
                            },
                            leadingIcon = Icons.Rounded.Refresh,
                        )
                    }
                    if (safetyMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        NazeStatusLabel(
                            label = safetyMessage!!,
                            color = if (safetyOk) NazeColors.success else NazeColors.error,
                            icon = if (safetyOk) {
                                Icons.Rounded.Check
                            } else {
                                Icons.Rounded.ErrorOutline
                            },
                        )
                    }
                }
            }
        }
        // Phase 25: backup card. Export writes the full configuration,
        // API keys included, into one JSON document handed to the share
        // sheet; import applies a pasted document through the same
        // stores the rest of this screen uses.
        item {
            NazeCard {
                Column {
                    Text(
                        "Backup",
                        style = NazeTypography.section,
                        color = NazeColors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Export the whole on-device configuration (safety limits, " +
                            "allowed applications, selected provider, and provider " +
                            "keys) as one JSON document. The export includes your " +
                            "API keys, so share it only with a destination you trust.",
                        style = NazeTypography.caption,
                        color = NazeColors.textMuted,
                    )
                    Spacer(Modifier.height(12.dp))
                    NazeButton(
                        text = "Export config",
                        onClick = { runCatching { shareConfig(context, porter.export()) } },
                        leadingIcon = Icons.Rounded.Share,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Paste an exported configuration below and press Import " +
                            "to apply it on this device.",
                        style = NazeTypography.caption,
                        color = NazeColors.textMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    NazeTextField(
                        value = importJson,
                        onValueChange = { importJson = it },
                        placeholder = "Paste configuration JSON here",
                        minLines = 3,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NazeButton(
                            text = "Import",
                            onClick = {
                                val result = porter.import(importJson)
                                backupOk = result.isSuccess
                                backupMessage = result.fold(
                                    {
                                        selectedId = store.selectedId()
                                        apiKey = store.load(selectedId).apiKey
                                        model = store.load(selectedId).model
                                        baseUrl = store.load(selectedId).baseUrl
                                        savedTick = savedTick + 1
                                        refreshSafetyTexts()
                                        allowedList = appsStore.load()
                                        importJson = ""
                                        "Configuration applied on this device"
                                    },
                                    { e -> e.message ?: "import failed" },
                                )
                            },
                            enabled = importJson.isNotBlank(),
                            isPrimary = true,
                            leadingIcon = Icons.Rounded.Check,
                        )
                        NazeButton(
                            text = "Clear",
                            onClick = {
                                importJson = ""
                                backupMessage = null
                            },
                        )
                    }
                    if (backupMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        NazeStatusLabel(
                            label = backupMessage!!,
                            color = if (backupOk) NazeColors.success else NazeColors.error,
                            icon = if (backupOk) {
                                Icons.Rounded.Check
                            } else {
                                Icons.Rounded.ErrorOutline
                            },
                        )
                    }
                }
            }
        }
        item {
            NazeCard {
                Column {
                    Text(
                        "Allowed applications",
                        style = NazeTypography.section,
                        color = NazeColors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "The agent can open and drive only the packages on this " +
                            "list. Every run is refused until at least one " +
                            "application is allowed.",
                        style = NazeTypography.caption,
                        color = NazeColors.textMuted,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (allowedList.isEmpty()) {
                        Text(
                            "No applications allowed.",
                            style = NazeTypography.body,
                            color = NazeColors.warning,
                        )
                    } else {
                        allowedList.forEach { packageName ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    packageName,
                                    style = NazeTypography.body.copy(
                                        color = NazeColors.textPrimary,
                                    ),
                                )
                                TextButton(
                                    onClick = {
                                        appsStore.remove(packageName)
                                        allowedList = appsStore.load()
                                    },
                                ) {
                                    Text("Remove", color = NazeColors.error)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    NazeTextField(
                        value = newPackage,
                        onValueChange = { newPackage = it },
                        placeholder = "com.example.app",
                        minLines = 1,
                    )
                    Spacer(Modifier.height(8.dp))
                    NazeButton(
                        text = "Add",
                        onClick = {
                            val ok = appsStore.add(newPackage.trim())
                            appOk = ok
                            appMessage = if (ok) {
                                allowedList = appsStore.load()
                                newPackage = ""
                                "Package added to the allowed list"
                            } else {
                                "Invalid or duplicate package name"
                            }
                        },
                        enabled = newPackage.isNotBlank(),
                        leadingIcon = Icons.Rounded.Check,
                    )
                    if (appMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        NazeStatusLabel(
                            label = appMessage!!,
                            color = if (appOk) NazeColors.success else NazeColors.error,
                            icon = if (appOk) {
                                Icons.Rounded.Check
                            } else {
                                Icons.Rounded.ErrorOutline
                            },
                        )
                    }
                }
            }
        }
        item {
            NazeCard {
                Column {
                    Text(
                        "About",
                        style = NazeTypography.section,
                        color = NazeColors.textPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    DetailRow("Application", "Naze Motion")
                    DetailRow(
                        "Version",
                        runCatching {
                            LocalContext.current.packageManager
                                .getPackageInfo(LocalContext.current.packageName, 0).versionName
                        }.getOrNull() ?: "unknown",
                    )
                    DetailRow("Target application", "Alight Motion")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Everything the agent does runs on this device. Keys, " +
                            "safety limits, allowed applications, and run history " +
                            "stay local, and they leave the device only when you " +
                            "export or share them yourself.",
                        style = NazeTypography.caption,
                        color = NazeColors.textMuted,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
