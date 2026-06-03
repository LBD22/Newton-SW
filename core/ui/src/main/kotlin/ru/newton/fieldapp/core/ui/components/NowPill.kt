package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Compact "now" pill that shows the currently active mode — e.g.
 * "АКТИВНО · ROVER / NTRIP" at the top of the receiver settings screen.
 * Spec §6.7.
 *
 * Visual: brand-soft fill, brand-deep text, brand 1dp border, full-radius
 * pill, dot with halo glow on the left.
 */
@Composable
fun NowPill(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color = NewtonTheme.colors.brand,
) {
    val colors = NewtonTheme.colors
    Row(
        modifier = modifier
            .clip(NewtonTheme.shapes.pill)
            .background(colors.brandSoft)
            .border(1.dp, colors.brand.copy(alpha = 0.30f), NewtonTheme.shapes.pill)
            .padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Static dot with soft halo (no animation — this is a static state pill,
        // not a live-status indicator).
        Row(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = 0.22f)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            ) {}
        }

        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(0.10f, androidx.compose.ui.unit.TextUnitType.Em)),
            color = colors.brandDeep,
        )
    }
}
