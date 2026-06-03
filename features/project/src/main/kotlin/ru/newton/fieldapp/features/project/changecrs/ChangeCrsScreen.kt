package ru.newton.fieldapp.features.project.changecrs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.components.NewtonSuccessButton
import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.crs.CrsPresets
import ru.newton.fieldapp.crs.displayLabel

@Composable
fun ChangeCrsScreen(
    onBack: () -> Unit,
    onApplied: () -> Unit,
    viewModel: ChangeCrsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        val editing = state as? ChangeCrsState.Editing ?: return@LaunchedEffect
        if (editing.phase is ChangeCrsViewModel.Phase.Done) onApplied()
    }

    ChangeCrsContent(
        state = state,
        onBack = onBack,
        onPickPreset = viewModel::onPickPreset,
        onApply = viewModel::apply,
        onAcknowledgeError = viewModel::acknowledgeError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeCrsContent(
    state: ChangeCrsState,
    onBack: () -> Unit,
    onPickPreset: (String) -> Unit,
    onApply: () -> Unit,
    onAcknowledgeError: () -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Сменить СК") },
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
        bottomBar = {
            val editing = state as? ChangeCrsState.Editing
            val canApply = editing != null &&
                editing.targetPresetId != null &&
                editing.targetPresetId != editing.currentPresetId &&
                editing.phase is ChangeCrsViewModel.Phase.Editing
            val isApplying = editing?.phase is ChangeCrsViewModel.Phase.Applying
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NewtonSecondaryButton(onClick = onBack, text = "Отмена", modifier = Modifier.weight(1f))
                NewtonSuccessButton(
                    onClick = onApply,
                    text = if (isApplying) "Применение…" else "Пересчитать",
                    icon = Icons.Default.Save,
                    enabled = canApply,
                    modifier = Modifier.weight(2f),
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                ChangeCrsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                ChangeCrsState.NotFound -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Проект не найден", style = MaterialTheme.typography.titleMedium)
                }
                is ChangeCrsState.Editing -> EditingBody(state, onOpenPicker = { pickerOpen = true })
            }
        }
    }

    val editing = state as? ChangeCrsState.Editing
    if (pickerOpen && editing != null) {
        CrsPickerDialog(
            currentPresetId = editing.targetPresetId ?: editing.currentPresetId,
            onSelected = {
                onPickPreset(it)
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false },
        )
    }
    val failed = (editing?.phase as? ChangeCrsViewModel.Phase.Failed)
    if (failed != null) {
        AlertDialog(
            onDismissRequest = onAcknowledgeError,
            title = { Text("Не удалось пересчитать") },
            text = { Text(failed.message) },
            confirmButton = { TextButton(onClick = onAcknowledgeError) { Text("Хорошо") } },
        )
    }
}

@Composable
private fun EditingBody(state: ChangeCrsState.Editing, onOpenPicker: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NewtonCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NewtonSectionLabel(state.projectName)
                Text(
                    "Точек в проекте: ${state.totalPoints}. Все они будут пересчитаны " +
                        "в новой системе координат — операцию нельзя отменить, делайте " +
                        "экспорт CSV перед заменой.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val currentLabel = CrsPresets.parse(state.currentPresetId)?.displayLabel()
                    ?: state.currentPresetId
                OutlinedTextField(
                    value = currentLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Текущая СК") },
                    modifier = Modifier.fillMaxWidth(),
                )
                val targetLabel = state.targetPresetId
                    ?.let { CrsPresets.parse(it)?.displayLabel() ?: it }
                    ?: "Не выбрана"
                OutlinedTextField(
                    value = targetLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Новая СК") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = onOpenPicker) { Text("Выбрать") }
                    },
                )
            }
        }
        if (state.preview.isNotEmpty()) {
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NewtonSectionLabel("Предпросмотр")
                        NewtonInfoBadge("первые ${state.preview.size}")
                    }
                    state.preview.forEach { row ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(row.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "до: N=${"%.3f".format(row.oldN)}  E=${"%.3f".format(row.oldE)}  H=${"%.3f".format(row.oldH)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "после: N=${"%.3f".format(row.newN)}  E=${"%.3f".format(row.newE)}  H=${"%.3f".format(row.newH)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrsPickerDialog(
    currentPresetId: String,
    onSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выбор системы координат") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                items(CrsPresets.mvpCatalogue, key = Crs::presetId) { crs ->
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column {
                            TextButton(
                                onClick = { onSelected(crs.presetId) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    crs.displayLabel(),
                                    style = if (crs.presetId == currentPresetId) {
                                        MaterialTheme.typography.titleMedium
                                    } else {
                                        MaterialTheme.typography.bodyLarge
                                    },
                                    color = if (crs.presetId == currentPresetId) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
