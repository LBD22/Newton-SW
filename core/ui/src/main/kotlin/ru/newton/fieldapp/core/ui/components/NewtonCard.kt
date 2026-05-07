package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Branded card — white surface, large rounded corners, very subtle elevation.
 *
 * Matches the dashboard mock-ups: cards float on the page-tinted background
 * and group related fields. Use this in place of Material3 `Card` /
 * `ElevatedCard` everywhere except where the surface needs to feel inert
 * (status bars, full-width strips).
 */
@Composable
fun NewtonCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val base = modifier
        .shadow(elevation = 1.dp, shape = shape, ambientColor = Color(0x10000000), spotColor = Color(0x10000000))
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
    val clickable = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(modifier = clickable.padding(contentPadding)) { content() }
}
