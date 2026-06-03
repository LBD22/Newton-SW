package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Live-coordinates card shown during point-survey averaging. Spec §6.9.
 *
 * Visual: white surface with `hairlineStrong` border, 16dp radius, left
 * 4dp `success` marker stripe (matching the [StatusCard] convention so the
 * eye groups them as the "live data" pair).
 *
 * The bottom row carries a sigma readout split by a dashed-divider — left:
 * mono mu/HRMS info; right: small uppercase fix-mini pill (success when RTK Fix).
 *
 * @param latitude DMS-formatted latitude, e.g. `"55°45′12.347″ N"`.
 * @param longitude DMS-formatted longitude.
 * @param height "187.231 m" formatted height.
 * @param sigma free-form left half of the bottom row, e.g.
 *   `"σ 11 / 16 mm  ·  HRMS / VRMS"`.
 * @param fixLabel uppercase short label like `"RTK Fix"`, `"Float"`. Tinted by
 *   [fixColor].
 */
@Composable
fun LiveCard(
    latitude: String,
    longitude: String,
    height: String,
    sigma: String,
    fixLabel: String,
    modifier: Modifier = Modifier,
    fixColor: Color = NewtonTheme.colors.success,
) {
    val colors = NewtonTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.hairlineStrong, shape),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawLeftFixStripe(fixColor)
                .padding(top = 14.dp, bottom = 12.dp, start = 18.dp, end = 18.dp),
        ) {
            CoordLine("N", latitude, colors)
            CoordLine("E", longitude, colors)
            CoordLine("H", height, colors)

            // Dashed divider before the sigma row.
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(top = 4.dp, bottom = 4.dp),
            ) {
                drawLine(
                    color = colors.hairlineStrong,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = sigma,
                    style = NewtonTheme.mono.monoSmall,
                    color = colors.text2,
                )
                Text(
                    text = fixLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W800),
                    color = fixColor,
                )
            }
        }
    }
}

@Composable
private fun CoordLine(label: String, value: String, colors: ru.newton.fieldapp.core.ui.theme.NewtonColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W500),
            color = colors.text2,
        )
        Text(
            text = value,
            style = NewtonTheme.mono.monoMedium.copy(fontWeight = FontWeight.W600),
            color = colors.brandDeep,
        )
    }
}

private fun Modifier.drawLeftFixStripe(color: Color): Modifier = drawBehind {
    val w = 4.dp.toPx()
    val inset = 14.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(0f, inset),
        size = Size(w, size.height - inset * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w, w),
    )
}
