package ru.newton.fieldapp.features.settings.rover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.domain.receiver.RoverMode

@Composable
fun RoverSettingsScreen(
    onBack: () -> Unit,
    onNavigateToApply: () -> Unit = {},
    viewModel: RoverSettingsViewModel = hiltViewModel(),
    pendingViewModel: ru.newton.fieldapp.features.settings.nav.PendingChangesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingCount by pendingViewModel.pendingCount.collectAsStateWithLifecycle()
    RoverSettingsContent(
        state = state,
        pendingCount = pendingCount,
        onModeSelected = viewModel::onModeSelected,
        onMaskChanged = viewModel::onMaskChanged,
        onMaskCommit = viewModel::onMaskCommit,
        onRtcmIdChanged = viewModel::onRtcmIdChanged,
        onBack = onBack,
        onNavigateToApply = onNavigateToApply,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoverSettingsContent(
    state: RoverSettingsState,
    pendingCount: Int,
    onModeSelected: (RoverMode) -> Unit,
    onMaskChanged: (String) -> Unit,
    onMaskCommit: () -> Unit,
    onRtcmIdChanged: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToApply: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Настройки ровера") },
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
            if (pendingCount > 0) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    ru.newton.fieldapp.core.ui.components.PendingBanner(
                        pendingCount = pendingCount,
                        onApply = onNavigateToApply,
                    )
                }
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
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Режим работы")
                    ModeOption("Автономный (ровер)", state.mode == RoverMode.ROVER) { onModeSelected(RoverMode.ROVER) }
                    ModeOption("От базы", state.mode == RoverMode.ROVER_BASE) { onModeSelected(RoverMode.ROVER_BASE) }
                    ModeOption("От мастера", state.mode == RoverMode.ROVER_MASTER) { onModeSelected(RoverMode.ROVER_MASTER) }
                }
            }
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NewtonSectionLabel("Параметры приёма")
                    OutlinedTextField(
                        value = state.maskText,
                        onValueChange = onMaskChanged,
                        label = { Text("Маска возвышения, °") },
                        isError = state.maskError != null,
                        supportingText = state.maskError?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (!it.isFocused) onMaskCommit() },
                    )
                    OutlinedTextField(
                        value = state.rtcmIdText,
                        onValueChange = onRtcmIdChanged,
                        label = { Text("RTCM id (необязательно)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Text(
                "Изменения станут активны после применения на экране диагностики.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
