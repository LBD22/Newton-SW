package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Square-aspect grid tile per Field Blue Tile-Data spec (§6.1):
 * white surface · hairline border · 18dp radius · brand-soft icon container
 * with brand glyph · brand-deep label. No shadow.
 *
 * Used on the Settings / Survey dashboards where 8–15 entry points need to be
 * visible. Layout is icon-centred-above-label (rather than the spec's
 * top-left-stacked) because the existing screens treat this as a launcher
 * grid — centred reads cleaner at this density.
 *
 * For tighter Field Blue compliance on the home screen with hero +
 * accent/data/system distinction, use [TileHero], [TileAccent], [TileData],
 * [TileSystem] from `Tiles.kt`.
 */
@Composable
fun NewtonTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = NewtonTheme.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.hairline, shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.brandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.brand,
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.W700),
                color = colors.brandDeep,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.text2,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
