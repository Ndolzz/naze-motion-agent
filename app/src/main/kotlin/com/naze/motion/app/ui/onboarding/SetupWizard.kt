package com.naze.motion.app.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.naze.motion.app.agent.ApiKeyStore
import com.naze.motion.core.access.AccessibilityConnection
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.NazeStatusLabel
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeShapes
import com.naze.motion.app.ui.theme.NazeTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SetupWizard (PART 2): the beginner-friendly guided setup. Four steps:
 * device access, AI connection, connection test, ready. Every permission
 * is explained before it is requested, re-checked against the real device
 * state after the user returns from system settings, and nothing is
 * forced: each step can be skipped with a clear statement of what will
 * not work. Technical details are never the headline; raw provider errors
 * are only shown under a "Technical details" expander.
 */
private enum class WizardStep { DEVICE, AI, TEST, READY }

@Composable
fun SetupWizard(
    onFinished: () -> Unit,
    startStep: String = "device",
) {
    val context = LocalContext.current
    val store = remember { ApiKeyStore(context.applicationContext) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var step by remember {
        mutableStateOf(
            when (startStep) {
                "ai" -> WizardStep.AI
                "test" -> WizardStep.TEST
                else -> WizardStep.DEVICE
            },
        )
    }

    // Device state, re-checked continuously so the UI is correct the
    // moment the user returns from system settings.
    var accessOk by remember { mutableStateOf(AccessibilityConnection.connected) }
    var overlayOk by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            accessOk = AccessibilityConnection.connected
            overlayOk = Settings.canDrawOverlays(context)
            delay(500)
        }
    }

    // AI connection state, hoisted so it survives step changes.
    var providerId by remember { mutableStateOf(store.selectedId().ifBlank { ApiKeyStore.LOCAL }) }
    var keyText by remember { mutableStateOf("") }
    var modelCustom by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    var testStage by remember { mutableStateOf(0) }
    var testOk by remember { mutableStateOf(false) }
    var testFailure by remember { mutableStateOf<TestFailure?>(null) }

    // Back walks the wizard one step back; at the first step the default
    // Android behavior applies.
    BackHandler(enabled = step != WizardStep.DEVICE) {
        step = when (step) {
            WizardStep.AI -> WizardStep.DEVICE
            WizardStep.TEST -> WizardStep.AI
            WizardStep.READY -> WizardStep.TEST
            WizardStep.DEVICE -> WizardStep.DEVICE
        }
    }

    val providerEntry = remember(providerId) {
        ApiKeyStore.catalog.firstOrNull { it.id == providerId }
    }
    val isLocal = providerEntry?.kind == null

    fun openAccessibilitySettings() {
        val open = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(open) }
    }

    fun openOverlaySettings() {
        val open = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + context.packageName),
        )
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(open) }
    }

    fun saveAndTest() {
        val entry = providerEntry ?: return
        val kind = entry.kind ?: return
        // Never clobber a stored key or model with a blank field: the
        // wizard may run again later on top of an existing config.
        val storedBefore = store.load(providerId)
        val baseUrl = storedBefore.baseUrl.ifBlank { entry.defaultBaseUrl ?: "" }
        val model = modelCustom.trim().ifBlank {
            storedBefore.model.ifBlank { entry.defaultModel ?: "" }
        }
        val key = keyText.trim().ifBlank { storedBefore.apiKey }
        store.save(providerId, key, model, baseUrl)
        store.setSelected(providerId)
        step = WizardStep.TEST
        runTest(
            kind = kind,
            baseUrl = baseUrl,
            apiKey = key,
            model = model,
            scope = scope,
            setTesting = {
                testing = it
                if (it) {
                    testStage = 0
                    testFailure = null
                }
            },
            setResult = { ok, failure ->
                testOk = ok
                testFailure = failure
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        StepProgress(current = step.ordinal, total = 4)
        Spacer(Modifier.height(16.dp))
        when (step) {
            WizardStep.DEVICE -> DeviceStep(
                accessOk = accessOk,
                overlayOk = overlayOk,
                onEnableAccess = ::openAccessibilitySettings,
                onAllowOverlay = ::openOverlaySettings,
                onContinue = { step = WizardStep.AI },
            )
            WizardStep.AI -> AiStep(
                store = store,
                providerId = providerId,
                onSelectProvider = {
                    providerId = it
                    keyText = ""
                    modelCustom = ""
                },
                keyText = keyText,
                onKeyChange = { keyText = it },
                clipboardText = { clipboard.getText()?.text ?: "" },
                modelCustom = modelCustom,
                onModelCustomChange = { modelCustom = it },
                onUseLocal = {
                    store.setSelected(ApiKeyStore.LOCAL)
                    step = WizardStep.READY
                },
                onContinue = ::saveAndTest,
            )
            WizardStep.TEST -> TestStep(
                providerLabel = providerEntry?.label ?: "",
                isLocal = isLocal,
                testing = testing,
                testStage = testStage,
                testOk = testOk,
                testFailure = testFailure,
                onStageChange = { testStage = it },
                onTest = {
                    val entry = providerEntry ?: return@TestStep
                    val kind = entry.kind ?: return@TestStep
                    val storedBefore = store.load(providerId)
                    val baseUrl = storedBefore.baseUrl.ifBlank { entry.defaultBaseUrl ?: "" }
                    val model = modelCustom.trim().ifBlank {
                        storedBefore.model.ifBlank { entry.defaultModel ?: "" }
                    }
                    val key = keyText.trim().ifBlank { storedBefore.apiKey }
                    store.save(providerId, key, model, baseUrl)
                    store.setSelected(providerId)
                    runTest(
                        kind = kind,
                        baseUrl = baseUrl,
                        apiKey = key,
                        model = model,
                        scope = scope,
                        setTesting = {
                            testing = it
                            if (it) {
                                testStage = 0
                                testFailure = null
                            }
                        },
                        setResult = { ok, failure ->
                            testOk = ok
                            testFailure = failure
                        },
                    )
                },
                onContinue = { step = WizardStep.READY },
                onChooseModel = { step = WizardStep.AI },
            )
            WizardStep.READY -> ReadyStep(
                accessOk = accessOk,
                overlayOk = overlayOk,
                isLocal = store.selectedId() == ApiKeyStore.LOCAL,
                testOk = testOk,
                onFinished = onFinished,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** One real testConfig call; staged progress is driven by TestStep's pulse. */
private fun runTest(
    kind: com.naze.motion.core.ai.AiProviderKind,
    baseUrl: String,
    apiKey: String,
    model: String,
    scope: kotlinx.coroutines.CoroutineScope,
    setTesting: (Boolean) -> Unit,
    setResult: (Boolean, TestFailure?) -> Unit,
) {
    setTesting(true)
    scope.launch {
        val result = ApiKeyStore.testConfig(
            kind = kind,
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model,
        )
        result.fold(
            { setResult(true, null) },
            { e -> setResult(false, mapFailure(e)) },
        )
        setTesting(false)
    }
}

private data class TestFailure(
    val title: String,
    val message: String,
    val technical: String,
    val showChooseModel: Boolean,
    val showCheckKey: Boolean,
)

/** Friendly headline first; the raw HTTP detail stays in technical view. */
private fun mapFailure(e: Throwable): TestFailure {
    val raw = e.message ?: "unknown error"
    return when {
        raw.contains("incomplete configuration") ->
            TestFailure(
                title = "A few details are missing",
                message = "Add an API key and a model, then try again.",
                technical = raw,
                showChooseModel = false,
                showCheckKey = true,
            )
        raw.contains("http 401") || raw.contains("http 403") ->
            TestFailure(
                title = "We couldn't connect your AI",
                message = "Your API key may be invalid or no longer active.",
                technical = raw,
                showChooseModel = false,
                showCheckKey = true,
            )
        raw.contains("http 404") ->
            TestFailure(
                title = "This model is unavailable",
                message = "Choose another available model or try again later.",
                technical = raw,
                showChooseModel = true,
                showCheckKey = false,
            )
        raw.contains("http 429") || raw.contains("http 5") ->
            TestFailure(
                title = "AI temporarily unavailable",
                message = "The selected model is currently busy or unavailable. " +
                    "Try again or choose another model.",
                technical = raw,
                showChooseModel = true,
                showCheckKey = false,
            )
        raw.contains("network") || raw.contains("connect") ||
            raw.contains("resolve") || raw.contains("timed out") ->
            TestFailure(
                title = "You're offline",
                message = "Check your internet connection and try again.",
                technical = raw,
                showChooseModel = false,
                showCheckKey = false,
            )
        else ->
            TestFailure(
                title = "We couldn't connect your AI",
                message = "Something went wrong while talking to your AI. " +
                    "Try again, or choose another model.",
                technical = raw,
                showChooseModel = true,
                showCheckKey = false,
            )
    }
}

@Composable
private fun StepProgress(current: Int, total: Int) {
    Column(modifier = Modifier.semantics { contentDescription = "Setup step " + (current + 1) + " of " + total }) {
        Text(
            "SET UP NAZE MOTION",
            style = NazeTypography.label,
            color = NazeColors.textMuted,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            for (i in 0 until total) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            if (i <= current) NazeColors.primary else NazeColors.border,
                            CircleShape,
                        )
                        .semantics {
                            contentDescription =
                                if (i <= current) "Completed step " + (i + 1) else "Upcoming step " + (i + 1)
                        },
                )
                if (i < total - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (i < current) NazeColors.primary else NazeColors.border),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            (current + 1).toString() + " of " + total,
            style = NazeTypography.caption,
            color = NazeColors.textMuted,
        )
    }
}

@Composable
private fun HelpExpander(title: String, content: String) {
    var open by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { open = !open }) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = NazeColors.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(title, color = NazeColors.primary, style = NazeTypography.caption)
        }
        if (open) {
            Text(
                content,
                style = NazeTypography.caption,
                color = NazeColors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun DeviceStep(
    accessOk: Boolean,
    overlayOk: Boolean,
    onEnableAccess: () -> Unit,
    onAllowOverlay: () -> Unit,
    onContinue: () -> Unit,
) {
    Column {
        Text("Prepare your device", style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
        Spacer(Modifier.height(16.dp))

        Text("Let Naze Motion control supported apps", style = NazeTypography.section, color = NazeColors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "Naze Motion needs permission to interact with supported apps " +
                "and perform actions for you.",
            style = NazeTypography.body,
            color = NazeColors.textSecondary,
        )
        Spacer(Modifier.height(10.dp))
        if (accessOk) {
            NazeStatusLabel(label = "Access enabled", color = NazeColors.success, icon = Icons.Rounded.Check)
        } else {
            NazeStatusLabel(label = "Access not enabled", color = NazeColors.error, icon = Icons.Rounded.ErrorOutline)
        }
        Spacer(Modifier.height(10.dp))
        NazeButton(
            text = if (accessOk) "Open settings" else "Enable access",
            onClick = onEnableAccess,
            isPrimary = !accessOk,
            leadingIcon = Icons.Rounded.PlayArrow,
        )
        Spacer(Modifier.height(6.dp))
        HelpExpander(
            title = "Why do I need this?",
            content = "Accessibility access allows Naze Motion to detect and " +
                "interact with supported UI elements.",
        )

        Spacer(Modifier.height(24.dp))
        Text("Show Naze Motion over other apps", style = NazeTypography.section, color = NazeColors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            "This allows the Naze Agent to appear while you are using " +
                "another supported app.",
            style = NazeTypography.body,
            color = NazeColors.textSecondary,
        )
        Spacer(Modifier.height(10.dp))
        // Simple visualization: a supported app with the floating agent
        // on top of it.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(NazeColors.surface, NazeShapes.card)
                .border(1.dp, NazeColors.border, NazeShapes.card)
                .padding(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(NazeColors.surfaceElevated, NazeShapes.button)
                    .semantics { contentDescription = "Another app fills the screen" },
            ) {
                Text(
                    "Other app",
                    style = NazeTypography.caption,
                    color = NazeColors.textMuted,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(NazeColors.surfaceStrong, NazeShapes.button)
                    .border(1.dp, NazeColors.primary, NazeShapes.button)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .semantics { contentDescription = "Small floating Naze Agent card" },
            ) {
                Text("Naze Agent", style = NazeTypography.caption, color = NazeColors.primary)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (overlayOk) {
            NazeStatusLabel(label = "Floating Agent ready", color = NazeColors.success, icon = Icons.Rounded.Check)
        } else {
            NazeStatusLabel(label = "Floating Agent not allowed yet", color = NazeColors.warning, icon = Icons.Rounded.ErrorOutline)
        }
        Spacer(Modifier.height(10.dp))
        NazeButton(
            text = if (overlayOk) "Open settings" else "Allow access",
            onClick = onAllowOverlay,
            isPrimary = !overlayOk,
        )
        Spacer(Modifier.height(6.dp))
        HelpExpander(
            title = "What happens if I don't allow it?",
            content = "You can still explore Naze Motion, but automation that " +
                "requires device interaction will not be available.",
        )

        Spacer(Modifier.height(28.dp))
        NazeButton(
            text = "Continue",
            onClick = onContinue,
            isPrimary = true,
            enabled = accessOk && overlayOk,
            leadingIcon = Icons.Rounded.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!accessOk || !overlayOk) {
            TextButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("Continue without this for now", color = NazeColors.textMuted)
            }
        }
    }
}

@Composable
private fun AiStep(
    store: ApiKeyStore,
    providerId: String,
    onSelectProvider: (String) -> Unit,
    keyText: String,
    onKeyChange: (String) -> Unit,
    clipboardText: () -> String,
    modelCustom: String,
    onModelCustomChange: (String) -> Unit,
    onUseLocal: () -> Unit,
    onContinue: () -> Unit,
) {
    val entry = remember(providerId) { ApiKeyStore.catalog.firstOrNull { it.id == providerId } }
    val isLocal = entry?.kind == null
    val keySaved = remember(providerId) { store.hasKey(providerId) }
    var showCustomModel by remember { mutableStateOf(false) }

    Column {
        Text("Connect your AI", style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Naze Motion uses an AI model to understand your instructions " +
                "and generate automation actions.",
            style = NazeTypography.body,
            color = NazeColors.textSecondary,
        )
        Spacer(Modifier.height(16.dp))

        Text("Choose your AI provider", style = NazeTypography.section, color = NazeColors.textPrimary)
        Spacer(Modifier.height(8.dp))
        ApiKeyStore.catalog.forEach { catalog ->
            val selected = catalog.id == providerId
            val subtitle = when {
                catalog.kind == null -> "On device, no AI required"
                catalog.defaultBaseUrl != null && catalog.defaultModel != null -> "Recommended configuration"
                else -> "API compatible"
            }
            NazeCard(padding = 12.dp, onClick = { onSelectProvider(catalog.id) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            catalog.label,
                            style = NazeTypography.body.copy(
                                color = if (selected) NazeColors.primary else NazeColors.textPrimary,
                            ),
                        )
                        Text(subtitle, style = NazeTypography.caption, color = NazeColors.textMuted)
                    }
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = NazeColors.primary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (isLocal) {
            NazeCard {
                Column {
                    Text("No AI needed", style = NazeTypography.subtitle, color = NazeColors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Built-in templates plan simple tasks entirely on this " +
                            "device. You can connect an AI later in Settings.",
                        style = NazeTypography.caption,
                        color = NazeColors.textMuted,
                    )
                    Spacer(Modifier.height(10.dp))
                    NazeButton(text = "Use on-device templates", onClick = onUseLocal, isPrimary = true)
                }
            }
        } else if (entry != null && entry.kind != null) {
            Spacer(Modifier.height(16.dp))
            Text("Add your API key", style = NazeTypography.section, color = NazeColors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                "Your API key allows Naze Motion to communicate with " +
                    entry.label + ".",
                style = NazeTypography.body,
                color = NazeColors.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = keyText,
                onValueChange = onKeyChange,
                placeholder = {
                    Text(
                        if (keySaved) "A key is saved. Paste a new key to replace it." else "Paste your API key",
                        style = NazeTypography.body,
                        color = NazeColors.textMuted,
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
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
            val clip = clipboardText()
            if (clip.isNotBlank()) {
                TextButton(onClick = { onKeyChange(clip) }) {
                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = NazeColors.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste from clipboard", color = NazeColors.primary, style = NazeTypography.caption)
                }
            }
            HelpExpander(
                title = "Where do I get an API key?",
                content = "1. Open your " + entry.label + " account.\n" +
                    "2. Create or copy an API key.\n" +
                    "3. Return to Naze Motion.\n" +
                    "4. Paste it here.",
            )
            if (keySaved && keyText.isBlank()) {
                NazeStatusLabel(label = "API key saved securely", color = NazeColors.success, icon = Icons.Rounded.Key)
            }

            Spacer(Modifier.height(16.dp))
            Text("Choose your AI model", style = NazeTypography.section, color = NazeColors.textPrimary)
            Spacer(Modifier.height(8.dp))
            NazeCard(padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            entry.defaultModel ?: "Default model",
                            style = NazeTypography.body,
                            color = NazeColors.textPrimary,
                        )
                        Text(
                            "Default model for " + entry.label,
                            style = NazeTypography.caption,
                            color = NazeColors.textMuted,
                        )
                    }
                    NazeStatusLabel(label = "Recommended", color = NazeColors.primary, icon = Icons.Rounded.Check)
                }
            }
            HelpExpander(
                title = "Which one should I choose?",
                content = "The recommended model is a good starting point for " +
                    "most tasks. A different model can be set later in " +
                    "Settings under Advanced settings.",
            )
            if (showCustomModel) {
                OutlinedTextField(
                    value = modelCustom,
                    onValueChange = onModelCustomChange,
                    placeholder = {
                        Text("Model name", style = NazeTypography.body, color = NazeColors.textMuted)
                    },
                    singleLine = true,
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
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            } else {
                TextButton(onClick = { showCustomModel = true }) {
                    Text("Use a different model (advanced)", color = NazeColors.textMuted, style = NazeTypography.caption)
                }
            }
            Spacer(Modifier.height(20.dp))
            NazeButton(
                text = "Save and continue",
                onClick = onContinue,
                isPrimary = true,
                enabled = keyText.isNotBlank() || keySaved,
                leadingIcon = Icons.Rounded.Key,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TestStep(
    providerLabel: String,
    isLocal: Boolean,
    testing: Boolean,
    testStage: Int,
    testOk: Boolean,
    testFailure: TestFailure?,
    onStageChange: (Int) -> Unit,
    onTest: () -> Unit,
    onContinue: () -> Unit,
    onChooseModel: () -> Unit,
) {
    val stageMessages = listOf(
        "Connecting to your AI...",
        "Checking API key...",
        "Testing model...",
    )
    Column {
        Text("Test your connection", style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "This checks that " + (providerLabel.ifBlank { "your AI" }) +
                " is reachable and can plan actions.",
            style = NazeTypography.body,
            color = NazeColors.textSecondary,
        )
        Spacer(Modifier.height(16.dp))

        if (isLocal) {
            NazeStatusLabel(label = "On-device templates need no connection test", color = NazeColors.success, icon = Icons.Rounded.Check)
            Spacer(Modifier.height(20.dp))
            NazeButton(text = "Continue", onClick = onContinue, isPrimary = true, modifier = Modifier.fillMaxWidth())
            return@Column
        }

        if (testing) {
            NazeCard(padding = 12.dp) {
                Column {
                    val msg = stageMessages[testStage.coerceIn(0, stageMessages.size - 1)]
                    NazeStatusLabel(label = msg, color = NazeColors.primary, icon = Icons.Rounded.Bolt)
                }
            }
            // A short, bounded pulse so the progress message feels alive
            // while the real test runs in the background. No infinite loop.
            LaunchedEffect(testing) {
                for (s in 1..2) {
                    delay(700)
                    onStageChange(s)
                }
            }
        } else if (testOk) {
            NazeCard(padding = 12.dp) {
                Column {
                    NazeStatusLabel(label = "You're connected", color = NazeColors.success, icon = Icons.Rounded.Check)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Naze Motion can now communicate with your AI model.",
                        style = NazeTypography.body,
                        color = NazeColors.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            NazeButton(text = "Continue", onClick = onContinue, isPrimary = true, modifier = Modifier.fillMaxWidth(), leadingIcon = Icons.Rounded.PlayArrow)
        } else if (testFailure != null) {
            NazeCard(padding = 12.dp) {
                Column {
                    Text(testFailure.title, style = NazeTypography.section, color = NazeColors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(testFailure.message, style = NazeTypography.body, color = NazeColors.textSecondary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NazeButton(text = "Try again", onClick = onTest, isPrimary = true, leadingIcon = Icons.Rounded.Refresh)
                        if (testFailure.showCheckKey || testFailure.showChooseModel) {
                            NazeButton(
                                text = if (testFailure.showCheckKey) "Check API key" else "Choose another model",
                                onClick = onChooseModel,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    HelpExpander(title = "Technical details", content = testFailure.technical)
                }
            }
        } else {
            NazeButton(
                text = "Test connection",
                onClick = onTest,
                isPrimary = true,
                leadingIcon = Icons.Rounded.Bolt,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ReadyStep(
    accessOk: Boolean,
    overlayOk: Boolean,
    isLocal: Boolean,
    testOk: Boolean,
    onFinished: () -> Unit,
) {
    Column {
        Text("You're ready", style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Naze Motion is ready.",
            style = NazeTypography.section,
            color = NazeColors.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "You can now give Naze Motion your first automation instruction.",
            style = NazeTypography.body,
            color = NazeColors.textSecondary,
        )
        Spacer(Modifier.height(20.dp))
        ReadyCheck(label = "Device access", done = accessOk)
        Spacer(Modifier.height(6.dp))
        ReadyCheck(label = "Floating Agent", done = overlayOk)
        Spacer(Modifier.height(6.dp))
        if (isLocal) {
            ReadyCheck(label = "On-device templates", done = true)
        } else {
            ReadyCheck(label = "AI connection", done = testOk)
        }
        Spacer(Modifier.height(6.dp))
        ReadyCheck(label = "Model selected", done = true)
        Spacer(Modifier.height(28.dp))
        NazeButton(
            text = "Start using Naze Motion",
            onClick = onFinished,
            isPrimary = true,
            leadingIcon = Icons.Rounded.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReadyCheck(label: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label + if (done) ", completed" else ", not set up" },
    ) {
        Icon(
            if (done) Icons.Rounded.Check else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = if (done) NazeColors.success else NazeColors.textMuted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, style = NazeTypography.body, color = NazeColors.textPrimary)
        Spacer(Modifier.width(8.dp))
        Text(
            if (done) "Done" else "Not set up yet",
            style = NazeTypography.caption,
            color = NazeColors.textMuted,
        )
    }
}
