package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Thin status capsule shown under the distance-card on Stakeout — a one-line
 * "RTK Fix · 17 SAT · HDOP 0.9" readout. Spec §6.10.
 *
 * Uses [PingingDot] for the live indicator on the left and renders the rest of
 * the segments separated by mono mid-dots. The key word (`RTK Fix`) is
 * uppercase + brand-deep weight 800 success-coloured to make the live status
 * the dominant element.
 *
 * @param keyword the leading "live" word — usually `"RTK Fix"`, `"Float"` etc.
 *   Rendered uppercase via [String.uppercase].
 * @param segments additional mono segments, joined visually with mid-dots.
 *   E.g. `listOf("17 SAT", "HDOP 0.9")`.
 * @param liveColor colour of the pinging-dot and the keyword text. Defaults to
 *   success — pass `NewtonTheme.colors.warning` for Float, etc.
 */
@Composable
fun InlineStatus(
    keyword: String,
    segments: List<String>,
    modifier: Modifier = Modifier,
    liveColor: Color = NewtonTheme.colors.success,
) {
    val colors = NewtonTheme.colors
    Row(
        modifier = modifier
            .clip(NewtonTheme.shapes.pill)
            .background(colors.surface)
            .border(1.dp, colors.hairline, NewtonTheme.shapes.pill)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PingingDot(color = liveColor)

        Text(
            text = keyword.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W800),
            color = liveColor,
        )

        segments.forEachIndexed { index, segment ->
            Text(text = "·", style = MaterialTheme.typography.bodySmall, color = colors.text3)
            Text(
                text = segment,
                style = NewtonTheme.mono.monoSmall,
                color = colors.text2,
            )
        }
    }
}
