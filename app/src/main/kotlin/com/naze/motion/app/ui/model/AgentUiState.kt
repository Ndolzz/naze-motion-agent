package com.naze.motion.app.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.naze.motion.app.ui.components.TimelineItemState
import com.naze.motion.app.ui.theme.NazeColors

/**
 * Explicit UI state models. No overlapping booleans.
 * Screens render from these sealed states only.
 */
sealed interface AgentUiState {
    val statusLabel: String
    val statusColor: Color
    val statusIcon: ImageVector

    data object Idle : AgentUiState {
        override val statusLabel = "READY"
        override val statusColor = NazeColors.success
        override val statusIcon = Icons.Rounded.CheckCircle
    }

    data object Planning : AgentUiState {
        override val statusLabel = "PLANNING"
        override val statusColor = NazeColors.info
        override val statusIcon = Icons.Rounded.Search
    }

    data object Validating : AgentUiState {
        override val statusLabel = "VALIDATING"
        override val statusColor = NazeColors.info
        override val statusIcon = Icons.Rounded.Settings
    }

    data object Executing : AgentUiState {
        override val statusLabel = "EXECUTING"
        override val statusColor = NazeColors.primary
        override val statusIcon = Icons.Rounded.PlayArrow
    }

    data object Recovering : AgentUiState {
        override val statusLabel = "RECOVERING"
        override val statusColor = NazeColors.warning
        override val statusIcon = Icons.Rounded.Refresh

    }

    data object Completed : AgentUiState {
        override val statusLabel = "COMPLETED"
        override val statusColor = NazeColors.success
        override val statusIcon = Icons.Rounded.CheckCircle
    }

    data class Failed(val reason: String) : AgentUiState {
        override val statusLabel = "FAILED"
        override val statusColor = NazeColors.error
        override val statusIcon = Icons.Rounded.ErrorOutline
    }

    data object Cancelled : AgentUiState {
        override val statusLabel = "CANCELLED"
        override val statusColor = NazeColors.textMuted
        override val statusIcon = Icons.Rounded.Cancel
    }
}

/** One entry in an execution timeline. */
data class TimelineEntry(
    val label: String,
    val state: TimelineItemState,
)

/** Mock preview data while the execution engine is not wired yet. */
object MockData {
    val plan = listOf(
        "Open Alight Motion",
        "Create 1080 x 1920 project",
        "Import video.mp4",
        "Add text NAZE",
        "Apply scale animation",
        "Export",
    )

    fun timeline(currentIndex: Int, failed: Boolean = false): List<TimelineEntry> =
        plan.mapIndexed { i, label ->
            val state = when {
                i < currentIndex -> TimelineItemState.SUCCESS
                i == currentIndex && failed -> TimelineItemState.FAILED
                i == currentIndex -> TimelineItemState.ACTIVE
                else -> TimelineItemState.PENDING
            }
            TimelineEntry(label, state)
        }

    val log = listOf(
        "10:42:01" to "ACTION_STARTED",
        "10:42:02" to "TARGET_FOUND",
        "10:42:02" to "CLICK_EXECUTED",
        "10:42:03" to "OBSERVATION_CAPTURED",
        "10:42:03" to "VERIFICATION_PASSED",
    )

    val recentWorkflows = listOf(
        "Cinematic Intro" to "8 actions · 2 min ago",
        "Naze Promo" to "6 actions · yesterday",
    )

    val history = listOf(
        HistoryItem("Cinematic Intro", "Completed", "10:4
2", true),
        HistoryItem("Naze Promo", "Failed", "09:21", false),
        HistoryItem("Logo Animation", "Completed", "Yesterday", true),
    )

    val systemStatus = listOf(
        Triple("Accessibility", "Connected", NazeColors.success),
        Triple("Alight Motion", "Detected", NazeColors.success),
        Triple("AI Provider", "Connected", NazeColors.success),
        Triple("Automation", "Ready", NazeColors.primary),
    )
}

data class HistoryItem(
    val name: String,
    val outcome: String,
    val time: String,
    val completed: Boolean,
)
