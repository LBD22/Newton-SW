package ru.newton.fieldapp.features.settings.ntrip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton

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
        onLoadMountpoints = viewModel::onLoadMountpoints,
        onMountpointSelected = viewModel::onMountpointSelected,
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
    onLoadMountpoints: () -> Unit,
    onMountpointSelected: (String) -> Unit,
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
        // verticalScroll + imePadding: the form is taller than a phone screen
        // once the keyboard is up. Without scroll the password field was clipped
        // and the Save button slid off the bottom edge — unreachable, which read
        // to testers as "saving does nothing".
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
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
            MountpointPicker(
                state = state,
                onLoadMountpoints = onLoadMountpoints,
                onMountpointSelected = onMountpointSelected,
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

            state.saveError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth(),
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

/**
 * Source-table mountpoint picker (NTR-002, Баг-003). Fetches the caster's
 * mountpoint list on demand and lets the user pick one — hand-typing a wrong
 * mountpoint silently breaks RTK. The free-text field above stays available as
 * a fallback for casters that don't publish a source table.
 */
@Composable
private fun MountpointPicker(
    state: NtripProfileEditState,
    onLoadMountpoints: () -> Unit,
    onMountpointSelected: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NewtonSecondaryButton(
            onClick = onLoadMountpoints,
            text = "Загрузить список точек",
            enabled = !state.loadingMountpoints && !state.saving,
        )
        if (state.loadingMountpoints) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.padding(start = 12.dp).size(20.dp),
            )
        }
    }

    state.mountpointError?.let { message ->
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }

    if (state.mountpoints.isNotEmpty()) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            NewtonSecondaryButton(
                onClick = { expanded = true },
                text = "Выбрать из списка (${state.mountpoints.size})",
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.mountpoints.forEach { mp ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(mp.id, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    listOf(mp.format, mp.navSystem)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onMountpointSelected(mp.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
