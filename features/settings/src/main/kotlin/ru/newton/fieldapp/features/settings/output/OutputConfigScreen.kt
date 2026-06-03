package ru.newton.fieldapp.features.settings.output

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.domain.receiver.OutputMessageConfig
import ru.newton.fieldapp.domain.receiver.OutputStreamConfig
import ru.newton.fieldapp.domain.receiver.StreamTarget

@Composable
fun OutputConfigScreen(
    onBack: () -> Unit,
    onNavigateToApply: () -> Unit = {},
    viewModel: OutputConfigViewModel = hiltViewModel(),
    pendingViewModel: ru.newton.fieldapp.features.settings.nav.PendingChangesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingCount by pendingViewModel.pendingCount.collectAsStateWithLifecycle()
    var addingMessage by remember { mutableStateOf(false) }
    var addingStream by remember { mutableStateOf(false) }

    OutputConfigContent(
        state = state,
        pendingCount = pendingCount,
        onBack = onBack,
        onNavigateToApply = onNavigateToApply,
        onApplyDefaults = viewModel::applyMvpDefaults,
        onClearAll = viewModel::clearAll,
        onAddMessageClick = { addingMessage = true },
        onAddStreamClick = { addingStream = true },
        onRemoveMessage = viewModel::removeMessage,
        onRemoveStream = viewModel::removeStream,
    )

    if (addingMessage) {
        AddMessageDialog(
            onAdd = {
                viewModel.addMessage(it)
                addingMessage = false
            },
            onDismiss = { addingMessage = false },
        )
    }
    if (addingStream) {
        AddStreamDialog(
            onAdd = {
                viewModel.addStream(it)
                addingStream = false
            },
            onDismiss = { addingStream = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OutputConfigContent(
    state: OutputConfigState,
    pendingCount: Int,
    onBack: () -> Unit,
    onNavigateToApply: () -> Unit,
    onApplyDefaults: () -> Unit,
    onClearAll: () -> Unit,
    onAddMessageClick: () -> Unit,
    onAddStreamClick: () -> Unit,
    onRemoveMessage: (Int) -> Unit,
    onRemoveStream: (Int) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Сообщения и потоки") },
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (pendingCount > 0) {
                    ru.newton.fieldapp.core.ui.components.PendingBanner(
                        pendingCount = pendingCount,
                        onApply = onNavigateToApply,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NewtonSecondaryButton(
                        onClick = onClearAll,
                        text = "Очистить",
                        modifier = Modifier.weight(1f),
                    )
                    NewtonPrimaryButton(
                        onClick = onApplyDefaults,
                        text = "MVP-набор",
                        modifier = Modifier.weight(2f),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Изменения попадут в очередь команд приёмника. Применить можно на " +
                    "экране «Применение и диагностика».",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MessagesCard(state.messages, onAdd = onAddMessageClick, onRemove = onRemoveMessage)
            StreamsCard(state.streams, onAdd = onAddStreamClick, onRemove = onRemoveStream)
        }
    }
}

@Composable
private fun MessagesCard(messages: List<OutputMessageConfig>, onAdd: () -> Unit, onRemove: (Int) -> Unit) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                NewtonSectionLabel("Сообщения (${messages.size})")
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (messages.isEmpty()) {
                Text(
                    "Нет настроенных сообщений. Нажмите + чтобы добавить, либо «MVP-набор».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                messages.forEachIndexed { index, msg ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${msg.type} @ ${msg.rate} (${msg.format})", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Источник: ${msg.source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamsCard(streams: List<OutputStreamConfig>, onAdd: () -> Unit, onRemove: (Int) -> Unit) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                NewtonSectionLabel("Потоки (${streams.size})")
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (streams.isEmpty()) {
                Text(
                    "Нет настроенных потоков. Поток связывает источники с целью (Bluetooth / TCP).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                streams.forEachIndexed { index, stream ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stream.sources.joinToString(" │ ") + " → " + describeTarget(stream.target),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            NewtonInfoBadge(targetTypeLabel(stream.target))
                        }
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            }
        }
    }
}

private fun describeTarget(target: StreamTarget): String = when (target) {
    is StreamTarget.Bluetooth -> "Bluetooth"
    is StreamTarget.TcpClient -> "TCP ${target.host}:${target.port}"
    is StreamTarget.TcpServer -> "TCP server :${target.port}"
    is StreamTarget.GsmTcpClient -> "GSM TCP ${target.host}:${target.port}"
    is StreamTarget.Com -> "COM${target.index} @ ${target.baudrate}"
    is StreamTarget.Uhf -> "УКВ ${target.frequencyMHz} МГц / ${target.protocol}"
}

private fun targetTypeLabel(target: StreamTarget): String = when (target) {
    is StreamTarget.Bluetooth -> "BT"
    is StreamTarget.TcpClient -> "TCP-клиент"
    is StreamTarget.TcpServer -> "TCP-сервер"
    is StreamTarget.GsmTcpClient -> "GSM"
    is StreamTarget.Com -> "COM"
    is StreamTarget.Uhf -> "УКВ"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMessageDialog(onAdd: (OutputMessageConfig) -> Unit, onDismiss: () -> Unit) {
    val sources = listOf("M", "R1", "R2", "PPP", "IMU")
    val types = listOf("GPGGA", "GPRMC", "GPGSA", "GPGSV", "GPGST", "GPTRA", "GPHDT", "GPZDA")
    val rates = listOf("20HZ", "10HZ", "5HZ", "2HZ", "1HZ", "5S", "10S", "ONNEW", "ONCHANGE")
    val formats = listOf("A", "B", "AB")

    var source by remember { mutableStateOf(sources.first()) }
    var type by remember { mutableStateOf(types.first()) }
    var rate by remember { mutableStateOf("1HZ") }
    var format by remember { mutableStateOf(formats.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое сообщение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EnumDropdown("Источник", source, sources) { source = it }
                EnumDropdown("Тип", type, types) { type = it }
                EnumDropdown("Частота", rate, rates) { rate = it }
                EnumDropdown("Формат", format, formats) { format = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAdd(OutputMessageConfig(source = source, type = type, rate = rate, format = format))
            }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStreamDialog(onAdd: (OutputStreamConfig) -> Unit, onDismiss: () -> Unit) {
    val sources = listOf("M", "R1", "R2", "PPP", "IMU")
    val targets = listOf("Bluetooth", "TCP-клиент", "TCP-сервер")
    val selected = remember { mutableStateOf(sources.toSet().minus(setOf("IMU", "PPP"))) }
    var targetKind by remember { mutableStateOf(targets.first()) }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("2101") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый поток") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Источники:", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sources.forEach { src ->
                        val isSelected = src in selected.value
                        TextButton(
                            onClick = {
                                selected.value = if (isSelected) {
                                    selected.value - src
                                } else {
                                    selected.value + src
                                }
                            },
                        ) {
                            Text(
                                src,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                EnumDropdown("Цель", targetKind, targets) { targetKind = it }
                if (targetKind != "Bluetooth") {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Хост") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Порт") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val srcList = sources.filter { it in selected.value }
                if (srcList.isEmpty()) return@TextButton
                val target: StreamTarget = when (targetKind) {
                    "Bluetooth" -> StreamTarget.Bluetooth
                    "TCP-клиент" -> StreamTarget.TcpClient(host = host.trim(), port = port.toIntOrNull() ?: 2101)
                    else -> StreamTarget.TcpServer(port = port.toIntOrNull() ?: 2101)
                }
                onAdd(OutputStreamConfig(sources = srcList, target = target))
            }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnumDropdown(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onChange(opt)
                        expanded = false
                    },
                )
            }
        }
    }
}
