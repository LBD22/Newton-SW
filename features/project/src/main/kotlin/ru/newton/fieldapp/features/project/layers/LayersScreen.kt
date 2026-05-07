package ru.newton.fieldapp.features.project.layers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
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
import ru.newton.fieldapp.domain.model.Layer
import ru.newton.fieldapp.domain.model.NewLayer
import ru.newton.fieldapp.domain.repository.LayerRepository
import javax.inject.Inject

/**
 * PRJ-030 — список слоёв проекта; PRJ-031 — диалог редактирования.
 *
 * Слой = ярлык + цвет + флаг видимости. Привязан к проекту через FK,
 * каскадно удаляется при удалении проекта.
 */
@HiltViewModel
class LayersViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: LayerRepository,
    ) : ViewModel() {
        private val projectId: Long = checkNotNull(savedStateHandle["projectId"]) {
            "LayersViewModel requires `projectId` nav argument"
        }

        val layers: StateFlow<List<Layer>> = repository.observeByProject(projectId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        fun create(name: String, colorRgb: Int, visible: Boolean) {
            viewModelScope.launch {
                repository.create(
                    NewLayer(
                        projectId = projectId,
                        name = name.trim(),
                        colorRgb = colorRgb,
                        visible = visible,
                    ),
                )
            }
        }

        fun update(layer: Layer) {
            viewModelScope.launch { repository.update(layer) }
        }

        fun toggleVisibility(layer: Layer) {
            viewModelScope.launch { repository.update(layer.copy(visible = !layer.visible)) }
        }

        fun delete(id: Long) {
            viewModelScope.launch { repository.delete(id) }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayersScreen(
    onBack: () -> Unit,
    viewModel: LayersViewModel = hiltViewModel(),
) {
    val layers by viewModel.layers.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Layer?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Слои") },
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
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Default.Add, contentDescription = "Новый слой")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (layers.isEmpty()) {
                NewtonCard {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NewtonSectionLabel("Слоёв пока нет")
                        Text(
                            "Создайте слой для группировки точек на карте и в экспортах. " +
                                "Слой попадает в поле «code» при экспорте CSV/DXF.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(layers, key = Layer::id) { layer ->
                        LayerRow(
                            layer = layer,
                            onEdit = { editing = layer },
                            onToggle = { viewModel.toggleVisibility(layer) },
                            onDelete = { viewModel.delete(layer.id) },
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        LayerEditDialog(
            initial = null,
            onConfirm = { name, color, visible ->
                viewModel.create(name, color, visible)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
    editing?.let { layer ->
        LayerEditDialog(
            initial = layer,
            onConfirm = { name, color, visible ->
                viewModel.update(layer.copy(name = name, colorRgb = color, visible = visible))
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun LayerRow(
    layer: Layer,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    NewtonCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF000000.toInt() or layer.colorRgb)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    layer.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (layer.visible) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (!layer.visible) {
                    Text(
                        "скрыт",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (layer.visible) "Скрыть" else "Показать",
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Редактировать")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить")
            }
        }
    }
}

@Composable
private fun LayerEditDialog(
    initial: Layer?,
    onConfirm: (name: String, colorRgb: Int, visible: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var colorHex by remember(initial) {
        mutableStateOf("%06X".format(initial?.colorRgb ?: 0xFFFFFF))
    }
    var visible by remember(initial) { mutableStateOf(initial?.visible ?: true) }
    val colorParsed = remember(colorHex) {
        colorHex.trim().removePrefix("#").toIntOrNull(16)?.takeIf { it in 0..0xFFFFFF }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новый слой" else "Слой «${initial.name}»") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя слоя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = colorHex,
                    onValueChange = { colorHex = it.trim().take(7) },
                    label = { Text("Цвет HEX (RRGGBB)") },
                    singleLine = true,
                    isError = colorParsed == null,
                    supportingText = if (colorParsed == null) {
                        { Text("Шесть hex-цифр, без #") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Видимый", modifier = Modifier.weight(1f))
                    Switch(checked = visible, onCheckedChange = { visible = it })
                }
                colorParsed?.let { rgb ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Превью:", style = MaterialTheme.typography.bodyMedium)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF000000.toInt() or rgb)),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && colorParsed != null,
                onClick = { onConfirm(name.trim(), colorParsed ?: 0xFFFFFF, visible) },
            ) { Text(if (initial == null) "Создать" else "Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
