package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Section label with the trailing dashed-tail line per Field Blue spec §6.5.
 *
 * Visual: small uppercase brand-text-2 label on the left, then a thin dashed
 * line filling the remaining row width to the right. The dashed line acts as a
 * cartographic "section divider" — adds identity to settings/forms.
 *
 * Use [NewtonSectionLabel] (no tail) for labels inside a card or where you
 * want plain text. Use this when introducing a group in a settings list.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = NewtonTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
        )
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
        ) {
            val dashOn = 6f
            val dashOff = 4f
            drawLine(
                color = colors.hairlineStrong,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = size.height,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff)),
            )
        }
    }
}
