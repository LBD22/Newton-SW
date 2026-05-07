package ru.newton.fieldapp.features.project.addpoint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.theme.NewtonTheme

@Composable
fun AddPointScreen(
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddPointViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is AddPointState.Saved) onSaved()
    }

    AddPointContent(
        state = state,
        onNameChanged = viewModel::onNameChanged,
        onCodeChanged = viewModel::onCodeChanged,
        onNorthingChanged = viewModel::onNorthingChanged,
        onEastingChanged = viewModel::onEastingChanged,
        onHeightChanged = viewModel::onHeightChanged,
        onSave = viewModel::onSaveClicked,
        onAcknowledgeFailure = viewModel::onAcknowledgeFailure,
        onCancel = onCancel,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPointContent(
    state: AddPointState,
    onNameChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onNorthingChanged: (String) -> Unit,
    onEastingChanged: (String) -> Unit,
    onHeightChanged: (String) -> Unit,
    onSave: () -> Unit,
    onAcknowledgeFailure: () -> Unit,
    onCancel: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новая точка") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        val errors = (state as? AddPointState.Editing)?.errors ?: AddPointState.FieldErrors()
        val isSaving = state is AddPointState.Saving
        val failed = state as? AddPointState.Failed
        val numericKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChanged,
                label = { Text("Имя точки") },
                singleLine = true,
                isError = errors.name != null,
                supportingText = errors.name?.let { { Text(it) } },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.code,
                onValueChange = onCodeChanged,
                label = { Text("Код (необязательно)") },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.northing,
                onValueChange = onNorthingChanged,
                label = { Text("N (Север), м") },
                singleLine = true,
                isError = errors.northing != null,
                supportingText = errors.northing?.let { { Text(it) } },
                keyboardOptions = numericKeyboard,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.easting,
                onValueChange = onEastingChanged,
                label = { Text("E (Восток), м") },
                singleLine = true,
                isError = errors.easting != null,
                supportingText = errors.easting?.let { { Text(it) } },
                keyboardOptions = numericKeyboard,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.height,
                onValueChange = onHeightChanged,
                label = { Text("H (Высота), м") },
                singleLine = true,
                isError = errors.height != null,
                supportingText = errors.height?.let { { Text(it) } },
                keyboardOptions = numericKeyboard,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            failed?.let {
                Text(
                    text = it.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onAcknowledgeFailure) { Text("Попробовать ещё") }
            }

            if (failed == null) {
                Button(
                    onClick = onSave,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text(if (isSaving) "Сохранение…" else "Сохранить")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddPointPreview_Empty() {
    NewtonTheme {
        AddPointContent(
            state = AddPointState.Editing(),
            onNameChanged = {},
            onCodeChanged = {},
            onNorthingChanged = {},
            onEastingChanged = {},
            onHeightChanged = {},
            onSave = {},
            onAcknowledgeFailure = {},
            onCancel = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddPointPreview_Errors() {
    NewtonTheme {
        AddPointContent(
            state = AddPointState.Editing(
                name = "  ",
                northing = "abc",
                easting = "1.23",
                height = "",
                errors = AddPointState.FieldErrors(
                    name = "Имя точки обязательно",
                    northing = "N: ожидается число",
                    height = "H: ожидается число",
                ),
            ),
            onNameChanged = {},
            onCodeChanged = {},
            onNorthingChanged = {},
            onEastingChanged = {},
            onHeightChanged = {},
            onSave = {},
            onAcknowledgeFailure = {},
            onCancel = {},
        )
    }
}
