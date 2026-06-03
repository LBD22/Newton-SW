package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Compass-rose watermark used in the corner of status-card per spec §7.3.
 *
 * Intentionally low-contrast (default opacity 0.07): adds cartographic identity
 * to every screen without competing with content. Position via the parent's
 * negative-inset offset (`offset(x = (-10).dp, y = (-10).dp)`).
 *
 * @param color base ink (defaults to brand-deep).
 * @param alpha overall opacity — keep below 0.10 unless you want the watermark
 *   to become a focal point.
 */
@Composable
fun TopoRoseWatermark(
    modifier: Modifier = Modifier,
    color: Color = NewtonTheme.colors.brandDeep,
    alpha: Float = 0.07f,
) {
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val outerR = minOf(size.width, size.height) / 2 * 0.92f
        val midR = outerR * 0.78f

        // outer thin ring (rose perimeter)
        drawCircle(
            color = color.copy(alpha = alpha * 4),
            radius = outerR,
            center = Offset(cx, cy),
            style = Stroke(1.dp.toPx()),
        )

        // inner dashed ring (chart-style)
        drawCircle(
            color = color.copy(alpha = alpha * 2.5f),
            radius = midR,
            center = Offset(cx, cy),
            style = Stroke(
                width = 0.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 2f)),
            ),
        )

        // 4-point rose diamonds (one path, rotated four times)
        val diamond = Path().apply {
            val tipY = cy - outerR * 0.85f
            val midOff = outerR * 0.06f
            moveTo(cx, tipY)
            lineTo(cx + midOff, cy - midOff)
            lineTo(cx, cy)
            lineTo(cx - midOff, cy - midOff)
            close()
        }
        listOf(0f, 90f, 180f, 270f).forEach { deg ->
            rotate(deg, Offset(cx, cy)) {
                drawPath(diamond, color = color.copy(alpha = alpha * 5))
            }
        }
    }
}

