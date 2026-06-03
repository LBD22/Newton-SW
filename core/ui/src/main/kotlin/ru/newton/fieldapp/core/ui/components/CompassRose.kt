package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Multi-layer Stakeout compass rose per Field Blue spec §7.1.
 *
 * Visual layers (drawn outside-in on Canvas, then cardinal labels overlaid
 * as proper Compose Text for crisp font rendering):
 *  1. Radial gradient background (`#F7FAFD → #FFFFFF → #F0F4FA`).
 *  2. Outer stroke + thin inner stroke (brand-deep, 2 dp + 0.5 dp).
 *  3. Two dashed rings at r×0.78 and r×0.55 (chart-style markers).
 *  4. Cardinal tick marks N/S/E/W (S/E/W dimmed).
 *  5. 30°/60° middle tick marks.
 *  6. Minor 10° tick marks (subtle, dense).
 *  7. 4-point inner rose decoration (very low opacity).
 *  8. Target circle at bearing direction (dashed outline + filled centre).
 *  9. Bearing arrow with cyan→brand vertical gradient + dim 180° tail.
 *  10. Centre bullseye (white / navy / brand / white).
 *
 * @param bearing direction the arrow points, in degrees clockwise from N.
 *  0 = arrow points up. Pass 47f for "the point is to the NE-ish".
 * @param onTarget when true (point inside tolerance), the bearing arrow flips
 *  from the cool cyan→brand gradient to a solid success-green fill. The target
 *  circle also turns success-green to reinforce the "you can stop walking" cue.
 */
