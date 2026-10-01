package com.naze.motion.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * NazePalette (PART 1 design system): the complete token set for one
 * theme. The dark palette is the canonical studio identity; the light
 * palette is designed on its own terms (not an inversion): deeper accent
 * hues for contrast, soft cool surfaces, and the same semantic meaning
 * for every color. applyToColors() publishes the palette into the
 * NazeColors token object so every legacy screen follows the active
 * theme without per-file rewrites.
 */
data class NazePalette(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceStrong: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textDisabled: Color,
    val primary: Color,
    val secondary: Color,
    val motion: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
) {
    fun applyToColors() {
        NazeColors.background = background
        NazeColors.surface = surface
        NazeColors.surfaceElevated = surfaceElevated
        NazeColors.surfaceStrong = surfaceStrong
        NazeColors.border = border
        NazeColors.borderStrong = borderStrong
        NazeColors.textPrimary = textPrimary
        NazeColors.textSecondary = textSecondary
        NazeColors.textMuted = textMuted
        NazeColors.textDisabled = textDisabled
        NazeColors.primary = primary
        NazeColors.secondary = secondary
        NazeColors.motion = motion
        NazeColors.success = success
        NazeColors.warning = warning
        NazeColors.error = error
        NazeColors.info = info
    }
}

object NazePalettes {

    /** Cinematic studio dark: layered cool surfaces, electric accents. */
    val dark = NazePalette(
        background = Color(0xFF0B0D10),
        surface = Color(0xFF101318),
        surfaceElevated = Color(0xFF151A20),
        surfaceStrong = Color(0xFF1A2027),
        border = Color(0xFF252B33),
        borderStrong = Color(0xFF303741),
        textPrimary = Color(0xFFF2F4F7),
        textSecondary = Color(0xFFA6ADB7),
        textMuted = Color(0xFF6F7782),
        textDisabled = Color(0xFF4A515B),
        primary = Color(0xFF4C8DFF),
        secondary = Color(0xFF7C6CF0),
        motion = Color(0xFF22D3EE),
        success = Color(0xFF35C98A),
        warning = Color(0xFFF2B84B),
        error = Color(0xFFF06464),
        info = Color(0xFF4C8DFF),
    )

    /** Designed light palette: paper-cool surfaces, deepened accents. */
    val light = NazePalette(
        background = Color(0xFFF4F6FA),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFFFFFFF),
        surfaceStrong = Color(0xFFE9EDF3),
        border = Color(0xFFD8DEE8),
        borderStrong = Color(0xFFC3CBD8),
        textPrimary = Color(0xFF171B21),
        textSecondary = Color(0xFF4A5361),
        textMuted = Color(0xFF7B8494),
        textDisabled = Color(0xFFAEB5C2),
        primary = Color(0xFF2563EB),
        secondary = Color(0xFF6D5AE8),
        motion = Color(0xFF0891B2),
        success = Color(0xFF15803D),
        warning = Color(0xFFB45309),
        error = Color(0xFFDC2626),
        info = Color(0xFF2563EB),
    )
}

/** Theme mode selector. Dark is the studio default. */
enum class NazeThemeMode { SYSTEM, DARK, LIGHT }
