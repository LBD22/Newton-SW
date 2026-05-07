package ru.newton.fieldapp.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Small uppercase brand-blue category label — "ТРАКТОР", "ОРУДИЕ",
 * "СЕЛЬХОЗМАШИНА" in the corporate dashboard. Sits above a card's title or
 * inside a card to mark a section.
 */
@Composable
fun NewtonSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}
