package com.naze.motion.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.naze.motion.app.ui.components.NazeActionTimeline
import com.naze.motion.app.ui.components.NazeButton
import com.naze.motion.app.ui.components.NazeCard
import com.naze.motion.app.ui.components.TimelineItemState
import com.naze.motion.app.ui.model.MockData
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeTypography

/**
 * Plan review screen. Nothing runs until the user confirms.
 * The engine is not wired yet, so the plan is preview data.
 */
@Composable
fun PlanningScreen(
    instruction: String,
    onRun: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(instruction, style = NazeTypography.pageTitle, color = NazeColors.textPrimary)
        Spacer(Modifier.height(16.dp))
        Column {
            Text("Plan", style = NazeTypography.section, color = NazeColors.textPrimary)
            Spacer(Modifier.height(12.dp))
            NazeCard {
                NazeActionTimeline(
                    MockData.plan.map { it to TimelineItemState.PENDING },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Estimated actions: " + MockData.plan.size,
            style = NazeTypography.caption,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NazeButton(text = "Cancel", onClick = onCancel)
            NazeButton(text = "Run", onClick = onRun, isPrimary = true, leadingIcon = Icons.Rounded.PlayArrow)
        }
    }
}
