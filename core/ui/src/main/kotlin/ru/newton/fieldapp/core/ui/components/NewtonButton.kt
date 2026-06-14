package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonPalette
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Branded action buttons. Per Field Blue spec §6.4 — monochrome blue palette
 * for all primary actions; `success` green is reserved for STATUS indicators
 * only (RTK Fix dot, success marker-stripes), never on action buttons.
 *
 *  - [NewtonPrimaryButton]  — brand-blue gradient, inset highlight, brand glow shadow.
 *    Use for the dominant action on a screen ("Сохранить", "Применить", "Начать").
 *  - [NewtonSuccessButton]  — **API kept for legacy callsites; visually identical
 *    to [NewtonPrimaryButton]**. Semantically the same as primary now.
 *  - [NewtonSecondaryButton] — white surface with hairline-strong outline.
 *
 * All three keep a consistent 52dp min-height and 16dp corner radius so they
 * line up cleanly side-by-side in a `Row`.
 */
private val ActionShape = RoundedCornerShape(16.dp)
private val ActionMinHeight = 52.dp

@Composable
fun NewtonPrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = NewtonTheme.colors
    GradientButton(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        gradient = Brush.verticalGradient(0f to colors.brand, 1f to Color(0xFF134EBC)),
        glowColor = colors.brand,
        contentColor = Color.White,
        text = text,
        icon = icon,
    )
}

@Composable
fun NewtonSuccessButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    // Field Blue forbids green on action buttons (spec §2.5: success used only
    // for status indicators). Delegate to the brand-blue gradient so callsites
    // that historically meant "Save / commit" still render correctly.
    NewtonPrimaryButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
    )
}

@Composable
fun NewtonSecondaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = NewtonTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = ActionMinHeight)
            .clip(ActionShape)
            .background(colors.surface)
            .border(1.5.dp, colors.hairlineStrong, ActionShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.brandDeep)
        }
        ButtonLabel(text = text, color = colors.brandDeep)
    }
}

@Composable
private fun GradientButton(
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    gradient: Brush,
    glowColor: Color,
    contentColor: Color,
    text: String,
    icon: ImageVector?,
) {
    Row(
        modifier = modifier
            .heightIn(min = ActionMinHeight)
            .shadow(elevation = if (enabled) 4.dp else 0.dp, shape = ActionShape, spotColor = glowColor, ambientColor = glowColor)
            .clip(ActionShape)
            .background(gradient)
            .border(1.dp, Color.White.copy(alpha = 0.20f), ActionShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        }
        ButtonLabel(text = text, color = contentColor)
    }
}

@Composable
private fun ButtonLabel(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.W800,
            letterSpacing = TextUnit(0.06f, TextUnitType.Em),
        ),
        color = color,
        // Never break a short label like "ЖУРНАЛ" char-by-char onto several lines.
        maxLines = 1,
        softWrap = false,
    )
}
