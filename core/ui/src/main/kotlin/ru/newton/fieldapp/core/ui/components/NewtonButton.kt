package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonPalette

/**
 * Branded action buttons. Three variants matching the bottom action bar in the
 * brand reference:
 *  - [NewtonPrimaryButton]   — filled brand blue (e.g. "Сбросить настройки")
 *  - [NewtonSuccessButton]   — filled brand green (e.g. "Сохранить настройки")
 *  - [NewtonSecondaryButton] — white with outlined edge ("Вернуться назад")
 *
 * All three use the same large 56 dp height and 18 dp corner radius so they
 * line up cleanly when placed side-by-side in a `Row`.
 */
private val ActionShape = RoundedCornerShape(18.dp)
private val ActionMinHeight = 56.dp
private val ActionPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)

@Composable
fun NewtonPrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ActionMinHeight),
        enabled = enabled,
        shape = ActionShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = ActionPadding,
    ) { ButtonRow(icon = icon, text = text) }
}

@Composable
fun NewtonSuccessButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ActionMinHeight),
        enabled = enabled,
        shape = ActionShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = NewtonPalette.SuccessGreen,
            contentColor = NewtonPalette.OnPrimary,
        ),
        contentPadding = ActionPadding,
    ) { ButtonRow(icon = icon, text = text) }
}

@Composable
fun NewtonSecondaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ActionMinHeight),
        enabled = enabled,
        shape = ActionShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = ActionPadding,
    ) { ButtonRow(icon = icon, text = text) }
}

@Composable
private fun ButtonRow(icon: ImageVector?, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        icon?.let {
            Icon(imageVector = it, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
