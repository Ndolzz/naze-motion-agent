package com.naze.motion.app.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Precision Studio design system tokens (PART 1). Centralized: no screen
 * may hard code colors, radii, spacing, or motion values.
 *
 * NazeColors is state backed so the whole tree, including legacy screens,
 * follows the active palette published by NazeMotionTheme.
 */
object NazeColors {
    var background by mutableStateOf(Color(0xFF0B0D10))
    var surface by mutableStateOf(Color(0xFF101318))
    var surfaceElevated by mutableStateOf(Color(0xFF151A20))
    var surfaceStrong by mutableStateOf(Color(0xFF1A2027))

    var border by mutableStateOf(Color(0xFF252B33))
    var borderStrong by mutableStateOf(Color(0xFF303741))

    var textPrimary by mutableStateOf(Color(0xFFF2F4F7))
    var textSecondary by mutableStateOf(Color(0xFFA6ADB7))
    var textMuted by mutableStateOf(Color(0xFF6F7782))
    var textDisabled by mutableStateOf(Color(0xFF4A515B))

    var primary by mutableStateOf(Color(0xFF4C8DFF))
    var secondary by mutableStateOf(Color(0xFF7C6CF0))
    var motion by mutableStateOf(Color(0xFF22D3EE))
    var success by mutableStateOf(Color(0xFF35C98A))
    var warning by mutableStateOf(Color(0xFFF2B84B))
    var error by mutableStateOf(Color(0xFFF06464))
    var info by mutableStateOf(Color(0xFF4C8DFF))
}

/**
 * Typography hierarchy: Display, Headline (pageTitle), Title (section),
 * Subtitle, Body, Label, Caption, Numeric, Technical. Text colors are no
 * longer baked into the styles: text follows the themed content color.
 */
object NazeTypography {
    val display = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp, letterSpacing = 0.5.sp)
    val pageTitle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
    val section = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
    val subtitle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    val body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val label = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp, letterSpacing = 1.sp)
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)
    val numeric = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
    val technical = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
}

data class NazeSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val base: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    val huge: Dp = 48.dp,
)

object NazeShapes {
    val small = RoundedCornerShape(6.dp)
    val button = RoundedCornerShape(8.dp)
    val card = RoundedCornerShape(10.dp)
    val container = RoundedCornerShape(14.dp)
}

/** Motion tokens: durations in ms plus shared easing curves. */
object NazeAnimations {
    const val durationShort = 150
    const val durationDefault = 200
    const val durationLong = 250
    const val splashDurationMs = 1500

    val easingStandard = FastOutSlowInEasing
    val easingDecelerate = LinearOutSlowInEasing
}

val LocalNazeSpacing = staticCompositionLocalOf { NazeSpacing() }

private fun nazeShapes(): Shapes = Shapes(
    extraSmall = NazeShapes.small,
    small = NazeShapes.button,
    medium = NazeShapes.card,
    large = NazeShapes.container,
)

fun nazeColorScheme() = darkColorScheme(
    primary = NazePalettes.dark.primary,
    background = NazePalettes.dark.background,
    surface = NazePalettes.dark.surface,
    surfaceVariant = NazePalettes.dark.surfaceElevated,
    error = NazePalettes.dark.error,
    onPrimary = NazePalettes.dark.textPrimary,
    onBackground = NazePalettes.dark.textPrimary,
    onSurface = NazePalettes.dark.textPrimary,
    outline = NazePalettes.dark.border,
    outlineVariant = NazePalettes.dark.borderStrong,
)

fun nazeLightColorScheme() = lightColorScheme(
    primary = NazePalettes.light.primary,
    background = NazePalettes.light.background,
    surface = NazePalettes.light.surface,
    surfaceVariant = NazePalettes.light.surfaceStrong,
    error = NazePalettes.light.error,
    onPrimary = Color.White,
    onBackground = NazePalettes.light.textPrimary,
    onSurface = NazePalettes.light.textPrimary,
    outline = NazePalettes.light.border,
    outlineVariant = NazePalettes.light.borderStrong,
)

/**
 * PART 1 theme entry point. Dark is the default studio look; SYSTEM
 * follows the OS; LIGHT is the designed light palette. The active palette
 * is published into NazeColors so every screen follows the theme.
 */
@Composable
fun NazeMotionTheme(
    mode: NazeThemeMode = NazeThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        NazeThemeMode.SYSTEM -> isSystemInDarkTheme()
        NazeThemeMode.DARK -> true
        NazeThemeMode.LIGHT -> false
    }
    (if (dark) NazePalettes.dark else NazePalettes.light).applyToColors()
    MaterialTheme(
        colorScheme = if (dark) nazeColorScheme() else nazeLightColorScheme(),
        typography = Typography(),
        shapes = nazeShapes(),
        content = content,
    )
}
