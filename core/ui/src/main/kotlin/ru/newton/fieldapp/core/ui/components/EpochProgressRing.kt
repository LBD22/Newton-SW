package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Circular epoch-counter ring shown on the survey-averaging screen, per spec §7.2.
 *
 * Visual layers (outside-in):
 *  1. Dashed guide-circle (topo-style decorative outer marker).
 *  2. Solid track ring in `surfaceDeep` — represents the "remaining" budget.
 *  3. Progress arc with cyan→brand gradient, rounded caps.
 *  4. Cardinal tick marks at 12/3/6/9 (12 o'clock is emphasised).
 *  5. Centre text overlay: big mono current epoch + small mono `/total` +
 *     uppercase label "эпох".
 *
 * @param current number of epochs already captured.
 * @param total target epoch count (denominator).
 * @param label text under the counter, usually "эпох". Set null to hide.
 */
@Composable
fun EpochProgressRing(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 200.dp,
    label: String? = "эпох",
) {
    val colors = NewtonTheme.colors
    val mono = NewtonTheme.mono
    val typography = MaterialTheme.typography

    val progressFraction = if (total > 0) current.coerceIn(0, total).toFloat() / total else 0f

    val arcBrush = remember(colors.brand) {
        Brush.verticalGradient(
            0f to Color(0xFF22D3EE), // cyan luminance accent (allowed in gradients only)
            1f to colors.brand,
        )
    }

    Box(modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val maxR = min(size.width, size.height) / 2

            val guideR = maxR * 0.94f
            val trackR = maxR * 0.82f
            val trackStrokePx = 12.dp.toPx()

            // 1. dashed guide-circle
            drawCircle(
                color = colors.brandDeep.copy(alpha = 0.25f),
                radius = guideR,
                center = Offset(cx, cy),
                style = Stroke(
                    width = 0.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 3f)),
                ),
            )

            // 2. track ring
            drawCircle(
                color = colors.surfaceDeep,
                radius = trackR,
                center = Offset(cx, cy),
                style = Stroke(trackStrokePx),
            )

            // 3. progress arc — start at 12 o'clock (top), sweep clockwise
            if (progressFraction > 0f) {
                val sweep = progressFraction * 360f
                drawArc(
                    brush = arcBrush,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(cx - trackR, cy - trackR),
                    size = Size(trackR * 2, trackR * 2),
                    style = Stroke(width = trackStrokePx, cap = StrokeCap.Round),
                )
            }

            // 4. cardinal tick marks
            val tickInsetPx = 4.dp.toPx()
            val tickLenMajor = 10.dp.toPx()
            val tickLenMinor = 6.dp.toPx()
            for (angleDeg in 0 until 360 step 90) {
                val rad = Math.toRadians((angleDeg - 90).toDouble())
                val outR = trackR + trackStrokePx / 2 + tickInsetPx
                val len = if (angleDeg == 0) tickLenMajor else tickLenMinor
                val inR = outR + len
                drawLine(
                    color = colors.brandDeep.copy(alpha = if (angleDeg == 0) 0.85f else 0.4f),
                    start = Offset(
                        cx + (outR * cos(rad)).toFloat(),
                        cy + (outR * sin(rad)).toFloat(),
                    ),
                    end = Offset(
                        cx + (inR * cos(rad)).toFloat(),
                        cy + (inR * sin(rad)).toFloat(),
                    ),
                    strokeWidth = if (angleDeg == 0) 1.4.dp.toPx() else 0.8.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        // 5. centre text overlay
        Column(
            modifier = Modifier.wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = current.toString(),
                    style = mono.monoXl,
                    color = colors.brandDeep,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "/$total",
                    style = mono.monoLarge.copy(fontSize = 18.sp),
                    color = colors.text2,
                    modifier = Modifier.wrapContentSize(),
                )
            }
            if (label != null) {
                Text(
                    text = label.uppercase(),
                    style = typography.labelMedium,
                    color = colors.text2,
                )
            }
        }
    }
}
