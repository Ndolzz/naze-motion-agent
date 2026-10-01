package com.naze.motion.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.naze.motion.app.R
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeTypography

/**
 * WelcomeScreen (PART 2): the first-launch greeting. Explains what Naze
 * Motion does in plain language before any configuration appears
 * ("Explain first, configure second"). Get Started opens the guided
 * setup; Skip setup enters the app with a clear reminder later.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(64.dp))
        Image(
            painter = painterResource(R.drawable.naze_mark),
            contentDescription = "Naze Motion mark",
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "NAZE MOTION",
            style = NazeTypography.display.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
            color = NazeColors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Your AI motion assistant.",
            style = NazeTypography.subtitle,
            color = NazeColors.textSecondary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Create and automate motion tasks with the help of AI.",
            style = NazeTypography.body,
            color = NazeColors.textMuted,
        )
        Spacer(Modifier.height(36.dp))
        WelcomeBenefit(
            icon = Icons.Rounded.AutoAwesome,
            title = "AI Assistance",
            description = "Help turn instructions into actions.",
        )
        Spacer(Modifier.height(14.dp))
        WelcomeBenefit(
            icon = Icons.Rounded.Layers,
            title = "Motion Automation",
            description = "Let Naze Motion interact with supported apps.",
        )
        Spacer(Modifier.height(14.dp))
        WelcomeBenefit(
            icon = Icons.Rounded.Save,
            title = "Simple Workflows",
            description = "Save tasks and run them again.",
        )
        Spacer(Modifier.height(40.dp))
        NazeButton(
            text = "Get Started",
            onClick = onGetStarted,
            isPrimary = true,
            leadingIcon = Icons.Rounded.PlayArrow,
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("Skip setup", color = NazeColors.textMuted)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WelcomeBenefit(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = title + ": " + description },
    ) {
        Icon(icon, contentDescription = null, tint = NazeColors.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = NazeTypography.subtitle, color = NazeColors.textPrimary)
            Text(description, style = NazeTypography.caption, color = NazeColors.textMuted)
        }
    }
}
