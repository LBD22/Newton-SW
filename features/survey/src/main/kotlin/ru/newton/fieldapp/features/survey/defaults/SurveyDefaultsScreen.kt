package ru.newton.fieldapp.features.survey.defaults

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import javax.inject.Inject

@HiltViewModel
class SurveyDefaultsViewModel
    @Inject
    constructor(
        private val preferences: SurveyPreferences,
    ) : ViewModel() {
        val defaults: StateFlow<SurveyDefaults> = preferences.defaults
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                SurveyDefaults(minEpochs = 10, toleranceHorizontalM = 0.030, toleranceVerticalM = 0.050),
            )

        fun setMinEpochs(v: Int) { viewModelScope.launch { preferences.update { copy(minEpochs = v) } } }
        fun setToleranceH(v: Double) { viewModelScope.launch { preferences.update { copy(toleranceHorizontalM = v) } } }
        fun setToleranceV(v: Double) { viewModelScope.launch { preferences.update { copy(toleranceVerticalM = v) } } }
        fun setNamePrefix(v: String) { viewModelScope.launch { preferences.update { copy(namePrefix = v) } } }
        fun setNamePadding(v: Int) { viewModelScope.launch { preferences.update { copy(namePadding = v) } } }
        fun setTiltEnabled(v: Boolean) { viewModelScope.launch { preferences.update { copy(tiltCorrectionEnabled = v) } } }
        fun setPoleHeight(v: Double) { viewModelScope.launch { preferences.update { copy(poleHeightM = v) } } }
        fun addCode(code: String) {
            val trimmed = code.trim()
            if (trimmed.isBlank()) return
            viewModelScope.launch {
                preferences.update {
                    if (codeLibrary.contains(trimmed)) {
                        this
                    } else {
                        copy(codeLibrary = codeLibrary + trimmed)
                    }
                }
            }
        }
        fun removeCode(code: String) {
            viewModelScope.launch {
                preferences.update { copy(codeLibrary = codeLibrary.filter { it != code }) }
            }
        }
    }

@Composable
fun SurveyDefaultsScreen(
    onBack: () -> Unit,
    viewModel: SurveyDefaultsViewModel = hiltViewModel(),
) {
    val defaults by viewModel.defaults.collectAsStateWithLifecycle()
    SurveyDefaultsContent(
        defaults = defaults,
        onBack = onBack,
        onMinEpochsChanged = viewModel::setMinEpochs,
        onToleranceHChanged = viewModel::setToleranceH,
        onToleranceVChanged = viewModel::setToleranceV,
        onNamePrefixChanged = viewModel::setNamePrefix,
        onNamePaddingChanged = viewModel::setNamePadding,
        onAddCode = viewModel::addCode,
        onRemoveCode = viewModel::removeCode,
        onTiltEnabledChanged = viewModel::setTiltEnabled,
        onPoleHeightChanged = viewModel::setPoleHeight,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurveyDefaultsContent(
    defaults: SurveyDefaults,
    onBack: () -> Unit,
    onMinEpochsChanged: (Int) -> Unit,
    onToleranceHChanged: (Double) -> Unit,
    onToleranceVChanged: (Double) -> Unit,
    onNamePrefixChanged: (String) -> Unit,
    onNamePaddingChanged: (Int) -> Unit,
    onAddCode: (String) -> Unit,
    onRemoveCode: (String) -> Unit,
    onTiltEnabledChanged: (Boolean) -> Unit,
    onPoleHeightChanged: (Double) -> Unit,
) {
    var epochsText by remember(defaults.minEpochs) { mutableStateOf(defaults.minEpochs.toString()) }
    var tolHText by remember(defaults.toleranceHorizontalM) { mutableStateOf("%.3f".format(defaults.toleranceHorizontalM)) }
    var tolVText by remember(defaults.toleranceVerticalM) { mutableStateOf("%.3f".format(defaults.toleranceVerticalM)) }
    var prefixText by remember(defaults.namePrefix) { mutableStateOf(defaults.namePrefix) }
    var paddingText by remember(defaults.namePadding) { mutableStateOf(defaults.namePadding.toString()) }
    var poleHeightText by remember(defaults.poleHeightM) { mutableStateOf("%.2f".format(defaults.poleHeightM)) }
    var newCode by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Параметры съёмки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Накопление")
                    OutlinedTextField(
                        value = epochsText,
                        onValueChange = {
                            epochsText = it
                            it.trim().toIntOrNull()?.let(onMinEpochsChanged)
                        },
                        label = { Text("Минимум эпох на точку") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = tolHText,
                        onValueChange = {
                            tolHText = it
                            it.trim().replace(',', '.').toDoubleOrNull()?.let(onToleranceHChanged)
                        },
                        label = { Text("Допуск горизонтальный, м") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = tolVText,
                        onValueChange = {
                            tolVText = it
                            it.trim().replace(',', '.').toDoubleOrNull()?.let(onToleranceVChanged)
                        },
                        label = { Text("Допуск вертикальный, м") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Эпоха ≈ 1 секунда при 1 Гц. 10 эпох даёт ≈ 1 см " +
                            "повторяемости в Fixed RTK.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Авто-нумерация имён")
                    OutlinedTextField(
                        value = prefixText,
                        onValueChange = {
                            prefixText = it
                            onNamePrefixChanged(it)
                        },
                        label = { Text("Префикс имени") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = paddingText,
                        onValueChange = {
                            paddingText = it
                            it.trim().toIntOrNull()?.let(onNamePaddingChanged)
                        },
                        label = { Text("Цифр в номере (0..8)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val example = remember(prefixText, paddingText) {
                        val pad = paddingText.toIntOrNull()?.coerceIn(0, 8) ?: 3
                        val n = if (pad > 0) "1".padStart(pad, '0') else "1"
                        "${prefixText}$n"
                    }
                    Text(
                        "Пример первой точки: $example",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Быстрые коды")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = newCode,
                            onValueChange = { newCode = it },
                            label = { Text("Новый код") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                if (newCode.isNotBlank()) {
                                    onAddCode(newCode)
                                    newCode = ""
                                }
                            },
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить код")
                        }
                    }
                    if (defaults.codeLibrary.isEmpty()) {
                        Text(
                            "Список пуст. Добавьте часто используемые коды — " +
                                "они появятся кнопками на экране съёмки точки.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            defaults.codeLibrary.forEach { code ->
                                AssistChip(
                                    onClick = { onRemoveCode(code) },
                                    label = { Text(code) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Удалить",
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            NewtonSectionLabel("Tilt-компенсация (IMU)")
                            Text(
                                "Если приёмник «Ньютон» отправляет TRA с pitch/roll/" +
                                    "heading и IMU откалиброван, координата каждой эпохи " +
                                    "приводится к носку вехи — можно работать с " +
                                    "наклонной вехой до 30°.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = defaults.tiltCorrectionEnabled,
                            onCheckedChange = onTiltEnabledChanged,
                        )
                    }
                    OutlinedTextField(
                        value = poleHeightText,
                        onValueChange = {
                            poleHeightText = it
                            it.trim().replace(',', '.').toDoubleOrNull()?.let(onPoleHeightChanged)
                        },
                        label = { Text("Высота вехи, м") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = defaults.tiltCorrectionEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
