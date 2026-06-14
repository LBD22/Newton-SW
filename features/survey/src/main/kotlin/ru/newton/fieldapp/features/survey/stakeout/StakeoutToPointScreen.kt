package ru.newton.fieldapp.features.survey.stakeout

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.theme.LocalFixStatusColors

@Composable
fun StakeoutToPointScreen(
    onBack: () -> Unit,
    viewModel: StakeoutToPointViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StakeoutToPointContent(state = state, onBack = onBack, onSave = viewModel::saveAsBuilt)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StakeoutToPointContent(
    state: StakeoutState,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вынос точки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state) {
                StakeoutState.Loading -> Text("Загрузка цели…", style = MaterialTheme.typography.bodyMedium)
                StakeoutState.WaitingForFix -> Text(
                    "Ожидание фикса…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                is StakeoutState.Active -> ActiveBlock(state, onSave)
                is StakeoutState.Saved -> Text(
                    "As-built сохранён (id=${state.asBuiltPointId})",
                    color = MaterialTheme.colorScheme.primary,
                )
                is StakeoutState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ActiveBlock(state: StakeoutState.Active, onSave: () -> Unit) {
    val withinTolerance = state.vector.distanceM < state.toleranceM
    val context = LocalContext.current
    val deviceHeadingDeg = rememberDeviceHeadingDeg()

    // Buzz exactly once when the surveyor first crosses into tolerance — not
    // every recomposition. `LaunchedEffect(key)` re-runs only when the key
    // flips, so the haptic fires on the transition false→true.
    LaunchedEffect(withinTolerance) {
        if (withinTolerance) vibrateOnTarget(context)
    }

    if (withinTolerance) OnTargetBanner(state.toleranceM)

    // Big direction arrow — rotates relative to phone heading so "up" on the
    // arrow always means "where you should walk". On target the arrow turns
    // into a green check inside the disc.
    DirectionArrow(
        targetAzimuthDeg = state.vector.azimuthDeg,
        deviceHeadingDeg = deviceHeadingDeg,
        headingCorrectionDeg = state.headingCorrectionDeg,
        withinTolerance = withinTolerance,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )

    ru.newton.fieldapp.core.ui.components.NewtonCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Цель: ${state.targetName}", style = MaterialTheme.typography.titleMedium)
            Text(
                "Дистанция: ${"%.3f".format(state.vector.distanceM)} м",
                style = MaterialTheme.typography.headlineSmall,
                color = if (withinTolerance) {
                    LocalFixStatusColors.current.fixed
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                "Азимут: ${"%.1f".format(state.vector.azimuthDeg)}°",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                "ΔH: ${"%+.3f".format(state.vector.deltaH)} м",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
    // When in tolerance — bold success-coloured save. Otherwise — disabled
    // primary (greyed) so the user can't mis-tap before the point is reachable.
    if (withinTolerance) {
        ru.newton.fieldapp.core.ui.components.NewtonSuccessButton(
            onClick = onSave,
            text = "Сохранить как-есть",
            icon = Icons.Default.CheckCircle,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton(
            onClick = onSave,
            text = "Сначала войдите в допуск (${"%.2f".format(state.toleranceM)} м)",
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OnTargetBanner(toleranceM: Double) {
    val onTarget = LocalFixStatusColors.current.fixed
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(onTarget),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                "В ДОПУСКЕ",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Text(
                "Допуск ${"%.2f".format(toleranceM)} м",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

/**
 * Compass-style arrow centred in a disc, rotated so the tip points at the
 * stakeout target relative to which way the phone is facing.
 *
 * Rotation = `targetAzimuth - correctedDeviceHeading`. The target azimuth is in
 * the grid (or true, for geographic) frame; the raw device heading is MAGNETIC,
 * so [headingCorrectionDeg] (declination − convergence) lifts it into the same
 * frame. Without it the arrow is off by the local declination (10-25° in Russia)
 * and the surveyor walks a curve. On target ([withinTolerance]) the arrow is
 * replaced by a static green disc — the user looks down at the pole.
 */
@Composable
private fun DirectionArrow(
    targetAzimuthDeg: Double,
    deviceHeadingDeg: Float,
    headingCorrectionDeg: Double,
    withinTolerance: Boolean,
    modifier: Modifier = Modifier,
) {
    // Bearing relative to phone heading — what the surveyor needs to walk
    // toward. CompassRose handles all the rose / ticks / arrow rendering.
    val correctedHeading = deviceHeadingDeg + headingCorrectionDeg.toFloat()
    val bearing = ((targetAzimuthDeg.toFloat() - correctedHeading) + 360f) % 360f
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ru.newton.fieldapp.core.ui.components.CompassRose(
            bearing = bearing,
            sizeDp = 240.dp,
            onTarget = withinTolerance,
        )
    }
}

/**
 * One-shot 200 ms double-pulse on entering tolerance. Uses [VibratorManager]
 * (API 31+) — minSdk is 31, so the legacy [Vibrator] path is unreachable.
 * Wrapped in runCatching so a missing vibrator on a tablet doesn't crash the
 * stakeout flow.
 */
private fun vibrateOnTarget(context: Context) {
    runCatching {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
                .defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return@runCatching
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 80, 100, 80),
            intArrayOf(0, 255, 0, 255),
            -1,
        )
        vibrator.vibrate(effect)
    }
}
