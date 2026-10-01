package com.naze.motion.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naze.motion.app.ui.theme.NazeAnimations
import com.naze.motion.app.ui.theme.NazeColors
import com.naze.motion.app.ui.theme.NazeTypography

/**
 * NazeSplash (PART 1): the studio animated splash. Sequence: the motion
 * path draws an N, keyframe nodes appear at both ends, the wordmark and
 * tagline fade in, then the whole splash fades into the application.
 * Total on screen time stays inside 1000-1600 ms and the animation is
 * lightweight: one Canvas and two Text nodes, no particles, no loop.
 */
@Composable
fun NazeSplash(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            animationSpec = tween(durationMillis = NazeAnimations.splashDurationMs, easing = FastOutSlowInEasing),
        )
        fade.animateTo(0f, animationSpec = tween(durationMillis = NazeAnimations.durationLong))
        onFinished()
    }
    val t = progress.value
    Box(
        modifier = Modifier.fillMaxSize().alpha(fade.value),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val w = size.width
                val h = size.height
                val a = Offset(w * 0.30f, h * 0.70f)
                val b = Offset(w * 0.30f, h * 0.22f)
                val c = Offset(w * 0.70f, h * 0.70f)
                val d = Offset(w * 0.70f, h * 0.22f)
                val seg1 = (a - b).getDistance()
                val seg2 = (b - c).getDistance()
                val seg3 = (c - d).getDistance()
                val total = seg1 + seg2 + seg3
                var remaining = t * total
                val stroke = 6.dp.toPx()
                if (remaining > 0f) {
                    val f = (remaining / seg1).coerceAtMost(1f)
                    drawLine(NazeColors.primary, a, a + (b - a) * f, strokeWidth = stroke, cap = StrokeCap.Round)
                }
                remaining -= seg1
                if (remaining > 0f) {
                    val f = (remaining / seg2).coerceAtMost(1f)
                    drawLine(NazeColors.primary, b, b + (c - b) * f, strokeWidth = stroke, cap = StrokeCap.Round)
                }
                remaining -= seg2
                if (remaining > 0f) {
                    val f = (remaining / seg3).coerceAtMost(1f)
                    drawLine(NazeColors.primary, c, c + (d - c) * f, strokeWidth = stroke, cap = StrokeCap.Round)
                }
                // Keyframe nodes at both ends of the motion path.
                if (t > 0.08f) drawCircle(NazeColors.motion, radius = 4.dp.toPx(), center = a)
                if (t > 0.90f) drawCircle(NazeColors.motion, radius = 4.dp.toPx(), center = d)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "NAZE MOTION",
                style = NazeTypography.display.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                color = NazeColors.textPrimary.copy(alpha = ((t - 0.55f) / 0.2f).coerceIn(0f, 1f)),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "MOTION AGENT",
                style = NazeTypography.label.copy(letterSpacing = 3.sp),
                color = NazeColors.textMuted.copy(alpha = ((t - 0.75f) / 0.2f).coerceIn(0f, 1f)),
            )
        }
    }
}
