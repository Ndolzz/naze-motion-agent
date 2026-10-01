package com.naze.motion.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.NazeDivider
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeTypography

/**
 * AppSettingsScreen (PART 2): the beginner-friendly Settings surface.
 * It offers the two things a normal user needs (Beginner Mode and the
 * guided setup) and keeps every technical option behind an explicit
 * Advanced settings area, which hosts the existing full SettingsScreen
 * unchanged. Structure: Settings -> Advanced, exactly as PART 2 asks.
 */
@Composable
fun AppSettingsScreen(
    onOpenSetup: () -> Unit,
    beginnerMode: Boolean,
    onBeginnerModeChange: (Boolean) -> Unit,
) {
    var advancedOpen by remember { mutableStateOf(false) }

    // Android Back from the advanced area returns to the simple
    // settings instead of leaving the app (PART 1 back priority).
    BackHandler(enabled = advancedOpen) { advancedOpen = false }

    if (advancedOpen) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Advanced settings", style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
                    Text("Technical configuration", style = NazeTypography.label, color = NazeColors.textMuted)
                }
                NazeButton(text = "Simple settings", onClick = { advancedOpen = false })
            }
            NazeDivider()
            // The existing full settings screen, unchanged, as the
            // advanced area.
            SettingsScreen()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("Settings", style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
        Spacer(Modifier.height(14.dp))

        NazeCard(padding = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Beginner mode", style = NazeTypography.subtitle, color = NazeColors.textPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Hides technical options that are not needed for everyday use.",
                        style = NazeTypography.caption,
                        color = NazeColors.textMuted,
                    )
                }
                Switch(
                    checked = beginnerMode,
                    onCheckedChange = onBeginnerModeChange,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        NazeCard(padding = 14.dp) {
            Column {
                Text("Guided setup", style = NazeTypography.subtitle, color = NazeColors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Run the short guided setup again if something changed on this device.",
                    style = NazeTypography.caption,
                    color = NazeColors.textMuted,
                )
                Spacer(Modifier.height(10.dp))
                NazeButton(
                    text = "Run setup again",
                    onClick = onOpenSetup,
                    isPrimary = true,
                    leadingIcon = Icons.Rounded.Refresh,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        NazeCard(padding = 14.dp) {
            Column {
                Text("Advanced", style = NazeTypography.section, color = NazeColors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    "These settings are intended for advanced users.",
                    style = NazeTypography.caption,
                    color = NazeColors.warning,
                )
                Spacer(Modifier.height(10.dp))
                NazeButton(
                    text = "Open advanced settings",
                    onClick = { advancedOpen = true },
                    leadingIcon = Icons.Rounded.Tune,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
