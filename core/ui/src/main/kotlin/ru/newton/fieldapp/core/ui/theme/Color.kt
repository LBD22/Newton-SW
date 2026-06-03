package ru.newton.fieldapp.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Newton Field App — "Field Blue" brand palette.
 *
 * Canonical spec: `docs/design/TZ/newton-field-design-system.md` §2.
 * Visual reference: `docs/design/TZ/newton-field-blue.html`.
 *
 * Monochrome blue identity — cyan/teal are competitors' colours and forbidden
 * as standalone accents. Cyan may appear ONLY as a gradient stop alongside
 * brand-blue inside SVG/Canvas signature elements (compass arrow, progress ring)
 * where luminance is needed for legibility.
 */
internal object NewtonPalette {
    // Brand-blues ---------------------------------------------------------
    /** Primary — buttons, active pills, primary icons. */
    val Brand = Color(0xFF1A5DD8)

    /** Navy — large display text, headlines, hero backgrounds, dark icon containers. */
    val BrandDeep = Color(0xFF0E2A57)

    /** Mid blue — rare decorative accents only. */
    val BrandMid = Color(0xFF4F7FCE)

    /** Light blue — icon container fills, active thin pills, soft containers. */
    val BrandSoft = Color(0xFFDCE7FB)

    /** Very light blue — distinguishing background for accent tiles. */
    val BrandFaint = Color(0xFFEEF3FC)

    // Surfaces ------------------------------------------------------------
    /** Page background (behind tiles). */
    val Bg = Color(0xFFF5F8FC)

    /** Cards, tiles, top-bar. */
    val Surface = Color(0xFFFFFFFF)

    /** Auxiliary surface (very subtle separation). */
    val SurfaceTint = Color(0xFFF7FAFD)

    /** Hover states, switch off-state. */
    val SurfaceDeep = Color(0xFFEFF3FA)

    // Text ---------------------------------------------------------------
    /** Primary text (= BrandDeep). */
    val Text = Color(0xFF0E2A57)

    // Semantic -----------------------------------------------------------
    val Success = Color(0xFF16A35B)
    val SuccessSoft = Color(0xFFD8F1E2)
    val Warning = Color(0xFFD08400)
    val WarningSoft = Color(0xFFFCEDD0)
    val Error = Color(0xFFC0273D)

    // Fix-quality colours (drive FixStatusColors below) -------------------
    /** RTK Fixed. */
    val FixGreen = Color(0xFF16A35B)

    /** RTK Float. */
    val FloatYellow = Color(0xFFD08400)

    /** Single / DGNSS. */
    val SingleOrange = Color(0xFFE08000)

    /** No fix / link down. */
    val NoFixRed = Color(0xFFC0273D)

    // Dark-mode placeholders ---------------------------------------------
    // Dark mode is not formally defined in the Field Blue spec yet — these are
    // a sensible starting point until Phase 5 ("Field Mode" toggle) reworks
    // them. Avoid relying on dark-mode visuals for the v1 launch.
    val BgDark = Color(0xFF0E1116)
    val SurfaceDark = Color(0xFF14181F)
    val SurfaceVariantDark = Color(0xFF1F2530)
    val OnSurfaceDark = Color(0xFFEAEDF2)
    val OnSurfaceMutedDark = Color(0xFF9AA3B2)
    val OutlineDark = Color(0xFF2A313D)
}

/**
 * Semantic colours for receiver / fix status. Read these in any composable
 * that shows fix quality — never hard-code a colour for a fix kind.
 */
@androidx.compose.runtime.Immutable
data class FixStatusColors(
    val fixed: Color,
    val float: Color,
    val single: Color,
    val noFix: Color,
)
