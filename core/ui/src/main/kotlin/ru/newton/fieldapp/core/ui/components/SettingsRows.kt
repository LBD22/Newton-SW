package ru.newton.fieldapp.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Settings rows per Field Blue spec §6.5 — radio groups, switches, list tiles.
 *
 * Visual conventions:
 *  - Container [RadioGroupCard]: white surface, hairline border, 16dp radius,
 *    rows inside are separated by 1dp hairline.
 *  - Active radio row: brand-soft → brand-faint gradient bg, brand label,
 *    weight 700.
 *  - Switch on-state: brand fill, white thumb with soft shadow.
 *  - List-tile: 40dp icon container (brand-soft + brand glyph by default;
 *    [ListTileIconVariant.Navy] flips to brand-deep + brand-soft for the
 *    antenna / hardware-specific items).
 */

/**
 * Outer card wrapping a radio-group or a switch-group. Use as the parent of
 * one or more [RadioRow]s / [SwitchRow]s.
 */
@Composable
fun RadioGroupCard(
    modifier: Modifier = Modifier,
    content: @Composable Column.() -> Unit,
) {
    val colors = NewtonTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.hairline, shape),
    ) {
        content()
    }
}

/** Workaround: `Column` reference for `content` block scope. */
typealias Column = androidx.compose.foundation.layout.ColumnScope

/**
 * Radio row inside a [RadioGroupCard]. Pass `active = true` for the selected
 * option — it picks up the soft brand gradient + brand-deep text.
 */
@Composable
fun RadioRow(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val colors = NewtonTheme.colors
    val bgBrush = if (active) {
        Brush.horizontalGradient(0f to colors.brandSoft, 1f to colors.brandFaint)
    } else {
        Brush.linearGradient(0f to Color.Transparent, 1f to Color.Transparent)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .drawHairlineBottom(if (showDivider) colors.hairline else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioDot(active = active)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (active) FontWeight.W700 else FontWeight.W600,
            ),
            color = if (active) colors.brandDeep else colors.text,
        )
    }
}

@Composable
private fun RadioDot(active: Boolean) {
    val colors = NewtonTheme.colors
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = if (active) colors.brand else colors.hairlineStrong,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(colors.brand),
            )
        }
    }
}

/**
 * Switch row — label on the left, mono meta in the middle (optional), switch
 * on the right.
 */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    showDivider: Boolean = true,
) {
    val colors = NewtonTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .drawHairlineBottom(if (showDivider) colors.hairline else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        if (meta != null) {
            Text(
                text = meta,
                style = NewtonTheme.mono.monoSmall,
                color = colors.text3,
            )
        }
        FieldSwitch(checked = checked)
    }
}

@Composable
private fun FieldSwitch(checked: Boolean) {
    val colors = NewtonTheme.colors
    val trackColor = if (checked) colors.brand else colors.surfaceDeep
    val borderColor = if (checked) colors.brand else colors.hairlineStrong
    val thumbX by animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        label = "switch-thumb",
    )
    Box(
        modifier = Modifier
            .width(42.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(trackColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbX, top = 3.dp, bottom = 3.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** Variant of the leading icon container in a list-tile. */
enum class ListTileIconVariant {
    /** Brand-soft fill + brand glyph (default). */
    Brand,
    /** Brand-deep fill + brand-soft glyph — for hardware-specific items like the antenna. */
    Navy,
}

/**
 * List-tile per §6.5: leading rounded-square icon + title + optional subtitle
 * + trailing chevron. Use for navigation rows in settings (NTRIP, Antenna,
 * etc.) where tapping leads to a deeper screen.
 *
 * Distinct from [NewtonNavCard] (which lives in a different aesthetic) — keep
 * the legacy NavCard until Phase 4 sweeps callers.
 */
@Composable
fun ListTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconVariant: ListTileIconVariant = ListTileIconVariant.Brand,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val colors = NewtonTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            val iconBg = when (iconVariant) {
                ListTileIconVariant.Brand -> colors.brandSoft
                ListTileIconVariant.Navy -> colors.brandDeep
            }
            val iconTint = when (iconVariant) {
                ListTileIconVariant.Brand -> colors.brand
                ListTileIconVariant.Navy -> colors.brandSoft
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.text,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text2,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        if (trailingContent != null) {
            trailingContent()
        } else {
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.text3,
            )
        }
    }
}

private fun Modifier.drawHairlineBottom(color: Color): Modifier = drawBehind {
    if (color == Color.Transparent) return@drawBehind
    drawLine(
        color = color,
        start = Offset(0f, size.height - 0.5f),
        end = Offset(size.width, size.height - 0.5f),
        strokeWidth = 1f,
    )
}
