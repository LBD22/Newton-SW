package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Fix-quality bucket — controls the marker stripe colour, badge tint, and
 * fix-pill text.
 */
enum class FixQuality {
    RtkFix,
    RtkFloat,
    Single,
    NoFix,
}

/**
 * GNSS-status hero card placed at the top of every primary surveying screen.
 * Implements spec §6.2.
 *
 * Visual:
 *  - 18dp radius white card with `hairlineStrong` border.
 *  - Left 4dp marker stripe coloured by [FixQuality].
 *  - Watermark [TopoRoseWatermark] in the top-right (off-canvas via negative inset).
 *  - Top row: [PingingDot] + fix label (uppercase 11dp 800), then mono SAT/HDOP/age on the right.
 *  - Project line in Title-S brand-deep.
 *  - Optional mini-coords line — mono small text-2.
 */
@Composable
fun StatusCard(
    fix: FixQuality,
    fixLabel: String,
    metaText: String,
    project: String,
    modifier: Modifier = Modifier,
    miniCoords: String? = null,
) {
    val colors = NewtonTheme.colors
    val stripe = when (fix) {
        FixQuality.RtkFix -> colors.success
        FixQuality.RtkFloat -> colors.warning
        FixQuality.Single -> colors.warning
        FixQuality.NoFix -> colors.error
    }
    val pillColor = when (fix) {
        FixQuality.RtkFix -> colors.success
        FixQuality.RtkFloat -> colors.warning
        FixQuality.Single -> colors.warning
        FixQuality.NoFix -> colors.error
    }
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.hairlineStrong, shape),
    ) {
        // Topo-rose watermark — sits partially off-card, very low alpha.
        TopoRoseWatermark(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-10).dp)
                .size(90.dp),
            color = colors.brandDeep,
            alpha = 0.07f,
        )

        // Left marker stripe (drawn via background+padding so it tracks card height).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawLeftStripe(stripe)
                .padding(top = 14.dp, bottom = 14.dp, start = 18.dp, end = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PingingDot(color = pillColor)
                Text(
                    text = fixLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.W800,
                    ),
                    color = pillColor,
                )
                Text(
                    text = metaText,
                    style = NewtonTheme.mono.monoSmall,
                    color = colors.text2,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
            Text(
                text = project,
                style = MaterialTheme.typography.titleMedium,
                color = colors.brandDeep,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (miniCoords != null) {
                Text(
                    text = miniCoords,
                    style = NewtonTheme.mono.monoSmall,
                    color = colors.text2,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Left-edge 4dp stripe with rounded inner corner — works regardless of card height. */
private fun Modifier.drawLeftStripe(color: Color): Modifier = drawBehind {
    val w = 4.dp.toPx()
    val cornerR = 4.dp.toPx()
    val inset = 14.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(0f, inset),
        size = androidx.compose.ui.geometry.Size(w, size.height - inset * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR, cornerR),
    )
}
