package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonPalette

/**
 * Pill-shaped status chip, matching the top-strip indicators in the brand
 * reference (SAT 48, ФИКС, 14:34, etc.) and the inline status badges next to
 * configuration cards (Подключен / Активна / Загружено).
 *
 * Pass [filled = true] for the high-emphasis variant (the central "swath
 * width" / 40 см chip in the toolbar) — that flips to brand blue.
 */
@Composable
fun NewtonStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    background: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    filled: Boolean = false,
) {
    val resolvedBg = if (filled) MaterialTheme.colorScheme.primary else background
    val resolvedFg = if (filled) MaterialTheme.colorScheme.onPrimary else contentColor
    Row(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(50))
            .background(resolvedBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = if (filled) resolvedFg else MaterialTheme.colorScheme.primary,
            )
        }
        Text(text = text, style = MaterialTheme.typography.titleSmall, color = resolvedFg)
    }
}

/**
 * Pre-configured success badge ("Подключен" / "Активна").
 */
@Composable
fun NewtonSuccessBadge(text: String, modifier: Modifier = Modifier) {
    NewtonStatusPill(
        text = text,
        modifier = modifier,
        background = NewtonPalette.SuccessSoft,
        contentColor = NewtonPalette.Success,
    )
}

/**
 * Pre-configured neutral badge ("Загружено" / generic info).
 */
@Composable
fun NewtonInfoBadge(text: String, modifier: Modifier = Modifier) {
    NewtonStatusPill(
        text = text,
        modifier = modifier,
        background = NewtonPalette.BrandSoft,
        contentColor = NewtonPalette.BrandDeep,
    )
}
