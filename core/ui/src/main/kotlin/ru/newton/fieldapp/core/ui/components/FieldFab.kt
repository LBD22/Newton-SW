package ru.newton.fieldapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

/**
 * Field Blue buttons per spec §6.4.
 *
 * Note: the legacy `NewtonPrimaryButton` / `NewtonSecondaryButton` /
 * `NewtonSuccessButton` in [NewtonButton.kt] still exist and are kept until
 * Phase 4 sweeps callers — they read the new palette via M3 colorScheme so
 * they're visually consistent meanwhile.
 *
 * Use these (`FieldPrimaryButton`, `FieldOutlineButton`, `ExtendedFieldFab`)
 * for any new UI that follows the spec literally — they hit the gradient +
 * inset-highlight treatment the legacy filled `Button` can't reproduce.
 */

@Composable
fun FieldPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = NewtonTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .shadow(elevation = if (enabled) 4.dp else 0.dp, shape = shape, spotColor = colors.brand, ambientColor = colors.brand)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to colors.brand,
                    1f to Color(0xFF134EBC),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.20f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .height(52.dp)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.W800,
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.06f, androidx.compose.ui.unit.TextUnitType.Em),
            ),
            color = Color.White,
        )
    }
}

@Composable
fun FieldOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = NewtonTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.5.dp, colors.hairlineStrong, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .height(52.dp)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.brandDeep)
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.W700,
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.06f, androidx.compose.ui.unit.TextUnitType.Em),
            ),
            color = colors.brandDeep,
        )
    }
}

/**
 * Full-width extended FAB — the primary screen-level action. Spec §6.4:
 *  - 60dp height, 20dp radius
 *  - Same brand gradient as [FieldPrimaryButton]
 *  - Icon 24dp, text 14sp weight 800
 */
@Composable
fun ExtendedFieldFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = NewtonTheme.colors
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = if (enabled) 6.dp else 0.dp, shape = shape, spotColor = colors.brand, ambientColor = colors.brand)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    0f to colors.brand,
                    1f to Color(0xFF134EBC),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.20f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .height(60.dp)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.padding(end = 2.dp),
        )
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.W800,
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.06f, androidx.compose.ui.unit.TextUnitType.Em),
            ),
            color = Color.White,
        )
    }
}