@Composable
fun CompassRose(
    bearing: Float,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 280.dp,
    onTarget: Boolean = false,
) {
    val colors = NewtonTheme.colors

    val bgBrush = remember(colors.surface) {
        Brush.radialGradient(
            0f to Color(0xFFF7FAFD),
            0.7f to Color(0xFFFFFFFF),
            1f to Color(0xFFF0F4FA),
        )
    }

    val arrowBrush = remember(colors.brand, onTarget) {
        if (onTarget) {
            Brush.verticalGradient(0f to colors.success, 1f to colors.success)
        } else {
            Brush.verticalGradient(
                0f to Color(0xFF22D3EE), // cyan stop — allowed inside SVG gradients per spec
                1f to colors.brand,
            )
        }
    }
    val targetTint = if (onTarget) colors.success else colors.brand

    Box(modifier.size(sizeDp)) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val maxDim = min(size.width, size.height) / 2
            val outerR = maxDim * 0.94f // leave room for tick overshoot

            // ── 1. background gradient ────────────────────────────────────
            drawCircle(brush = bgBrush, radius = outerR, center = Offset(cx, cy))

            // ── 2. outer + inner strokes ─────────────────────────────────
            drawCircle(
                color = colors.brandDeep,
                radius = outerR,
                center = Offset(cx, cy),
                style = Stroke(2.dp.toPx()),
            )
            drawCircle(
                color = colors.brandDeep,
                radius = outerR - 10.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(0.5.dp.toPx()),
            )

            // ── 3. dashed rings ──────────────────────────────────────────
            listOf(0.78f to 0.45f, 0.55f to 0.30f).forEach { (factor, ringAlpha) ->
                drawCircle(
                    color = colors.brandDeep.copy(alpha = ringAlpha),
                    radius = outerR * factor,
                    center = Offset(cx, cy),
                    style = Stroke(
                        width = 0.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 3f)),
                    ),
                )
            }

            // ── 4. cardinal tick marks (N bright; S/E/W dim) ─────────────
            data class Tick(val angleDeg: Int, val alpha: Float, val widthDp: Float, val lenDp: Float)
            val cardinalTicks = listOf(
                Tick(0, 1.0f, 1.4f, 14f),
                Tick(180, 0.5f, 1.4f, 14f),
                Tick(90, 0.5f, 1.4f, 14f),
                Tick(270, 0.5f, 1.4f, 14f),
            )
            cardinalTicks.forEach { t ->
                val rad = Math.toRadians((t.angleDeg - 90).toDouble())
                val outR = outerR
                val inR = outerR - t.lenDp.dp.toPx()
                drawLine(
                    color = colors.brandDeep.copy(alpha = t.alpha),
                    start = Offset(cx + (inR * cos(rad)).toFloat(), cy + (inR * sin(rad)).toFloat()),
                    end = Offset(cx + (outR * cos(rad)).toFloat(), cy + (outR * sin(rad)).toFloat()),
                    strokeWidth = t.widthDp.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // ── 5. 30°/60° middle ticks ──────────────────────────────────
            val middleAngles = listOf(30, 60, 120, 150, 210, 240, 300, 330)
            middleAngles.forEach { deg ->
                val rad = Math.toRadians((deg - 90).toDouble())
                val outR = outerR
                val inR = outerR - 8.dp.toPx()
                val tickAlpha = if (deg in 31..89 || deg in 271..359) 1f else 0.5f
                drawLine(
                    color = colors.brandDeep.copy(alpha = tickAlpha),
                    start = Offset(cx + (inR * cos(rad)).toFloat(), cy + (inR * sin(rad)).toFloat()),
                    end = Offset(cx + (outR * cos(rad)).toFloat(), cy + (outR * sin(rad)).toFloat()),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            // ── 6. minor 10° ticks ───────────────────────────────────────
            for (deg in 10..350 step 10) {
                if (deg % 30 == 0) continue
                val rad = Math.toRadians((deg - 90).toDouble())
                val outR = outerR
                val inR = outerR - 4.dp.toPx()
                drawLine(
                    color = colors.brandDeep.copy(alpha = 0.4f),
                    start = Offset(cx + (inR * cos(rad)).toFloat(), cy + (inR * sin(rad)).toFloat()),
                    end = Offset(cx + (outR * cos(rad)).toFloat(), cy + (outR * sin(rad)).toFloat()),
                    strokeWidth = 0.4.dp.toPx(),
                )
            }

            // ── 7. 4-point rose decoration (very faint) ──────────────────
            val rosePath = Path().apply {
                val tipR = outerR * 0.68f
                val midOff = outerR * 0.04f
                moveTo(cx, cy - tipR)
                lineTo(cx + midOff, cy - midOff)
                lineTo(cx, cy)
                lineTo(cx - midOff, cy - midOff)
                close()
            }
            listOf(0f, 90f, 180f, 270f).forEach { deg ->
                rotate(deg, Offset(cx, cy)) {
                    drawPath(rosePath, color = colors.brandDeep.copy(alpha = 0.10f))
                }
            }

            // ── 8. target circle at bearing ──────────────────────────────
            val bearingRad = Math.toRadians((bearing - 90).toDouble())
            val targetR = outerR * 0.72f
            val targetCentre = Offset(
                cx + (targetR * cos(bearingRad)).toFloat(),
                cy + (targetR * sin(bearingRad)).toFloat(),
            )
            drawCircle(
                color = colors.brandDeep,
                radius = 14.dp.toPx(),
                center = targetCentre,
                style = Stroke(
                    width = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 2f)),
                ),
            )
            drawCircle(
                color = targetTint.copy(alpha = 0.22f),
                radius = 5.dp.toPx(),
                center = targetCentre,
            )
            drawCircle(
                color = targetTint,
                radius = 3.dp.toPx(),
                center = targetCentre,
            )

            // ── 9. bearing arrow (gradient) + dim tail ──────────────────
            val arrowTipR = outerR * 0.62f
            val arrowBaseR = outerR * 0.04f
            val arrowWing = outerR * 0.08f
            val perpRad = bearingRad + Math.PI / 2

            val arrowTip = Offset(
                cx + (arrowTipR * cos(bearingRad)).toFloat(),
                cy + (arrowTipR * sin(bearingRad)).toFloat(),
            )
            val arrowBack = Offset(
                cx - (arrowBaseR * cos(bearingRad)).toFloat(),
                cy - (arrowBaseR * sin(bearingRad)).toFloat(),
            )
            val arrowRight = Offset(
                arrowBack.x + (arrowWing * cos(perpRad)).toFloat(),
                arrowBack.y + (arrowWing * sin(perpRad)).toFloat(),
            )
            val arrowLeft = Offset(
                arrowBack.x - (arrowWing * cos(perpRad)).toFloat(),
                arrowBack.y - (arrowWing * sin(perpRad)).toFloat(),
            )

            val arrowPath = Path().apply {
                moveTo(arrowTip.x, arrowTip.y)
                lineTo(arrowRight.x, arrowRight.y)
                lineTo(cx, cy)
                lineTo(arrowLeft.x, arrowLeft.y)
                close()
            }
            drawPath(arrowPath, brush = arrowBrush)
            drawPath(arrowPath, color = colors.brandDeep, style = Stroke(1.2.dp.toPx()))

            // dim 180° tail
            val tailR = outerR * 0.62f
            val tailRad = bearingRad + Math.PI
            val tailTip = Offset(
                cx + (tailR * cos(tailRad)).toFloat(),
                cy + (tailR * sin(tailRad)).toFloat(),
            )
            val tailWingR = outerR * 0.025f
            val tailPerp = tailRad + Math.PI / 2
            val tailRight = Offset(
                cx + (tailWingR * cos(tailPerp)).toFloat(),
                cy + (tailWingR * sin(tailPerp)).toFloat(),
            )
            val tailLeft = Offset(
                cx - (tailWingR * cos(tailPerp)).toFloat(),
                cy - (tailWingR * sin(tailPerp)).toFloat(),
            )
            val tailPath = Path().apply {
                moveTo(tailTip.x, tailTip.y)
                lineTo(tailRight.x, tailRight.y)
                lineTo(tailLeft.x, tailLeft.y)
                close()
            }
            drawPath(tailPath, color = colors.brandDeep.copy(alpha = 0.35f))

            // ── 10. centre bullseye ──────────────────────────────────────
            drawCircle(color = Color.White, radius = 11.dp.toPx(), center = Offset(cx, cy))
            drawCircle(
                color = colors.brandDeep,
                radius = 11.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(2.dp.toPx()),
            )
            drawCircle(color = colors.brand, radius = 5.dp.toPx(), center = Offset(cx, cy))
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(cx, cy))
        }

        // Cardinal labels overlaid as Compose Text (crisp fonts, easy to localise).
        val labelStyle = MaterialTheme.typography.titleMedium
        Box(Modifier.fillMaxSize()) {
            Text(
                text = "N",
                style = labelStyle.copy(fontWeight = FontWeight.W800),
                color = colors.brandDeep,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 26.dp),
            )
            Text(
                text = "S",
                style = labelStyle.copy(fontWeight = FontWeight.W700),
                color = colors.brandDeep.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp),
            )
            Text(
                text = "W",
                style = labelStyle.copy(fontWeight = FontWeight.W700),
                color = colors.brandDeep.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 28.dp),
            )
            Text(
                text = "E",
                style = labelStyle.copy(fontWeight = FontWeight.W700),
                color = colors.brandDeep.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 28.dp),
            )
        }
    }
}

