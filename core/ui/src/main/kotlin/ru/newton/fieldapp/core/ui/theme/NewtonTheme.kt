package ru.newton.fieldapp.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Newton Field App theme — implements the "Field Blue" design system.
 *
 * Spec: `docs/design/TZ/newton-field-design-system.md`.
 * Visual reference: `docs/design/TZ/newton-field-blue.html`.
 *
 * Wiring strategy:
 *  - M3 [androidx.compose.material3.ColorScheme] is filled with the Field Blue
 *    palette so the ~265 existing call-sites of `MaterialTheme.colorScheme.*`
 *    keep working and automatically pick up the new look.
 *  - Tokens that don't have a clean M3 slot (`text3`, `brandDeep`, `brandSoft`,
 *    `brandFaint`, `brandGlow`, hairline alphas, semantic-soft, etc.) are
 *    provided via [LocalNewtonColors] and read through `NewtonTheme.colors`.
 *  - Spacing tokens via [LocalNewtonSpacing] (`NewtonTheme.spacing.lg`).
 *  - Mono typography via [LocalNewtonMonoTypography]
 *    (`NewtonTheme.mono.monoLarge`).
 *  - Pill / icon-cap shapes via [LocalNewtonShapes] (`NewtonTheme.shapes.pill`).
 *  - Fix-status colours via [LocalFixStatusColors]
 *    (`NewtonTheme.fixColors.fixed`).
 *
 * To opt into the high-visibility outdoor profile, pass `fieldMode = true`.
 * The full wiring (user preference + sensor-driven auto switch) is Phase 5.
 */

// ----- Material 3 colour scheme (driven by Field Blue) --------------------

private val LightColors = lightColorScheme(
    primary = NewtonPalette.Brand,
    onPrimary = Color.White,
    primaryContainer = NewtonPalette.BrandSoft,
    onPrimaryContainer = NewtonPalette.BrandDeep,
    inversePrimary = NewtonPalette.BrandMid,

    secondary = NewtonPalette.BrandDeep,
    onSecondary = Color.White,
    secondaryContainer = NewtonPalette.BrandFaint,
    onSecondaryContainer = NewtonPalette.BrandDeep,

    tertiary = NewtonPalette.BrandDeep,
    onTertiary = Color.White,
    tertiaryContainer = NewtonPalette.BrandFaint,
    onTertiaryContainer = NewtonPalette.BrandDeep,

    background = NewtonPalette.Bg,
    onBackground = NewtonPalette.Text,
    surface = NewtonPalette.Surface,
    onSurface = NewtonPalette.Text,
    surfaceVariant = NewtonPalette.SurfaceTint,
    onSurfaceVariant = Color(0xA80E2A57), // text-2 alpha 0.66

    surfaceContainerLowest = NewtonPalette.Surface,
    surfaceContainerLow = NewtonPalette.SurfaceTint,
    surfaceContainer = NewtonPalette.SurfaceTint,
    surfaceContainerHigh = NewtonPalette.SurfaceDeep,
    surfaceContainerHighest = NewtonPalette.SurfaceDeep,

    surfaceTint = NewtonPalette.Brand,

    outline = Color(0x380E2A57),     // hairline-strong 0.22
    outlineVariant = Color(0x1A0E2A57), // hairline 0.10

    error = NewtonPalette.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFCE3E7),
    onErrorContainer = Color(0xFF6A1320),
)

private val DarkColors = darkColorScheme(
    primary = NewtonPalette.BrandMid,
    onPrimary = NewtonPalette.BrandDeep,
    primaryContainer = NewtonPalette.BrandDeep,
    onPrimaryContainer = NewtonPalette.BrandSoft,

    secondary = NewtonPalette.BrandMid,
    onSecondary = NewtonPalette.BrandDeep,
    secondaryContainer = NewtonPalette.BrandDeep,
    onSecondaryContainer = NewtonPalette.BrandSoft,

    background = NewtonPalette.BgDark,
    onBackground = NewtonPalette.OnSurfaceDark,
    surface = NewtonPalette.SurfaceDark,
    onSurface = NewtonPalette.OnSurfaceDark,
    surfaceVariant = NewtonPalette.SurfaceVariantDark,
    onSurfaceVariant = NewtonPalette.OnSurfaceMutedDark,

    outline = NewtonPalette.OutlineDark,
    outlineVariant = NewtonPalette.OutlineDark,

    error = NewtonPalette.NoFixRed,
    onError = Color.White,
)

// ----- Fix-status colours -------------------------------------------------

private val LightFixColors = FixStatusColors(
    fixed = NewtonPalette.FixGreen,
    float = NewtonPalette.FloatYellow,
    single = NewtonPalette.SingleOrange,
    noFix = NewtonPalette.NoFixRed,
)

private val DarkFixColors = LightFixColors // status accents read fine on dark surfaces

val LocalFixStatusColors = staticCompositionLocalOf<FixStatusColors> {
    error("FixStatusColors not provided — wrap your composable in NewtonTheme.")
}

// ----- Theme composable ---------------------------------------------------

/**
 * @param darkTheme follow system dark preference (M3 only — dark Field Blue is
 *   placeholder until the spec defines it).
 * @param fieldMode opt into the high-visibility outdoor profile (warmer tint,
 *   stronger hairlines, lifted text alphas — see [FieldModeNewtonColors]).
 */
@Composable
fun NewtonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fieldMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val m3Colors = if (darkTheme) DarkColors else LightColors
    val fixColors = if (darkTheme) DarkFixColors else LightFixColors
    val newtonColors = if (fieldMode) FieldModeNewtonColors else LightNewtonColors

    CompositionLocalProvider(
        LocalFixStatusColors provides fixColors,
        LocalNewtonColors provides newtonColors,
        LocalNewtonSpacing provides NewtonSpacing(),
        LocalNewtonShapes provides NewtonShapes(),
        LocalNewtonMonoTypography provides NewtonMonoTypography(),
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography = NewtonM3Typography,
            shapes = NewtonM3Shapes,
            content = content,
        )
    }
}

// ----- Accessor object ----------------------------------------------------

/**
 * Ergonomic accessor mirroring [MaterialTheme]. Usage:
 * ```
 * Text("foo", color = NewtonTheme.colors.text2)
 * Spacer(Modifier.height(NewtonTheme.spacing.sectionGap))
 * Text("1.234", style = NewtonTheme.mono.monoLarge)
 * ```
 */
object NewtonTheme {
    val colors: NewtonColors
        @Composable
        @ReadOnlyComposable
        get() = LocalNewtonColors.current

    val spacing: NewtonSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalNewtonSpacing.current

    val shapes: NewtonShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalNewtonShapes.current

    val mono: NewtonMonoTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalNewtonMonoTypography.current

    val fixColors: FixStatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFixStatusColors.current
}
