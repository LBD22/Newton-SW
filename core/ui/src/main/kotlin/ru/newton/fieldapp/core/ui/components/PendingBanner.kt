package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Pending-changes banner per spec §6.8 — pinned to the bottom of the
 * receiver-settings screen, signals that the queued changes still need a
 * manual `system save`.
 *
 * Visual:
 *  - Warm gradient `#FFF6E0 → #FCEAB8`.
 *  - Left 4dp warning stripe.
 *  - Circular warning icon-cap on the left.
 *  - Body: title (weight 800 dark-amber) + sub (lighter amber).
 *  - Trailing action pill — warning solid bg, white uppercase text.
 *
 * The component is bottom-of-screen UX critical for Newton — every queued
 * Bluetooth command waits in the queue until the user taps this Apply button.
 * Do not auto-dismiss or visually downplay.
 *
 * @param pendingCount integer count shown in the title ("3 несохранённых
 *   изменения").
 * @param onApply tap callback for the trailing action pill.
 * @param applyLabel label for the action pill — defaults to "ПРИМЕНИТЬ".
 */
@Composable
fun PendingBanner(
    pendingCount: Int,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "$pendingCount несохранённых изменения",
    subtitle: String = "Применятся после «Сохранить»",
    applyLabel: String = "Применить",
) {
    val colors = NewtonTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val warmStart = Color(0xFFFFF6E0)
    val warmEnd = Color(0xFFFCEAB8)
    val darkAmber = Color(0xFF7A4F00)
    val midAmber = Color(0xFFA47100)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    0f to warmStart,
                    1f to warmEnd,
                ),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawLeftAmberStripe(colors.warning)
                .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.warning),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PriorityHigh,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W800),
                    color = darkAmber,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = midAmber,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                modifier = Modifier
                    .clip(NewtonTheme.shapes.pill)
                    .background(colors.warning)
                    .clickable(onClick = onApply)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = applyLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W800),
                    color = Color.White,
                )
            }
        }
    }
}

private fun Modifier.drawLeftAmberStripe(color: Color): Modifier = drawBehind {
    drawRect(
        color = color,
        topLeft = Offset(0f, 0f),
        size = Size(4.dp.toPx(), size.height),
    )
}

