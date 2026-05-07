package ru.newton.fieldapp.features.survey.codesets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.data.preferences.CodeSet
import ru.newton.fieldapp.data.preferences.CodeSetsPreferences
import ru.newton.fieldapp.features.survey.defaults.SurveyPreferences
import javax.inject.Inject

data class CodeSetsState(
    val sets: List<CodeSet> = emptyList(),
    val workingCodes: List<String> = emptyList(),
)

@HiltViewModel
class CodeSetsViewModel
    @Inject
    constructor(
        private val codeSets: CodeSetsPreferences,
        private val surveyPrefs: SurveyPreferences,
    ) : ViewModel() {
        val state: StateFlow<CodeSetsState> = kotlinx.coroutines.flow.combine(
            codeSets.sets,
            surveyPrefs.defaults,
        ) { sets, defaults ->
            CodeSetsState(sets = sets, workingCodes = defaults.codeLibrary)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CodeSetsState())

        /** Replace the working set with the contents of [name]. */
        fun load(name: String) {
            viewModelScope.launch {
                val sets = codeSets.sets.first()
                val target = sets.firstOrNull { it.name == name } ?: return@launch
                surveyPrefs.update { copy(codeLibrary = target.codes) }
            }
        }

        /** Persist [set] (creates or replaces). */
        fun save(set: CodeSet) {
            viewModelScope.launch { codeSets.save(set) }
        }

        /** Save the current working list as a named set. */
        fun saveWorkingAs(name: String) {
            viewModelScope.launch {
                val codes = surveyPrefs.defaults.first().codeLibrary
                codeSets.save(CodeSet(name = name.trim(), codes = codes))
            }
        }

        fun delete(name: String) {
            viewModelScope.launch { codeSets.delete(name) }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeSetsScreen(
    onBack: () -> Unit,
    viewModel: CodeSetsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CodeSet?>(null) }
    var saveAsDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Наборы кодов") },
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
            FloatingActionButton(onClick = { editing = CodeSet(name = "", codes = emptyList()) }) {
                Icon(Icons.Default.Add, contentDescription = "Новый набор")
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
            // Working set — what's currently active on PointSurveyScreen chips.
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        NewtonSectionLabel("Рабочий набор")
                        NewtonInfoBadge("${state.workingCodes.size}")
                    }
                    if (state.workingCodes.isEmpty()) {
                        Text(
                            "Пуст. Загрузите сохранённый набор или добавьте коды в " +
                                "«Параметры съёмки → Быстрые коды».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            state.workingCodes.forEach { code ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(code) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    ),
                                )
                            }
                        }
                    }
                    TextButton(onClick = { saveAsDialog = true }) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Text(" Сохранить рабочий как набор…", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Сохранённые наборы (${state.sets.size})")
                    if (state.sets.isEmpty()) {
                        Text(
                            "Пока пусто. Создайте набор кнопкой «+» или сохраните " +
                                "текущий рабочий список.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.sets, key = CodeSet::name) { set ->
                                SetRow(
                                    set = set,
                                    onLoad = { viewModel.load(set.name) },
                                    onEdit = { editing = set },
                                    onDelete = { viewModel.delete(set.name) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { current ->
        EditSetDialog(
            initial = current,
            onConfirm = { newSet ->
                viewModel.save(newSet)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
    if (saveAsDialog) {
        SaveWorkingAsDialog(
            onConfirm = { name ->
                viewModel.saveWorkingAs(name)
                saveAsDialog = false
            },
            onDismiss = { saveAsDialog = false },
        )
    }
}

@Composable
private fun SetRow(
    set: CodeSet,
    onLoad: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(set.name, style = MaterialTheme.typography.titleSmall)
            Text(
                set.codes.take(8).joinToString(" · ") + if (set.codes.size > 8) " …" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onLoad) { Text("Загрузить") }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Редактировать") }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Удалить") }
    }
}

@Composable
private fun EditSetDialog(
    initial: CodeSet,
    onConfirm: (CodeSet) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initial.name) { mutableStateOf(initial.name) }
    val codes = remember(initial) { mutableStateOf(initial.codes.toList()) }
    var newCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial.name.isBlank()) "Новый набор" else "Набор «${initial.name}»") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя набора") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCode,
                        onValueChange = { newCode = it },
                        label = { Text("Новый код") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        val trimmed = newCode.trim()
                        if (trimmed.isNotBlank() && trimmed !in codes.value) {
                            codes.value = codes.value + trimmed
                            newCode = ""
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить")
                    }
                }
                if (codes.value.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        codes.value.forEach { code ->
                            AssistChip(
                                onClick = { codes.value = codes.value - code },
                                label = { Text(code) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && codes.value.isNotEmpty(),
                onClick = {
                    onConfirm(CodeSet(name = name.trim(), codes = codes.value))
                },
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun SaveWorkingAsDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сохранить как набор") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя набора") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) },
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
