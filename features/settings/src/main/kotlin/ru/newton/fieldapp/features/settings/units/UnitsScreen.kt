package ru.newton.fieldapp.features.settings.units

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.components.SwitchRow
import ru.newton.fieldapp.data.preferences.AngleFormat
import ru.newton.fieldapp.data.preferences.DisplayConfig
import ru.newton.fieldapp.data.preferences.LengthUnit
import ru.newton.fieldapp.data.preferences.UnitsConfig

@Composable
fun UnitsScreen(
    onBack: () -> Unit,
    viewModel: UnitsSettingsViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val display by viewModel.display.collectAsStateWithLifecycle()
    UnitsContent(
        config = config,
        display = display,
        onLength = viewModel::setLength,
        onAngle = viewModel::setAngle,
        onFieldMode = viewModel::setFieldMode,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitsContent(
    config: UnitsConfig,
    display: DisplayConfig,
    onLength: (LengthUnit) -> Unit,
    onAngle: (AngleFormat) -> Unit,
    onFieldMode: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Единицы и формат") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Влияет только на отображение. Внутри приложение всегда хранит " +
                    "координаты в метрах и градусах WGS-84.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Длина")
                    RadioRow(
                        label = "Метры (м)",
                        selected = config.length == LengthUnit.METERS,
                        onClick = { onLength(LengthUnit.METERS) },
                    )
                    RadioRow(
                        label = "Футы (фт)",
                        selected = config.length == LengthUnit.FEET,
                        onClick = { onLength(LengthUnit.FEET) },
                    )
                }
            }

            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Углы")
                    RadioRow(
                        label = "Десятичные градусы (55.7558°)",
                        selected = config.angle == AngleFormat.DECIMAL_DEGREES,
                        onClick = { onAngle(AngleFormat.DECIMAL_DEGREES) },
                    )
                    RadioRow(
                        label = "Градусы-минуты-секунды (55°45'20.88\")",
                        selected = config.angle == AngleFormat.DMS,
                        onClick = { onAngle(AngleFormat.DMS) },
                    )
                }
            }

            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Дисплей")
                    SwitchRow(
                        label = "Полевой режим",
                        checked = display.fieldMode,
                        onCheckedChange = onFieldMode,
                        showDivider = false,
                    )
                    Text(
                        text = "Тёплая поверхность, плотные границы, более жирный " +
                            "текст — лучше читается на ярком солнце.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            Text(
                "Язык интерфейса наследуется от системного. Поддерживается русский.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
