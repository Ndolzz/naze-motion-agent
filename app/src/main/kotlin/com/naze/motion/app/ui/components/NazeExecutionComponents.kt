package com.naze.motion.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeShapes
import com.naze.motion.app.ui.theme.NazeTypography

/** Execution timeline item states: pending, active, success, failed, recovering. */
enum class TimelineItemState { PENDING, ACTIVE, SUCCESS, FAILED, RECOVERING }

@Composable
fun NazeActionTimeline(
    items: List<Pair<String, TimelineItemState>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, (label, state) ->
            NazeActionRow(index = index + 1, label = label, state = state)
        }
    }
}

@Composable
fun NazeActionRow(index: Int, label: String, state: TimelineItemState) {
    val labelColor = when (state) {
        TimelineItemState.SUCCESS -> NazeColors.textMuted
        TimelineItemState.ACTIVE -> NazeColors.textPrimary
        TimelineItemState.RECOVERING -> NazeColors.warning
        TimelineItemState.FAILED -> NazeColors.error
        TimelineItemState.PENDING -> NazeColors.textDisabled
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Text(
            String.format("%02d", index),
            style = NazeTypography.technical,
            color = NazeColors.textDisabled,
            modifier = Modifier.width(28.dp),
        )
        TimelineStateIndicator(state)
        Spacer(Modifier.width(12.dp))
        Text(label, style = NazeTypography.body.copy(color = labelColor))
    }
}

@Composable
private fun TimelineStateIndicator(state: TimelineItemState) {
    val size = 18.dp
    when (state) {
        TimelineItemState.SUCCESS -> Icon(
            Icons.Rounded.Check, contentDescription = "completed",
            tint = NazeColors.success, modifier = Modifier.size(size),
        )
        TimelineItemState.ACTIVE -> NazeStatusDot(NazeColors.primary, description = "current action")
        TimelineItemState.RECOVERING -> Icon(
            Icons.Rounded.Refresh, contentDescription = "recovering",
            tint = NazeColors.warning, modifier = Modifier.size(size),
        )
        TimelineItemState.FAILED -> NazeStatusDot(NazeColors.error, description = "failed")
        TimelineItemState.PENDING -> Box(
            modifier = Modifier.size(size).border(1.dp, NazeColors.border, CircleShape)
        )
    }
}

/** Monospace technical log. Expandable by caller. */
@Composable
fun NazeLog(entries: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NazeColors.surface, NazeShapes.card)
            .padding(12.dp),
    ) {
        entries.forEach { (time, event) ->
            Row {
                Text(
                    time,
                    style = NazeTypography.technical,
                    color = NazeColors.textDisabled,
                    modifier = Modifier.widthIn(max = 64.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(event, style = NazeTypography.technical, color = NazeColors.textMuted)
            }
        }
    }
}

/** Small progress indicator with status text. No big loading animations. */
@Composable
fun NazeProgress(label: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            color = NazeColors.primary,
            trackColor = NazeColors.border,
            modifier = Modifier.width(56.dp).height(2.dp).clip(RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Text(label, style = NazeTypography.caption, color = NazeColors.textSecondary)
    }
}
