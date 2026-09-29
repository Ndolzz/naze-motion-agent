package com.naze.motion.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.naze.motion.app.ui.theme.NazeAnimations
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeShapes
import com.naze.motion.app.ui.theme.NazeSpacing
import com.naze.motion.app.ui.theme.NazeTypography

/** Buttons. Idle, pressed, executing states handled by Material with token colors. */
@Composable
fun NazeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = NazeShapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) NazeColors.primary else NazeColors.surfaceElevated,
            contentColor = if (isPrimary) NazeColors.textPrimary else NazeColors.textSecondary,
        ),
        modifier = modifier.height(44.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = NazeTypography.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
    }
}

@Composable
fun NazeIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = NazeColors.textSecondary,
) {
    IconButton(onClick = onClick, modifier = modifier.size(44.dp)) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
fun NazeSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = NazeTypography.section, color = NazeColors.textPrimary)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun NazeCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.ui.unit.Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val base = modifier
        .fillMaxWidth()
        .background(NazeColors.surface, NazeShapes.card)
        .border(1.dp, NazeColors.border, NazeShapes.card)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(padding)
    Box(modifier = base) { content() }
}

@Composable
fun NazeStatusDot(color: Color, modifier: Modifier = Modifier, description: String? = null) {
    Box(
        modifier = modifier
            .size(8.dp)
            .background(color, CircleShape)
            .then(
                if (description != null) Modifier.semantics { contentDescription = description }
                else Modifier
            ),
    )
}

@Composable
fun NazeStatusLabel(
    label: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics { contentDescription = label + " status" },
    ) {
        NazeStatusDot(color, description = label)
        Spacer(Modifier.width(6.dp))
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = NazeTypography.caption.copy(color = color))
    }
}

@Composable
fun NazeDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = NazeColors.border,
    )
}

@Composable
fun NazeEmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
            .semantics { contentDescription = title },
    ) {
        Icon(icon, contentDescription = null, tint = NazeColors.textMuted, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = NazeTypography.section, color = NazeColors.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            style = NazeTypography.body,
            color = NazeColors.textMuted,
            modifier = Modifier.width(240.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onAction) {
                Text(actionLabel, color = NazeColors.primary)
            }
        }
    }
}

@Composable
fun NazeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 3,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(placeholder, style = NazeTypography.body, color = NazeColors.textMuted)
        },
        minLines = minLines,
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
        modifier = modifier.fillMaxWidth(),
    )
}
