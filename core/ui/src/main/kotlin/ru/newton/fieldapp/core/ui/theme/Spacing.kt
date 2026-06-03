package ru.newton.fieldapp.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Named spacing tokens per Field Blue spec §4.1.
 *
 * Base unit 4dp. Use named values when laying out screens — they communicate
 * intent ("between cards") better than naked `8.dp`.
 */
@Immutable
data class NewtonSpacing(
    /** 4dp — smallest gap, inside dense pills. */
    val xs: Dp = 4.dp,
    /** 8dp — tight inline gap. */
    val sm: Dp = 8.dp,
    /** 10dp — between tiles in a grid. */
    val tileGap: Dp = 10.dp,
    /** 12dp — between rows within a card. */
    val md: Dp = 12.dp,
    /** 14dp — between major sections of a screen. */
    val sectionGap: Dp = 14.dp,
    /** 16dp — generous between-section gap. */
    val lg: Dp = 16.dp,
    /** 18dp — screen edge padding (mobile content list). */
    val screenEdge: Dp = 18.dp,
    /** 20dp — large gap, hero padding. */
    val xl: Dp = 20.dp,
    /** 24dp — extra-large gap, top-bar / system spacing. */
    val xxl: Dp = 24.dp,
    /** 32dp — section dividers, top-of-screen margins. */
    val xxxl: Dp = 32.dp,
)

val LocalNewtonSpacing = staticCompositionLocalOf { NewtonSpacing() }
