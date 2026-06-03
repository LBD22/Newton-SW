package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Field Blue tile variants for the home / index dashboards. Spec §6.1.
 *
 * Hierarchy:
 *  - [TileHero] — single 2×2 hero, navy gradient + topo rings + crosshair. Used
 *    for the primary action ("Снять точку"). One per screen.
 *  - [TileAccent] — outlined brand-faint + brand border. Frequent secondary
 *    actions (Линия, Вынос).
 *  - [TileData] — filled icon container, white card. Data views (Карта, Точки,
 *    CAD).
 *  - [TileSystem] — outlined icon container (transparent fill + brand border),
 *    white card. System / hardware items (Приёмник, NTRIP, Проекты).
 *
 * All 1×1 tiles target ~96dp min height. Hero is 2×2 in a 3-column grid.
 */

private val TileShape = RoundedCornerShape(18.dp)
private val TileIconShape = RoundedCornerShape(12.dp)
private val TileMinHeight = 96.dp

@Composable
fun TileHero(
    title: String,
    sub: String?,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewtonTheme.colors
    val gradient = Brush.linearGradient(
        0f to colors.brand,
        0.6f to Color(0xFF0E3F95),
        1f to colors.brandDeep,
    )
    Box(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = TileShape, ambientColor = colors.brand, spotColor = colors.brand)
            .clip(TileShape)
            .background(gradient)
            .clickable(onClick = onClick)
            .drawTopoRings()
            .padding(18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.brandSoft)
                    .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.brandDeep,
                    modifier = Modifier.size(30.dp),
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.W800,
                        lineHeight = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp),
                    ),
                    color = Color.White,
                )
                if (sub != null) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 1.dp),
                        ) {}
                        Text(
                            text = sub.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W800),
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TileAccent(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewtonTheme.colors
    TileBase(
        modifier = modifier,
        onClick = onClick,
        background = colors.brandFaint,
        border = colors.brand to 1.5.dp,
    ) {
        TileIconBlock(
            icon = icon,
            iconBg = colors.brand,
            iconTint = colors.brandSoft,
        )
        TileLabel(text = title)
    }
}

@Composable
fun TileData(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val colors = NewtonTheme.colors
    Box(modifier = modifier) {
        TileBase(
            modifier = Modifier,
            onClick = onClick,
            background = colors.surface,
            border = colors.hairline to 1.dp,
        ) {
            TileIconBlock(
                icon = icon,
                iconBg = colors.brandSoft,
                iconTint = colors.brand,
            )
            TileLabel(text = title)
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 11.dp, end = 11.dp)
                    .clip(NewtonTheme.shapes.pill)
                    .background(colors.brandDeep)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = badge,
                    style = NewtonTheme.mono.monoSmall.copy(fontWeight = FontWeight.W800),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
fun TileSystem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NewtonTheme.colors
    TileBase(
        modifier = modifier,
        onClick = onClick,
        background = colors.surface,
        border = colors.hairline to 1.dp,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(TileIconShape)
                .border(1.5.dp, colors.brand, TileIconShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.brand)
        }
        TileLabel(text = title)
    }
}

// ── internals ──────────────────────────────────────────────────────────────

@Composable
private fun TileBase(
    modifier: Modifier,
    onClick: () -> Unit,
    background: Color,
    border: Pair<Color, androidx.compose.ui.unit.Dp>,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(TileShape)
            .background(background)
            .border(border.second, border.first, TileShape)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        content()
    }
}

@Composable
private fun TileIconBlock(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
) {
    Box(
        modifier = androidx.compose.ui.Modifier
            .size(38.dp)
            .clip(TileIconShape)
            .background(iconBg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
    }
}

@Composable
private fun TileLabel(text: String) {
    val colors = NewtonTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.W700),
        color = colors.brandDeep,
        modifier = Modifier.padding(top = 10.dp),
    )
}

/** Concentric rings + crosshair overlay used inside the Hero tile. */
private fun Modifier.drawTopoRings(): Modifier = drawBehind {
    val cx = size.width * 0.78f
    val cy = size.height * 0.28f
    val ringColor = Color.White
    listOf(0.06f to 100f, 0.08f to 80f, 0.06f to 60f, 0.05f to 40f).forEach { (alpha, r) ->
        drawCircle(
            color = ringColor.copy(alpha = alpha),
            radius = r,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(1.2f),
        )
    }
    // Crosshair marker
    val crossR = 8f
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(cx - crossR, cy),
        end = Offset(cx + crossR, cy),
        strokeWidth = 1.5f,
    )
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(cx, cy - crossR),
        end = Offset(cx, cy + crossR),
        strokeWidth = 1.5f,
    )
}
