package ru.newton.fieldapp.features.project.create

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.components.NewtonSuccessButton
import ru.newton.fieldapp.core.ui.theme.NewtonTheme
import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.crs.CrsPresets
import ru.newton.fieldapp.crs.displayLabel

@Composable
fun CreateProjectScreen(
    onCancel: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: CreateProjectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        (state as? CreateProjectState.Saved)?.let { onCreated(it.projectId) }
    }

    CreateProjectContent(
        state = state,
        onNameChanged = viewModel::onNameChanged,
        onCommentChanged = viewModel::onCommentChanged,
        onCrsPickerOpen = viewModel::onCrsPickerOpen,
        onCrsPickerDismiss = viewModel::onCrsPickerDismiss,
        onCrsSelected = viewModel::onCrsSelected,
        onSaveClicked = viewModel::onSaveClicked,
        onAcknowledgeFailure = viewModel::onAcknowledgeFailure,
        onCancel = onCancel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProjectContent(
    state: CreateProjectState,
    onNameChanged: (String) -> Unit,
    onCommentChanged: (String) -> Unit,
    onCrsPickerOpen: () -> Unit,
    onCrsPickerDismiss: () -> Unit,
    onCrsSelected: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onAcknowledgeFailure: () -> Unit,
    onCancel: () -> Unit,
) {
    val nameError = (state as? CreateProjectState.Editing)?.nameError
    val isSaving = state is CreateProjectState.Saving
    val isFailed = state as? CreateProjectState.Failed

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Новый проект") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
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
            BottomActionBar(
                isSaving = isSaving,
                isFailed = isFailed != null,
                canSave = state.name.isNotBlank(),
                onCancel = onCancel,
                onSave = onSaveClicked,
                onRetry = onAcknowledgeFailure,
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
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NewtonSectionLabel("Параметры проекта")
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onNameChanged,
                        label = { Text("Имя проекта") },
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it) } },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.comment,
                        onValueChange = onCommentChanged,
                        label = { Text("Комментарий (необязательно)") },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val crsLabel = remember(state.crsPresetId) {
                        CrsPresets.parse(state.crsPresetId)?.displayLabel() ?: state.crsPresetId
                    }
                    OutlinedTextField(
                        value = crsLabel,
                        onValueChange = {},
                        label = { Text("Система координат") },
                        readOnly = true,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = onCrsPickerOpen, enabled = !isSaving) {
                                Text("Выбрать")
                            }
                        },
                    )
                }
            }

            isFailed?.let {
                NewtonCard {
                    Text(
                        it.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if ((state as? CreateProjectState.Editing)?.showCrsPicker == true) {
        CrsPickerDialog(
            currentPresetId = state.crsPresetId,
            onSelected = onCrsSelected,
            onDismiss = onCrsPickerDismiss,
        )
    }
}

@Composable
private fun BottomActionBar(
    isSaving: Boolean,
    isFailed: Boolean,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NewtonSecondaryButton(
            onClick = onCancel,
            text = "Отмена",
            modifier = Modifier.weight(1f),
        )
        when {
            isFailed -> NewtonSuccessButton(
                onClick = onRetry,
                text = "Попробовать ещё",
                modifier = Modifier.weight(2f),
            )
            isSaving -> NewtonSuccessButton(
                onClick = {},
                text = "Сохранение…",
                enabled = false,
                modifier = Modifier.weight(2f),
            )
            else -> NewtonSuccessButton(
                onClick = onSave,
                text = "Создать",
                icon = Icons.Default.Save,
                enabled = canSave,
                modifier = Modifier.weight(2f),
            )
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
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

@Suppress("unused")
private val PreviewSpinner: @Composable () -> Unit = { CircularProgressIndicator() }

@Preview(showBackground = true)
@Composable
private fun CreateProjectPreview_Empty() {
    NewtonTheme {
        CreateProjectContent(
            state = CreateProjectState.Editing(),
            onNameChanged = {},
            onCommentChanged = {},
            onCrsPickerOpen = {},
            onCrsPickerDismiss = {},
            onCrsSelected = {},
            onSaveClicked = {},
            onAcknowledgeFailure = {},
            onCancel = {},
        )
    }
}
