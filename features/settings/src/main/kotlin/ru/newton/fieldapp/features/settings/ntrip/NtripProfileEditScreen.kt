package ru.newton.fieldapp.features.settings.ntrip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NtripProfileEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: NtripProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedId) {
        if (state.savedId != null) onSaved()
    }

    NtripProfileEditContent(
        state = state,
        onBack = onBack,
        onNameChanged = viewModel::onNameChanged,
        onHostChanged = viewModel::onHostChanged,
        onPortChanged = viewModel::onPortChanged,
        onMountpointChanged = viewModel::onMountpointChanged,
        onLoginChanged = viewModel::onLoginChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onSendNmeaChanged = viewModel::onSendNmeaChanged,
        onUseTlsChanged = viewModel::onUseTlsChanged,
        onSave = viewModel::onSaveClicked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NtripProfileEditContent(
    state: NtripProfileEditState,
    onBack: () -> Unit,
    onNameChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onMountpointChanged: (String) -> Unit,
    onLoginChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSendNmeaChanged: (Boolean) -> Unit,
    onUseTlsChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == 0L) "Новый NTRIP профиль" else "NTRIP профиль") },
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
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChanged,
                label = { Text("Имя профиля") },
                isError = state.errors.name != null,
                supportingText = state.errors.name?.let { { Text(it) } },
                singleLine = true,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.host,
                onValueChange = onHostChanged,
                label = { Text("Хост") },
                isError = state.errors.host != null,
                supportingText = state.errors.host?.let { { Text(it) } },
                singleLine = true,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.portText,
                onValueChange = onPortChanged,
                label = { Text("Порт") },
                isError = state.errors.port != null,
                supportingText = state.errors.port?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.mountpoint,
                onValueChange = onMountpointChanged,
                label = { Text("Mountpoint") },
                isError = state.errors.mountpoint != null,
                supportingText = state.errors.mountpoint?.let { { Text(it) } },
                singleLine = true,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.login,
                onValueChange = onLoginChanged,
                label = { Text("Логин") },
                singleLine = true,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChanged,
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.useTls,
                    onCheckedChange = onUseTlsChanged,
                    enabled = !state.saving,
                )
                Text(
                    "HTTPS (TLS) — для современных кастеров",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.sendNmea,
                    onCheckedChange = onSendNmeaChanged,
                    enabled = !state.saving,
                )
                Text(
                    "Отправлять GPGGA на кастер (для VRS)",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            NewtonPrimaryButton(
                onClick = onSave,
                text = if (state.saving) "Сохранение…" else "Сохранить",
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
