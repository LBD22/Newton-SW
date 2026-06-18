package ru.newton.fieldapp.features.settings.ppp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.components.PendingBanner
import ru.newton.fieldapp.data.receiver.PendingChangesService
import ru.newton.fieldapp.domain.receiver.PppSetting
import ru.newton.fieldapp.features.settings.nav.PendingChangesViewModel
import javax.inject.Inject

/**
 * SET — PPP режим. Раздел `ppp <TYPE> <on|off>` командного порта плюс
 * особый случай `ppp sbas <on|off> <SBAS_SYSTEM>`. Изменения уходят в
 * [PendingChangesService] как часть `ReceiverConfigPatch.ppp`; применяются
 * через общий Apply-флоу.
 */
data class PppDraft(
    val type: String = "rtcmssr",
    val enabled: Boolean = false,
    val sbasSystem: String = "egnos",
)

@HiltViewModel
class PppViewModel
    @Inject
    constructor(
        private val pendingChanges: PendingChangesService,
    ) : ViewModel() {
        private val _draft = MutableStateFlow(
            pendingChanges.patch.value.ppp?.let { current ->
                PppDraft(
                    type = current.type,
                    enabled = current.enabled,
                    sbasSystem = current.sbasSystem ?: "egnos",
                )
            } ?: PppDraft(),
        )
        val draft: StateFlow<PppDraft> = _draft.asStateFlow()

        fun setType(type: String) { _draft.value = _draft.value.copy(type = type) }
        fun setEnabled(enabled: Boolean) { _draft.value = _draft.value.copy(enabled = enabled) }
        fun setSbasSystem(s: String) { _draft.value = _draft.value.copy(sbasSystem = s) }

        fun queue() {
            val d = _draft.value
            viewModelScope.launch {
                pendingChanges.update {
                    it.copy(
                        ppp = PppSetting(
                            type = d.type,
                            enabled = d.enabled,
                            sbasSystem = if (d.type == "sbas") d.sbasSystem else null,
                        ),
                    )
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PppScreen(
    onBack: () -> Unit,
    onNavigateToApply: () -> Unit = {},
    viewModel: PppViewModel = hiltViewModel(),
    pendingViewModel: PendingChangesViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val pendingCount by pendingViewModel.pendingCount.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("PPP / SBAS") },
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (pendingCount > 0) {
                    PendingBanner(
                        pendingCount = pendingCount,
                        onApply = onNavigateToApply,
                    )
                }
                NewtonPrimaryButton(
                    onClick = {
                        viewModel.queue()
                        onBack()
                    },
                    text = "В очередь команд",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            NewtonSectionLabel("Режим PPP")
                            Text(
                                "PPP — Precise Point Positioning. Альтернатива RTK для зон без " +
                                    "связи с касте́ром или базой.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = draft.enabled,
                            onCheckedChange = viewModel::setEnabled,
                        )
                    }
                }
            }
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    NewtonSectionLabel("Тип")
                    PppTypeRow("rtcmssr", "RTCM SSR", draft.type, viewModel::setType)
                    PppTypeRow("orient", "Orient+", draft.type, viewModel::setType)
                    PppTypeRow("pass2pass", "PASS2PASS", draft.type, viewModel::setType)
                    PppTypeRow("sino", "SINO", draft.type, viewModel::setType)
                    PppTypeRow("sbas", "SBAS", draft.type, viewModel::setType)
                    PppTypeRow("rtk", "RTK (как PPP)", draft.type, viewModel::setType)
                }
            }
            if (draft.type == "sbas") {
                NewtonCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        NewtonSectionLabel("Система SBAS")
                        SbasRow("egnos", "EGNOS (Европа)", draft.sbasSystem, viewModel::setSbasSystem)
                        SbasRow("waas", "WAAS (Америка)", draft.sbasSystem, viewModel::setSbasSystem)
                        SbasRow("msas", "MSAS (Япония)", draft.sbasSystem, viewModel::setSbasSystem)
                        SbasRow("gagan", "GAGAN (Индия)", draft.sbasSystem, viewModel::setSbasSystem)
                    }
                }
            }
        }
    }
}

@Composable
private fun PppTypeRow(value: String, label: String, current: String, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = current == value, onClick = { onClick(value) })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = current == value, onClick = { onClick(value) })
        Text(label, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SbasRow(value: String, label: String, current: String, onClick: (String) -> Unit) =
    PppTypeRow(value, label, current, onClick)
