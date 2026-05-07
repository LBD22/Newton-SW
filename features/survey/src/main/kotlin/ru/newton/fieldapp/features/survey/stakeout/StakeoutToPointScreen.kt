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
import androidx.hilt.navigation.compose.hiltViewModel
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
        withinTolerance = withinTolerance,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    )

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    Button(
        onClick = onSave,
        enabled = withinTolerance,
        modifier = Modifier.fillMaxWidth(),
        colors = if (withinTolerance) {
            ButtonDefaults.buttonColors(
                containerColor = LocalFixStatusColors.current.fixed,
                contentColor = Color.White,
            )
        } else {
            ButtonDefaults.buttonColors()
        },
    ) {
        Text(
            if (withinTolerance) {
                "Сохранить как-есть"
            } else {
                "Сначала войдите в допуск (${"%.2f".format(state.toleranceM)} м)"
            },
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
 * Rotation = `targetAzimuth - deviceHeading`. Both are in geographic degrees
 * (0 = N, clockwise). When the surveyor turns to face the target, the arrow
 * settles to "up". When they're on target ([withinTolerance]), the arrow is
 * replaced by a static green disc — the user looks down at the pole, not at
 * the screen.
 */
@Composable
private fun DirectionArrow(
    targetAzimuthDeg: Double,
    deviceHeadingDeg: Float,
    withinTolerance: Boolean,
    modifier: Modifier = Modifier,
) {
    val fixed = LocalFixStatusColors.current.fixed
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val onTarget = withinTolerance

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val rotation = ((targetAzimuthDeg.toFloat() - deviceHeadingDeg) + 360f) % 360f
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(if (onTarget) fixed.copy(alpha = 0.18f) else container),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .size(180.dp)
                    .aspectRatio(1f)
                    .rotate(if (onTarget) 0f else rotation),
            ) {
                val w = size.width
                val h = size.height
                if (onTarget) {
                    // Bold green check mark — composed of two strokes.
                    val stroke = Stroke(width = w * 0.10f)
                    val checkPath = Path().apply {
                        moveTo(w * 0.22f, h * 0.55f)
                        lineTo(w * 0.45f, h * 0.78f)
                        lineTo(w * 0.80f, h * 0.32f)
                    }
                    drawPath(checkPath, fixed, style = stroke)
                } else {
                    // Filled triangular arrow with a tail — points "up" at 0°.
                    val centerX = w / 2f
                    val arrowPath = Path().apply {
                        moveTo(centerX, h * 0.10f)             // tip
                        lineTo(w * 0.78f, h * 0.65f)           // bottom-right
                        lineTo(centerX + w * 0.08f, h * 0.55f) // inner notch right
                        lineTo(centerX + w * 0.08f, h * 0.92f) // tail right
                        lineTo(centerX - w * 0.08f, h * 0.92f) // tail left
                        lineTo(centerX - w * 0.08f, h * 0.55f) // inner notch left
                        lineTo(w * 0.22f, h * 0.65f)           // bottom-left
                        close()
                    }
                    drawPath(arrowPath, primary)
                }
            }
            // Tiny "N" mark at the top of the disc — orients the user.
            Canvas(modifier = Modifier.size(220.dp)) {
                val w = size.width
                val h = size.height
                val stroke = Stroke(width = 3f)
                drawCircle(
                    color = primary.copy(alpha = 0.25f),
                    radius = w * 0.48f,
                    center = Offset(w / 2f, h / 2f),
                    style = stroke,
                )
                // Pip at top representing magnetic north relative to phone —
                // since canvas is anchored to phone, this stays at 12 o'clock.
                val pipSize = Size(w * 0.04f, h * 0.04f)
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(w / 2f - pipSize.width / 2f, h * 0.02f),
                    size = pipSize,
                )
            }
        }
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
