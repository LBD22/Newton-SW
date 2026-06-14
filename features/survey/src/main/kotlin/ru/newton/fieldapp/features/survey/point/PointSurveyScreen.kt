package ru.newton.fieldapp.features.survey.point

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSuccessButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.gnss.data.FixQuality

@Composable
fun PointSurveyScreen(
    onBack: () -> Unit,
    viewModel: PointSurveyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Drop the Idle "press to start" pre-step — opening this screen IS the intent
    // to capture a point. Auto-start whenever we are Idle, INCLUDING after the
    // user taps «Снять следующую» (which resets to Idle): keying on the Idle-ness
    // re-fires start() each time we return to Idle, instead of dead-ending on the
    // "Подготовка…" placeholder with no way forward.
    val isIdle = state is PointSurveyState.Idle
    LaunchedEffect(isIdle) {
        if (isIdle) viewModel.start()
    }
    PointSurveyContent(
        state = state,
        onBack = onBack,
        onStart = viewModel::start,
        onCancel = viewModel::cancel,
        onNameChanged = viewModel::onNameChanged,
        onCodeChanged = viewModel::onCodeChanged,
        onSave = viewModel::save,
        onReset = viewModel::reset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointSurveyContent(
    state: PointSurveyState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onNameChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Съёмка точки") },
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
                // Idle is a transient state — LaunchedEffect at the screen
                // level fires start() the moment we land here. Show a small
                // placeholder so we never render an empty Column.
                PointSurveyState.Idle -> Text(
                    "Подготовка…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is PointSurveyState.Collecting -> Collecting(state, onCancel)
                is PointSurveyState.Ready -> Ready(state, onNameChanged, onCodeChanged, onSave, onReset)
                PointSurveyState.Saving -> Text("Сохранение…", style = MaterialTheme.typography.titleMedium)
                is PointSurveyState.Saved -> Saved(state.pointId, onReset)
                is PointSurveyState.Error -> ErrorState(state.message, onReset)
            }
        }
    }
}

@Composable
private fun Collecting(state: PointSurveyState.Collecting, onCancel: () -> Unit) {
    val noDataYet = state.lastEpochAtUtc == 0L
    val waitingForFix = !noDataYet && state.currentFix == FixQuality.NoFix && state.collected == 0

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        ru.newton.fieldapp.core.ui.components.EpochProgressRing(
            current = state.collected,
            total = state.target,
        )
    }
    // Single unified status row — replaces the two stacked alert cards.
    // Three modes: hard problem (no data), soft wait (waiting for fix),
    // or live status (collecting fine).
    val (statusTitle, statusHint, isProblem) = when {
        noDataYet -> Triple(
            "Нет данных от приёмника",
            "Проверьте Bluetooth-подключение и NMEA-вывод на приёмнике.",
            true,
        )
        waitingForFix -> Triple(
            "Ожидание фикса…",
            "На открытом небе обычно 30–60 с после включения.",
            false,
        )
        else -> Triple(
            "Фикс: ${describe(state.currentFix)}",
            null,
            false,
        )
    }
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = statusTitle,
                style = MaterialTheme.typography.titleMedium,
                color = if (isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            if (statusHint != null) {
                Text(
                    text = statusHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    NewtonSecondaryButton(
        onClick = onCancel,
        text = "Отмена",
        icon = Icons.Default.Close,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Ready(
    state: PointSurveyState.Ready,
    onNameChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
) {
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Накоплено эпох: ${state.sampleCount}", style = MaterialTheme.typography.titleMedium)
            Text(
                "φ=${"%.7f".format(state.averageLat)}°  λ=${"%.7f".format(state.averageLon)}°",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "h=${"%.3f".format(state.averageH)} м, σH=${"%.3f".format(state.sigmaH)} м",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    OutlinedTextField(
        value = state.name,
        onValueChange = onNameChanged,
        label = { Text("Имя точки") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.code,
        onValueChange = onCodeChanged,
        label = { Text("Код (необязательно)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.codeLibrary.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.codeLibrary.forEach { code ->
                AssistChip(
                    onClick = { onCodeChanged(code) },
                    label = { Text(code) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (state.code == code) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
                )
            }
        }
    }
    NewtonSuccessButton(
        onClick = onSave,
        text = "Сохранить",
        icon = Icons.Default.Check,
        enabled = state.name.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    )
    NewtonSecondaryButton(
        onClick = onReset,
        text = "Отменить и накопить заново",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Saved(pointId: Long, onReset: () -> Unit) {
    Text("Точка сохранена (id=$pointId)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    NewtonPrimaryButton(
        onClick = onReset,
        text = "Снять следующую",
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorState(message: String, onReset: () -> Unit) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    NewtonPrimaryButton(
        onClick = onReset,
        text = "Назад",
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun describe(fix: FixQuality): String = when (fix) {
    FixQuality.NoFix -> "нет фикса"
    FixQuality.Single -> "Single"
    FixQuality.DGnss -> "DGNSS"
    FixQuality.FloatRtk -> "Float RTK"
    FixQuality.FixedRtk -> "Fixed RTK"
    is FixQuality.Ppp -> "PPP"
}
