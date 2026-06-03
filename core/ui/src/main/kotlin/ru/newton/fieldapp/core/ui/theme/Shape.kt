package ru.newton.fieldapp.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Field Blue radii per spec §4.2.
 *
 * Mapping to Material 3 [Shapes] (so existing M3 components inherit sensible
 * defaults):
 * - `extraSmall` = 12dp (chips, small containers)
 * - `small`      = 14dp (text-fields, form-rows)
 * - `medium`     = 16dp (default cards, list-tiles)
 * - `large`      = 18dp (status-card, distance-card, tiles)
 * - `extraLarge` = 24dp (modal sheets)
 *
 * Tokens beyond M3 are exposed via [NewtonShapes] (pill, icon-cap variants).
 */
internal val NewtonM3Shapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Field Blue shape tokens not covered by [Shapes]:
 *  - `pill` for status pills, FAB, capsule buttons
 *  - `iconCap` / `iconCapLg` for icon containers inside tiles
 */
@Immutable
data class NewtonShapes(
    val pill: Shape = RoundedCornerShape(999.dp),
    val iconCap: Shape = RoundedCornerShape(12.dp),
    val iconCapLg: Shape = RoundedCornerShape(16.dp),
)

val LocalNewtonShapes = staticCompositionLocalOf { NewtonShapes() }
