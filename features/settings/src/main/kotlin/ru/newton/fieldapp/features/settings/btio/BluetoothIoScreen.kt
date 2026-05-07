package ru.newton.fieldapp.features.settings.btio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.navigation.compose.hiltViewModel
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
import ru.newton.fieldapp.data.receiver.PendingChangesService
import javax.inject.Inject

@HiltViewModel
class BluetoothIoViewModel
    @Inject
    constructor(private val pendingChanges: PendingChangesService) : ViewModel() {
        private val _enabled = MutableStateFlow(pendingChanges.patch.value.bluetoothReceiver ?: true)
        val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

        fun setEnabled(v: Boolean) { _enabled.value = v }

        fun queue() {
            viewModelScope.launch {
                pendingChanges.update { it.copy(bluetoothReceiver = _enabled.value) }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothIoScreen(
    onBack: () -> Unit,
    viewModel: BluetoothIoViewModel = hiltViewModel(),
) {
    val enabled by viewModel.enabled.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth I/O приёмника") },
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
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
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
                            NewtonSectionLabel("Bluetooth I/O")
                            Text(
                                "Включает работу Bluetooth-канала приёмника как источника/" +
                                    "приёмника данных. При выключении прекращается всё, что " +
                                    "идёт через `input set bluetooth` или `output add stream " +
                                    "bluetooth`.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!enabled) {
                                Text(
                                    "⚠ Выключение прервёт активный поток NMEA на телефон.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Switch(checked = enabled, onCheckedChange = viewModel::setEnabled)
                    }
                }
            }
        }
    }
}
