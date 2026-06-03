package ru.newton.fieldapp.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ru.newton.fieldapp.core.ui.R

/**
 * Field Blue typography scale per spec §3.2.
 *
 * The spec calls for `Inter` (UI) + `JetBrains Mono` (numerics/metrics). Both
 * are deferred to a follow-up task — the font swap is a one-line change of
 * the [FieldUiFontFamily] / [FieldMonoFontFamily] aliases below. Sizes,
 * weights, line-heights and letter-spacing already match the spec.
 *
 * Tabular figures (`fontFeatureSettings = "tnum"`) are applied to every mono
 * style so digit columns line up regardless of which glyphs are present.
 *
 * Mapping spec roles → M3 [Typography]:
 *  | Spec               | M3                    |
 *  |--------------------|-----------------------|
 *  | Display-XL 40/700  | displayLarge          |
 *  | Display-L  30/800  | displayMedium         |
 *  | Display-M  24/800  | displaySmall          |
 *  | Title-L    22/700  | headlineMedium        |
 *  | Title-M    17/700  | titleLarge            |
 *  | Title-S    15/700  | titleMedium           |
 *  | Body       14/500  | bodyLarge             |
 *  | Body-S     13/500  | bodyMedium            |
 *  | Caption    12/500  | bodySmall             |
 *  | Micro-cap  11/800  | labelMedium (UPPERCASE at call site) |
 *
 * Mono styles (Mono-XL/L/M/S) have no clean M3 slot — read via
 * `NewtonTheme.mono.monoLarge` etc.
 */

// Bundled variable fonts. Inter has axes (opsz, wght); JetBrains Mono has (wght).
// We only drive `wght` from Compose — opsz is left at the font's default, which
// follows the size automatically.
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun interFont(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun jbmFont(weight: Int) = Font(
    R.font.jetbrains_mono,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

internal val FieldUiFontFamily: FontFamily = FontFamily(
    interFont(400),
    interFont(500),
    interFont(600),
    interFont(700),
    interFont(800),
)

internal val FieldMonoFontFamily: FontFamily = FontFamily(
    jbmFont(500),
    jbmFont(600),
    jbmFont(700),
)

private const val MonoFeatureTabular = "tnum"

internal val NewtonM3Typography: Typography = Typography(
    // ----- Display ---------------------------------------------------------
    displayLarge = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.025).em,
    ),
    displayMedium = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.025).em,
    ),
    displaySmall = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 24.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.022).em,
    ),
    // ----- Headline / Title -----------------------------------------------
    headlineLarge = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.005).em,
    ),
    titleMedium = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // ----- Body ------------------------------------------------------------
    bodyLarge = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.01.em,
    ),
    // ----- Label / Micro-cap ----------------------------------------------
    // Apply UPPERCASE + letter-spacing at call site for micro-cap usage.
    labelLarge = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.06.em,
    ),
    labelMedium = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W800,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.18.em,
    ),
    labelSmall = TextStyle(
        fontFamily = FieldUiFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.14.em,
    ),
)

/**
 * Mono typography scale per spec §3.2. All styles carry the `tnum` feature
 * for tabular figures — critical so coordinate columns don't shift between
 * digit changes.
 */
@Immutable
data class NewtonMonoTypography(
    val monoXl: TextStyle = TextStyle(
        fontFamily = FieldMonoFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.025).em,
        fontFeatureSettings = MonoFeatureTabular,
    ),
    val monoLarge: TextStyle = TextStyle(
        fontFamily = FieldMonoFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = MonoFeatureTabular,
    ),
    val monoMedium: TextStyle = TextStyle(
        fontFamily = FieldMonoFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = MonoFeatureTabular,
    ),
    val monoSmall: TextStyle = TextStyle(
        fontFamily = FieldMonoFontFamily,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.02.em,
        fontFeatureSettings = MonoFeatureTabular,
    ),
)

val LocalNewtonMonoTypography = staticCompositionLocalOf { NewtonMonoTypography() }
