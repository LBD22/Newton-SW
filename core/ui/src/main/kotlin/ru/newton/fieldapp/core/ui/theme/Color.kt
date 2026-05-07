package ru.newton.fieldapp.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Newton Field App brand palette — derived from the corporate dashboard mock-ups
 * (see `screenshots/`). Goal: a clean Inter-style "field instrument" look —
 * white cards on a near-white blue-tinted page, deep brand blue for primary
 * controls, vivid green for save / success, status pills with full-radius
 * coloured backgrounds.
 *
 * The launcher icon's `#1F3864` is preserved as the deepest tint so the app
 * icon and the in-app primary still register as the same brand.
 */
internal object NewtonPalette {
    // Primary brand blue — used on primary CTAs ("Сбросить настройки",
    // "Добавить точку А") and the active tab pill.
    val PrimaryBlue = Color(0xFF2C5BB5)
    val PrimaryBlueDeep = Color(0xFF1F3864) // launcher background, app-icon anchor
    val PrimaryBlueLight = Color(0xFF6189D6) // hover/pressed-light variant
    val PrimaryBlueTint = Color(0xFFE8EEF7) // selected-state pill background

    val OnPrimary = Color(0xFFFFFFFF)

    // Success green — used for "Сохранить настройки" and the start/play CTA.
    val SuccessGreen = Color(0xFF4CB85C)
    val SuccessGreenDark = Color(0xFF3D9A4A)
    val SuccessGreenTint = Color(0xFFD9F0DD) // "Активна" / "Подключен" pill bg

    // Status accents (also drive the FixStatusColors below).
    val FixGreen = Color(0xFF4CB85C) // RTK Fixed
    val FloatYellow = Color(0xFFF6B042) // RTK Float
    val SingleOrange = Color(0xFFEF8B3F) // single / DGNSS
    val NoFixRed = Color(0xFFE05A5A) // no fix / link down
    val WarnAmber = Color(0xFFE0A445) // generic warn

    // Surfaces. The page is a faint blue-grey; cards are pure white.
    val PageBackgroundLight = Color(0xFFEEF2F6)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceVariantLight = Color(0xFFF5F7FA)
    val SurfaceTintLight = Color(0xFFEDEFF3) // chip / pill base

    val SurfaceDark = Color(0xFF14181F)
    val SurfaceVariantDark = Color(0xFF1F2530)
    val PageBackgroundDark = Color(0xFF0E1116)

    // Text.
    val OnSurfaceLight = Color(0xFF1A1F2A)
    val OnSurfaceMutedLight = Color(0xFF6B7585)
    val OnSurfaceDark = Color(0xFFEAEDF2)
    val OnSurfaceMutedDark = Color(0xFF9AA3B2)

    // Outlines / dividers.
    val OutlineLight = Color(0xFFE1E6EE)
    val OutlineDark = Color(0xFF2A313D)
}

/**
 * Semantic colours for receiver / fix status. Read these in any composable that
 * shows fix quality — never hard-code a colour for a fix kind.
 */
data class FixStatusColors(
    val fixed: Color,
    val float: Color,
    val single: Color,
    val noFix: Color,
)
