package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Triple metric pill row shown under the status-card — three equal-width
 * capsules with a small uppercase label on top and a big mono value below.
 * Spec §6.3.
 *
 * Mark one pill (usually the most-decisive metric, e.g. `HDOP`) as `accent`
 * to give it a soft brand gradient and tint the value in `brand`.
 */
@Composable
fun MetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val colors = NewtonTheme.colors
    val shape = RoundedCornerShape(14.dp)
    val bgModifier = if (accent) {
        Modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to colors.brandSoft,
                    1f to colors.brandFaint,
                ),
            )
            .border(1.dp, colors.brand.copy(alpha = 0.20f), shape)
    } else {
        Modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.hairline, shape)
    }
    val valueColor = if (accent) colors.brand else colors.brandDeep

    Column(
        modifier = modifier
            .then(bgModifier)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.W800),
            color = colors.text3,
        )
        Text(
            text = value,
            style = NewtonTheme.mono.monoLarge.copy(fontWeight = FontWeight.W700),
            color = valueColor,
        )
    }
}

/**
 * Container for three pills in a row, equal-width. Wraps [MetricPill]s in a
 * `Row` with 8dp gap and `weight(1f)` on each child.
 */
@Composable
fun MetricPillRow(
    modifier: Modifier = Modifier,
    content: @Composable MetricPillRowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        with(MetricPillRowScopeImpl(this)) { content() }
    }
}

interface MetricPillRowScope {
    @Composable
    fun MetricPill(label: String, value: String, accent: Boolean = false)
}

private class MetricPillRowScopeImpl(private val rowScope: androidx.compose.foundation.layout.RowScope) :
    MetricPillRowScope {
    @Composable
    override fun MetricPill(label: String, value: String, accent: Boolean) {
        with(rowScope) {
            MetricPill(
                label = label,
                value = value,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
