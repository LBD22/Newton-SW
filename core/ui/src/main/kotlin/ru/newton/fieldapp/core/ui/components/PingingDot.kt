package ru.newton.fieldapp.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Live-status dot with a pulsing halo ring, per Field Blue spec §7.4.
 *
 * Use only on indicators that represent something genuinely "live" (RTK Fix,
 * NTRIP stream active). Do not use as decoration — the animation steals
 * attention.
 *
 * @param color base colour (success for RTK Fix, warning for Float, etc.).
 * @param dotSize diameter of the inner solid dot. The ping ring extends 6 dp
 *   beyond the dot at rest and scales out to 14 dp away on each cycle.
 */
@Composable
fun PingingDot(
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 9.dp,
) {
    val infinite = rememberInfiniteTransition(label = "ping")
    val scale by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "ping-scale",
    )
    val alpha by infinite.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "ping-alpha",
    )

    Canvas(modifier.size(dotSize + 14.dp)) {
        val centre = Offset(size.width / 2, size.height / 2)
        val dotR = dotSize.toPx() / 2
        val baseRingR = dotR + 6.dp.toPx()

        // outer expanding ring
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = baseRingR * scale,
            center = centre,
            style = Stroke(1.dp.toPx()),
        )
        // static halo
        drawCircle(
            color = color.copy(alpha = 0.18f),
            radius = dotR + 3.dp.toPx(),
            center = centre,
        )
        // solid dot
        drawCircle(
            color = color,
            radius = dotR,
            center = centre,
        )
    }
}
