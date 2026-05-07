package ru.newton.fieldapp.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Newton uses noticeably-rounded shapes throughout — the brand reference shows
 * cards with 24-32 dp corners and pill chips with full radius. Material 3's
 * defaults are too tight (4-12 dp); we override.
 */
internal val NewtonShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
