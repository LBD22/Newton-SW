package ru.newton.fieldapp.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Newton Field App tokens that don't have a clean Material 3 `ColorScheme`
 * counterpart. Read these via `NewtonTheme.colors.brandDeep`, etc.
 *
 * For tokens that DO have a clean M3 mapping (primary, surface, background,
 * outline, error, etc.) keep using `MaterialTheme.colorScheme.*` — the M3
 * scheme in [NewtonTheme] is wired to Field Blue values, so existing screens
 * automatically pick up the new palette without code changes.
 *
 * Mapping summary (Field Blue → M3 colorScheme):
 * - `brand`              → `primary`
 * - `brandDeep`          → `onSurface`, `onPrimaryContainer`
 * - `brandSoft`          → `primaryContainer`
 * - `bg`                 → `background`
 * - `surface`            → `surface`
 * - `text2`              → `onSurfaceVariant`
 * - `hairline`           → `outlineVariant`
 * - `hairlineStrong`     → `outline`
 * - `error`              → `error`
 *
 * Tokens listed here (`text3`, `brandFaint`, `brandMid`, `brandGlow`,
 * `hairlineSoft`, `success`, `successSoft`, `warning`, `warningSoft`,
 * `surfaceDeep`, `surfaceTint`) are NOT covered by M3 — access them through
 * the local instance.
 */
@Immutable
data class NewtonColors(
    val brand: Color,
    val brandDeep: Color,
    val brandMid: Color,
    val brandSoft: Color,
    val brandFaint: Color,
    val brandGlow: Color,
    val bg: Color,
    val surface: Color,
    val surfaceTint: Color,
    val surfaceDeep: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val hairlineSoft: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val error: Color,
    /** Whether this scheme is the high-visibility outdoor "Field Mode" variant. */
    val isFieldMode: Boolean = false,
)

internal val LightNewtonColors = NewtonColors(
    brand = NewtonPalette.Brand,
    brandDeep = NewtonPalette.BrandDeep,
    brandMid = NewtonPalette.BrandMid,
    brandSoft = NewtonPalette.BrandSoft,
    brandFaint = NewtonPalette.BrandFaint,
    brandGlow = Color(0x381A5DD8), // rgba(26,93,216,0.22)
    bg = NewtonPalette.Bg,
    surface = NewtonPalette.Surface,
    surfaceTint = NewtonPalette.SurfaceTint,
    surfaceDeep = NewtonPalette.SurfaceDeep,
    text = NewtonPalette.Text,
    text2 = Color(0xA80E2A57), // alpha 0.66
    text3 = Color(0x6B0E2A57), // alpha 0.42
    hairlineSoft = Color(0x0F0E2A57), // alpha 0.06
    hairline = Color(0x1A0E2A57),     // alpha 0.10
    hairlineStrong = Color(0x380E2A57), // alpha 0.22
    success = NewtonPalette.Success,
    successSoft = NewtonPalette.SuccessSoft,
    warning = NewtonPalette.Warning,
    warningSoft = NewtonPalette.WarningSoft,
    error = NewtonPalette.Error,
    isFieldMode = false,
)

/**
 * High-visibility outdoor profile per the post-spec follow-up. Same hue family,
 * tuned for direct sunlight:
 *  - warmer surface tint
 *  - stronger hairlines (so card boundaries survive screen glare)
 *  - lifted secondary/tertiary text alphas (so meta-info stays legible)
 */
internal val FieldModeNewtonColors = LightNewtonColors.copy(
    surface = Color(0xFFF5F7FB),
    surfaceTint = Color(0xFFEEF2F8),
    bg = Color(0xFFE8EEF6),
    surfaceDeep = Color(0xFFDFE6F1),
    text2 = Color(0xC70E2A57),         // alpha 0.78
    text3 = Color(0x8C0E2A57),         // alpha 0.55
    hairlineSoft = Color(0x1F0E2A57),  // alpha 0.12
    hairline = Color(0x2E0E2A57),      // alpha 0.18
    hairlineStrong = Color(0x4D0E2A57),// alpha 0.30
    isFieldMode = true,
)

/**
 * Errors loudly if accessed outside a [NewtonTheme]. Forces consumers to wrap
 * their previews / fragments / hosts in the theme rather than silently reading
 * a default and rendering wrong.
 */
val LocalNewtonColors = staticCompositionLocalOf<NewtonColors> {
    error("NewtonColors not provided — wrap your composable in NewtonTheme.")
}