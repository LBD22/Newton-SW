package ru.newton.fieldapp.features.settings.bridge

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
import ru.newton.fieldapp.data.receiver.PendingChangesService
import ru.newton.fieldapp.domain.receiver.BluetoothBridgeSetting
import ru.newton.fieldapp.domain.receiver.BluetoothBridgeTarget
import javax.inject.Inject

enum class BridgeKind { NONE, TCP_SERVER, TCP_CLIENT, COM, UHF }

data class BridgeDraft(
    val enabled: Boolean = false,
    val kind: BridgeKind = BridgeKind.NONE,
    val tcpHost: String = "",
    val tcpPort: String = "2101",
    val comIndex: Int = 1,
    val baud: String = "115200",
    val uhfFreq: String = "461.025",
    val uhfProtocol: String = "trimtalk",
    val uhfPower: String = "high",
)

@HiltViewModel
class BridgeViewModel
    @Inject
    constructor(private val pendingChanges: PendingChangesService) : ViewModel() {
        private val _draft = MutableStateFlow(BridgeDraft())
        val draft: StateFlow<BridgeDraft> = _draft.asStateFlow()

        fun setEnabled(v: Boolean) { _draft.value = _draft.value.copy(enabled = v) }
        fun setKind(k: BridgeKind) { _draft.value = _draft.value.copy(kind = k) }
        fun setTcpHost(v: String) { _draft.value = _draft.value.copy(tcpHost = v) }
        fun setTcpPort(v: String) { _draft.value = _draft.value.copy(tcpPort = v) }
        fun setComIndex(v: Int) { _draft.value = _draft.value.copy(comIndex = v) }
        fun setBaud(v: String) { _draft.value = _draft.value.copy(baud = v) }
        fun setUhfFreq(v: String) { _draft.value = _draft.value.copy(uhfFreq = v) }
        fun setUhfProtocol(v: String) { _draft.value = _draft.value.copy(uhfProtocol = v) }
        fun setUhfPower(v: String) { _draft.value = _draft.value.copy(uhfPower = v) }

        fun queue() {
            val d = _draft.value
            val target = when (d.kind) {
                BridgeKind.NONE -> null
                BridgeKind.TCP_SERVER -> BluetoothBridgeTarget.TcpServer(
                    port = d.tcpPort.toIntOrNull() ?: 2101,
                )
                BridgeKind.TCP_CLIENT -> BluetoothBridgeTarget.TcpClient(
                    host = d.tcpHost.trim(),
                    port = d.tcpPort.toIntOrNull() ?: 2101,
                )
                BridgeKind.COM -> BluetoothBridgeTarget.Com(
                    index = d.comIndex.coerceIn(1, 2),
                    baudrate = d.baud.toIntOrNull() ?: 115200,
                )
                BridgeKind.UHF -> BluetoothBridgeTarget.Uhf(
                    frequencyMHz = d.uhfFreq.replace(',', '.').toDoubleOrNull() ?: 461.025,
                    protocol = d.uhfProtocol,
                    baudrate = d.baud.toIntOrNull() ?: 19200,
                    power = d.uhfPower,
                )
            }
            viewModelScope.launch {
                pendingChanges.update {
                    it.copy(
                        bluetoothBridge = BluetoothBridgeSetting(
                            enabled = d.enabled,
                            target = target,
                        ),
                    )
                }
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BridgeScreen(
    onBack: () -> Unit,
    viewModel: BridgeViewModel = hiltViewModel(),
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth Bridge") },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            NewtonSectionLabel("Bridge")
                            Text(
                                "Передача данных между Bluetooth и другим интерфейсом " +
                                    "приёмника. При on данные с BT направляются на " +
                                    "выбранную цель.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = draft.enabled, onCheckedChange = viewModel::setEnabled)
                    }
                }
            }
            NewtonCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    NewtonSectionLabel("Цель")
                    KindRow("Только переключение", BridgeKind.NONE, draft.kind, viewModel::setKind)
                    KindRow("TCP-сервер на приёмнике", BridgeKind.TCP_SERVER, draft.kind, viewModel::setKind)
                    KindRow("TCP-клиент", BridgeKind.TCP_CLIENT, draft.kind, viewModel::setKind)
                    KindRow("COM-порт приёмника", BridgeKind.COM, draft.kind, viewModel::setKind)
                    KindRow("УКВ-радио", BridgeKind.UHF, draft.kind, viewModel::setKind)
                }
            }
            when (draft.kind) {
                BridgeKind.TCP_SERVER -> NewtonCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NewtonSectionLabel("TCP-сервер")
                        OutlinedTextField(
                            value = draft.tcpPort,
                            onValueChange = viewModel::setTcpPort,
                            label = { Text("Порт") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                BridgeKind.TCP_CLIENT -> NewtonCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NewtonSectionLabel("TCP-клиент")
                        OutlinedTextField(
                            value = draft.tcpHost,
                            onValueChange = viewModel::setTcpHost,
                            label = { Text("Хост") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draft.tcpPort,
                            onValueChange = viewModel::setTcpPort,
                            label = { Text("Порт") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                BridgeKind.COM -> NewtonCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NewtonSectionLabel("COM-порт")
                        Row {
                            ComOption(1, draft.comIndex, viewModel::setComIndex)
                            ComOption(2, draft.comIndex, viewModel::setComIndex)
                        }
                        OutlinedTextField(
                            value = draft.baud,
                            onValueChange = viewModel::setBaud,
                            label = { Text("Скорость, бод") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                BridgeKind.UHF -> NewtonCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        NewtonSectionLabel("УКВ-радио")
                        OutlinedTextField(
                            value = draft.uhfFreq,
                            onValueChange = viewModel::setUhfFreq,
                            label = { Text("Частота, МГц") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draft.uhfProtocol,
                            onValueChange = viewModel::setUhfProtocol,
                            label = { Text("Протокол (trimtalk/trimmk3/transeot/mac/tt450s/transparent/south/satel/lora)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draft.baud,
                            onValueChange = viewModel::setBaud,
                            label = { Text("Скорость, бод") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = draft.uhfPower,
                            onValueChange = viewModel::setUhfPower,
                            label = { Text("Мощность (high/low/0.5w/1w/2w/medium)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                BridgeKind.NONE -> Unit
            }
        }
    }
}

@Composable
private fun KindRow(label: String, value: BridgeKind, current: BridgeKind, onClick: (BridgeKind) -> Unit) {
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
private fun ComOption(index: Int, current: Int, onClick: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .selectable(selected = current == index, onClick = { onClick(index) })
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = current == index, onClick = { onClick(index) })
        Text("COM$index", style = MaterialTheme.typography.bodyMedium)
    }
}
