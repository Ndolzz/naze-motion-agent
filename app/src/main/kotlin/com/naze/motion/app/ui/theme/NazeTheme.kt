package com.naze.motion.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Precision Studio design system tokens.
 * Centralized. No screen may hard code colors, radii, or spacing.
 */
object NazeColors {
    val background = Color(0xFF0B0D10)
    val surface = Color(0xFF101318)
    val surfaceElevated = Color(0xFF151A20)
    val surfaceStrong = Color(0xFF1A2027)

    val border = Color(0xFF252B33)
    val borderStrong = Color(0xFF303741)

    val textPrimary = Color(0xFFF2F4F7)
    val textSecondary = Color(0xFFA6ADB7)
    val textMuted = Color(0xFF6F7782)
    val textDisabled = Color(0xFF4A515B)

    val primary = Color(0xFF4C8DFF)
    val success = Color(0xFF35C98A)
    val warning = Color(0xFFF2B84B)
    val error = Color(0xFFF06464)
    val info = Color(0xFF4C8DFF)
}

object NazeTypography {
    val display = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.S600(), lineHeight = 34.sp, color = NazeColors.textPrimary)
    val pageTitle = TextStyle(fontSize = 23.sp, fontWeight = FontWeight.S600(), lineHeight = 28.sp, color = NazeColors.textPrimary)
    val section = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.S600(), lineHeight = 22.sp, color = NazeColors.textPrimary)
    val body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.S400(), lineHeight = 20.sp, color = NazeColors.textSecondary)
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.S400(), lineHeight = 16.sp, color = NazeColors.textMuted)
    val technical = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp, color = NazeColors.textMuted)

    private fun FontWeight.Companion.S600() = FontWeight(600)
    private fun FontWeight.Companion.S400() = FontWeight(400)
}

data class NazeSpacing(
    val xs: androidx.compose.ui.unit.Dp = 4.dp,
    val sm: androidx.compose.ui.unit.Dp = 8.dp,
    val md: androidx.compose.ui.unit.Dp = 12.dp,
    val base: androidx.compose.ui.unit.Dp = 16.dp,
    val lg: androidx.compose.ui.unit.Dp = 20.dp,
    val xl: androidx.compose.ui.unit.Dp = 24.dp,
    val xxl: androidx.compose.ui.unit.Dp = 32.dp,
    val xxxl: androidx.compose.ui.unit.Dp = 40.dp,
    val huge: androidx.compose.ui.unit.Dp = 48.dp,
)

object NazeShapes {
    val small = RoundedCornerShape(6.dp)
    val button = RoundedCornerShape(8.dp)
    val card = RoundedCornerShape(10.dp)
    val container = RoundedCornerShape(14.dp)
}

object NazeAnimations {
    const val durationShort = 150
    const val durationDefault = 200
    const val durationLong = 250
}

val LocalNazeSpacing = staticCompositionLocalOf { NazeSpacing() }

private fun nazeShapes(): Shapes = Shapes(
    extraSmall = NazeShapes.small,
    small = NazeShapes.button,
    medium = NazeShapes.card,
    large = NazeShapes.container,
)

private fun nazeTypography(): Typography = Typography()

fun nazeColorScheme() = darkColorScheme(
    primary = NazeColors.primary,
    background = NazeColors.background,
    surface = NazeColors.surface,
    surfaceVariant = NazeColors.surfaceElevated,
    error = NazeColors.error,
    onPrimary = NazeColors.textPrimary,
    onBackground = NazeColors.textPrimary,
    onSurface = NazeColors.textPrimary,
    outline = NazeColors.border,
    outlineVariant = NazeColors.borderStrong,
)

@androidx.compose.runtime.Composable
fun NazeMotionTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
    MaterialTheme(
        colorScheme = nazeColorScheme(),
        typography = nazeTypography(),
        shapes = nazeShapes(),
        content = content,
    )
}
