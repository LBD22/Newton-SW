package ru.newton.fieldapp.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColors = lightColorScheme(
    primary = NewtonPalette.PrimaryBlue,
    onPrimary = NewtonPalette.OnPrimary,
    primaryContainer = NewtonPalette.PrimaryBlueTint,
    onPrimaryContainer = NewtonPalette.PrimaryBlueDeep,
    secondary = NewtonPalette.SuccessGreen,
    onSecondary = NewtonPalette.OnPrimary,
    secondaryContainer = NewtonPalette.SuccessGreenTint,
    onSecondaryContainer = NewtonPalette.SuccessGreenDark,
    tertiary = NewtonPalette.PrimaryBlueLight,
    background = NewtonPalette.PageBackgroundLight,
    onBackground = NewtonPalette.OnSurfaceLight,
    surface = NewtonPalette.SurfaceLight,
    onSurface = NewtonPalette.OnSurfaceLight,
    surfaceVariant = NewtonPalette.SurfaceVariantLight,
    onSurfaceVariant = NewtonPalette.OnSurfaceMutedLight,
    surfaceTint = NewtonPalette.PrimaryBlue,
    outline = NewtonPalette.OutlineLight,
    outlineVariant = NewtonPalette.OutlineLight,
    error = NewtonPalette.NoFixRed,
)

private val DarkColors = darkColorScheme(
    primary = NewtonPalette.PrimaryBlueLight,
    onPrimary = NewtonPalette.OnPrimary,
    primaryContainer = NewtonPalette.PrimaryBlueDeep,
    onPrimaryContainer = NewtonPalette.PrimaryBlueTint,
    secondary = NewtonPalette.SuccessGreen,
    onSecondary = NewtonPalette.OnPrimary,
    secondaryContainer = NewtonPalette.SuccessGreenDark,
    onSecondaryContainer = NewtonPalette.SuccessGreenTint,
    tertiary = NewtonPalette.PrimaryBlue,
    background = NewtonPalette.PageBackgroundDark,
    onBackground = NewtonPalette.OnSurfaceDark,
    surface = NewtonPalette.SurfaceDark,
    onSurface = NewtonPalette.OnSurfaceDark,
    surfaceVariant = NewtonPalette.SurfaceVariantDark,
    onSurfaceVariant = NewtonPalette.OnSurfaceMutedDark,
    outline = NewtonPalette.OutlineDark,
    outlineVariant = NewtonPalette.OutlineDark,
    error = NewtonPalette.NoFixRed,
)

private val LightFixColors = FixStatusColors(
    fixed = NewtonPalette.FixGreen,
    float = NewtonPalette.FloatYellow,
    single = NewtonPalette.SingleOrange,
    noFix = NewtonPalette.NoFixRed,
)

private val DarkFixColors = LightFixColors // accents read fine on dark surfaces too

val LocalFixStatusColors = staticCompositionLocalOf<FixStatusColors> {
    error("FixStatusColors not provided — wrap your composable in NewtonTheme.")
}

@Composable
fun NewtonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val fixColors = if (darkTheme) DarkFixColors else LightFixColors

    CompositionLocalProvider(LocalFixStatusColors provides fixColors) {
        MaterialTheme(
            colorScheme = colors,
            typography = NewtonTypography,
            shapes = NewtonShapes,
            content = content,
        )
    }
}
