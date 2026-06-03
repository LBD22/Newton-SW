package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
 * Empty-state placeholder for list screens with no data — a centered card
 * with a soft icon, title and supporting message. Optional CTA button.
 *
 * Usage:
 * ```
 * EmptyState(
 *     icon = Icons.Default.FolderOpen,
 *     title = "Проектов ещё нет",
 *     message = "Создайте первый проект, чтобы начать съёмку.",
 *     actionLabel = "Создать проект",
 *     onAction = { navController.navigate(CREATE_ROUTE) },
 * )
 * ```
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = NewtonTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.brand,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.W700),
            color = colors.brandDeep,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text2,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            NewtonPrimaryButton(
                onClick = onAction,
                text = actionLabel,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * Error state shown inside a screen body when a load failed. Uses warm error
 * tint without being a full-screen takeover so a partial screen can still
 * surface usable data above it.
 *
 * @param message free-form error description from the data layer.
 * @param onRetry optional callback rendered as a secondary outlined button.
 */
@Composable
fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Не удалось загрузить",
    onRetry: (() -> Unit)? = null,
) {
    val colors = NewtonTheme.colors
    NewtonCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W700),
                color = colors.error,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text2,
            )
            if (onRetry != null) {
                NewtonSecondaryButton(
                    onClick = onRetry,
                    text = "Повторить",
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
