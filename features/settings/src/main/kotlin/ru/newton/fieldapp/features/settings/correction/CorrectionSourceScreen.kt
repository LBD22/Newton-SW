package ru.newton.fieldapp.features.settings.correction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.gnss.ntrip.NtripProfile
import ru.newton.fieldapp.gnss.ntrip.NtripState

@Composable
fun CorrectionSourceScreen(
    onBack: () -> Unit,
    onManageProfiles: () -> Unit,
    viewModel: CorrectionSourceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CorrectionSourceContent(
        state = state,
        onBack = onBack,
        onManageProfiles = onManageProfiles,
        onSelectProfile = viewModel::onSelectProfile,
        onDisable = viewModel::onDisable,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CorrectionSourceContent(
    state: CorrectionSourceState,
    onBack: () -> Unit,
    onManageProfiles: () -> Unit,
    onSelectProfile: (Long) -> Unit,
    onDisable: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Источник коррекций") },
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
            NtripStatusCard(state.ntripState)

            Text("NTRIP профили", style = MaterialTheme.typography.titleMedium)
            if (state.profiles.isEmpty()) {
                Text(
                    "Нет сохранённых профилей. Создайте профиль в разделе «NTRIP профили».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.profiles, key = NtripProfile::id) { profile ->
                        ProfileCard(
                            profile = profile,
                            isActive = profile.id == state.activeProfileId,
                            onSelect = { onSelectProfile(profile.id) },
                        )
                    }
                }
            }

            if (state.activeProfileId != null) {
                NewtonSecondaryButton(
                    onClick = onDisable,
                    text = "Отключить NTRIP",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            NewtonSecondaryButton(
                onClick = onManageProfiles,
                text = "Управление профилями…",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun NtripStatusCard(state: NtripState) {
    val text = when (state) {
        NtripState.Idle -> "NTRIP: не подключён"
        is NtripState.FetchingSourceTable -> "Получение source-table…"
        is NtripState.Connecting -> "Подключение к ${state.mountpoint} (попытка ${state.attempt})…"
        is NtripState.AwaitingCorrections -> "Подключено к ${state.mountpoint} — ожидание поправок (нужна позиция)…"
        is NtripState.Streaming -> "Активно: ${state.mountpoint}, получено ${state.bytesReceived}Б"
        is NtripState.Reconnecting -> "Переподключение к ${state.mountpoint} через ${state.nextAttemptInMs / 1000}с"
        is NtripState.Failed -> "Ошибка: ${state.reason}${state.httpCode?.let { " (HTTP $it)" }.orEmpty()}"
    }
    val color = if (state is NtripState.Streaming) {
        MaterialTheme.colorScheme.primary
    } else if (state is NtripState.Failed) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Text(text, color = color)
    }
}

@Composable
private fun ProfileCard(
    profile: NtripProfile,
    isActive: Boolean,
    onSelect: () -> Unit,
) {
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(profile.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${profile.host}:${profile.port}/${profile.mountpoint}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            if (isActive) {
                Text(
                    "Активен",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                NewtonPrimaryButton(onClick = onSelect, text = "Выбрать")
            }
        }
    }
}
