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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.gnss.data.FixQuality

@Composable
fun PointSurveyScreen(
    onBack: () -> Unit,
    viewModel: PointSurveyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
                PointSurveyState.Idle -> Idle(onStart)
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
private fun Idle(onStart: () -> Unit) {
    Text(
        "Установите тур, удерживайте антенну над пикетом и нажмите «Начать». " +
            "Будет накоплено N эпох, заданное в Параметрах съёмки.",
        style = MaterialTheme.typography.bodyMedium,
    )
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Text("Начать накопление")
    }
}

@Composable
private fun Collecting(state: PointSurveyState.Collecting, onCancel: () -> Unit) {
    val noDataYet = state.lastEpochAtUtc == 0L
    val waitingForFix = !noDataYet && state.currentFix == FixQuality.NoFix && state.collected == 0

    Text(
        "Накоплено: ${state.collected} / ${state.target}",
        style = MaterialTheme.typography.titleMedium,
    )
    LinearProgressIndicator(
        progress = { state.collected.toFloat() / state.target.toFloat().coerceAtLeast(1f) },
        modifier = Modifier.fillMaxWidth(),
    )
    when {
        noDataYet -> ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Нет данных от приёмника",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    "Проверьте подключение Bluetooth в «Настройки → Подключение». " +
                        "Также убедитесь, что NMEA-вывод включён на приёмнике.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        waitingForFix -> ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Ожидание фикса…",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Приёмник на связи, но решение пока не получено. На открытом небе " +
                        "обычно занимает 30–60 секунд после первого включения.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> Text(
            "Текущий фикс: ${describe(state.currentFix)}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
}

@Composable
private fun Ready(
    state: PointSurveyState.Ready,
    onNameChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    Button(
        onClick = onSave,
        enabled = state.name.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Сохранить") }
    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
        Text("Отменить и накопить заново")
    }
}

@Composable
private fun Saved(pointId: Long, onReset: () -> Unit) {
    Text("Точка сохранена (id=$pointId)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
        Text("Снять следующую")
    }
}

@Composable
private fun ErrorState(message: String, onReset: () -> Unit) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Назад") }
}

private fun describe(fix: FixQuality): String = when (fix) {
    FixQuality.NoFix -> "нет фикса"
    FixQuality.Single -> "Single"
    FixQuality.DGnss -> "DGNSS"
    FixQuality.FloatRtk -> "Float RTK"
    FixQuality.FixedRtk -> "Fixed RTK"
    is FixQuality.Ppp -> "PPP"
}
