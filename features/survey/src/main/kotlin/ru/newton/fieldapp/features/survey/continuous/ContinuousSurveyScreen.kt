package ru.newton.fieldapp.features.survey.continuous

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.components.NewtonSuccessButton
import ru.newton.fieldapp.gnss.data.FixQuality

@Composable
fun ContinuousSurveyScreen(
    onBack: () -> Unit,
    viewModel: ContinuousSurveyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val codes by viewModel.codeLibrary.collectAsStateWithLifecycle()

    ContinuousSurveyContent(
        state = state,
        codeLibrary = codes,
        onBack = onBack,
        onModeChange = viewModel::setMode,
        onDistanceChange = viewModel::setDistanceThreshold,
        onTimeChange = viewModel::setTimeThreshold,
        onCodeChange = viewModel::setCode,
        onStart = viewModel::start,
        onStop = viewModel::stop,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContinuousSurveyContent(
    state: ContinuousSurveyState,
    codeLibrary: List<String>,
    onBack: () -> Unit,
    onModeChange: (ContinuousMode) -> Unit,
    onDistanceChange: (Double) -> Unit,
    onTimeChange: (Int) -> Unit,
    onCodeChange: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Непрерывная съёмка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                if (state.running) {
                    NewtonPrimaryButton(
                        onClick = onStop,
                        text = "Остановить",
                        icon = Icons.Default.Stop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    NewtonSuccessButton(
                        onClick = onStart,
                        text = "Начать",
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Триггер")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.mode == ContinuousMode.DISTANCE,
                            onClick = { onModeChange(ContinuousMode.DISTANCE) },
                            label = { Text("по расстоянию") },
                            enabled = !state.running,
                        )
                        FilterChip(
                            selected = state.mode == ContinuousMode.TIME,
                            onClick = { onModeChange(ContinuousMode.TIME) },
                            label = { Text("по времени") },
                            enabled = !state.running,
                        )
                    }
                    when (state.mode) {
                        ContinuousMode.DISTANCE -> {
                            var text by remember(state.distanceThresholdM) {
                                androidx.compose.runtime.mutableStateOf("%.2f".format(state.distanceThresholdM))
                            }
                            OutlinedTextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                    it.trim().replace(',', '.').toDoubleOrNull()?.let(onDistanceChange)
                                },
                                label = { Text("Шаг, м") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = !state.running,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        ContinuousMode.TIME -> {
                            var text by remember(state.timeThresholdSec) {
                                androidx.compose.runtime.mutableStateOf(state.timeThresholdSec.toString())
                            }
                            OutlinedTextField(
                                value = text,
                                onValueChange = {
                                    text = it
                                    it.trim().toIntOrNull()?.let(onTimeChange)
                                },
                                label = { Text("Период, секунд") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = !state.running,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Код для всех точек")
                    OutlinedTextField(
                        value = state.code,
                        onValueChange = onCodeChange,
                        label = { Text("Код (необязательно)") },
                        singleLine = true,
                        enabled = !state.running,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (codeLibrary.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            codeLibrary.forEach { code ->
                                AssistChip(
                                    onClick = { onCodeChange(code) },
                                    label = { Text(code) },
                                    enabled = !state.running,
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
                }
            }

            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NewtonSectionLabel(if (state.running) "Идёт запись" else "Готов к запуску")
                    Text(
                        "Сохранено: ${state.savedCount}",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    state.lastSavedName?.let {
                        Text(
                            "Последняя точка: $it",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (state.running) {
                        when (state.mode) {
                            ContinuousMode.DISTANCE -> Text(
                                "Прошли с прошлой: ${"%.2f".format(state.distanceSinceLastSaveM)} м " +
                                    "/ ${"%.2f".format(state.distanceThresholdM)} м",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            ContinuousMode.TIME -> Text(
                                "Прошло секунд: ${state.secondsSinceLastSave} / ${state.timeThresholdSec}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            "Текущий фикс: ${describeFix(state.currentFix)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            state.errorMessage?.let { msg ->
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun describeFix(fix: FixQuality): String = when (fix) {
    FixQuality.NoFix -> "нет фикса"
    FixQuality.Single -> "Single"
    FixQuality.DGnss -> "DGNSS"
    FixQuality.FloatRtk -> "Float RTK"
    FixQuality.FixedRtk -> "Fixed RTK"
    is FixQuality.Ppp -> "PPP"
}
