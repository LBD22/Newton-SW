package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Branded card — white surface, 18dp rounded corners, hairline border (no
 * shadow). Field Blue spec §4.3: shadows reserved for FAB / hero tiles only.
 *
 * Use this in place of Material3 `Card` / `ElevatedCard` for grouping fields
 * or content on the page-tinted background.
 */
@Composable
fun NewtonCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val colors = NewtonTheme.colors
    val shape = RoundedCornerShape(18.dp)
    val base = modifier
        .clip(shape)
        .background(colors.surface)
        .border(1.dp, colors.hairline, shape)
    val clickable = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(modifier = clickable.padding(contentPadding)) { content() }
}
