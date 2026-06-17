package ru.newton.fieldapp.features.settings.correction

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.gnss.command.UhfProtocol
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
        onSelectProfileViaGsm = viewModel::onSelectProfileViaGsm,
        onSelectProfileViaEthernet = viewModel::onSelectProfileViaEthernet,
        onSelectUhf = viewModel::onSelectUhf,
        onSelectCom = viewModel::onSelectCom,
        onSelectTcpClient = viewModel::onSelectTcpClient,
        onSelectTcpServer = viewModel::onSelectTcpServer,
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
    onSelectProfileViaGsm: (Long) -> Unit,
    onSelectProfileViaEthernet: (Long) -> Unit,
    onSelectUhf: (Double, String, Int) -> Unit,
    onSelectCom: (Int, Int) -> Unit,
    onSelectTcpClient: (String, Int) -> Unit,
    onSelectTcpServer: (Int) -> Unit,
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
        // verticalScroll: NTRIP + УКВ + COM/TCP cards and the «Управление
        // профилями…» button together exceed one screen, so the bottom (UHF
        // baud-rate fields and the profile-management entry point) was
        // unreachable — looked like "add NTRIP profile disappeared".
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NtripStatusCard(state.ntripState)

            if (state.inputApplyPending) {
                ApplyPendingCard()
            }

            if (state.gsmNtripApplyPending) {
                GsmNtripPendingCard(gsmModemEnabled = state.gsmModemEnabled)
            }

            Text("NTRIP профили", style = MaterialTheme.typography.titleMedium)
            if (state.profiles.isEmpty()) {
                Text(
                    "Нет сохранённых профилей. Создайте профиль в разделе «NTRIP профили».",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            } else {
                // Plain Column (not LazyColumn) because the whole screen is now
                // inside a verticalScroll — a lazy list there throws on the
                // infinite-height constraint. The profile count is tiny.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.profiles.forEach { profile ->
                        ProfileCard(
                            profile = profile,
                            isActiveController = profile.id == state.activeProfileId,
                            isActiveGsm = profile.id == state.gsmNtripActiveProfileId,
                            isActiveEthernet = profile.id == state.ethernetNtripActiveProfileId,
                            onSelectController = { onSelectProfile(profile.id) },
                            onSelectGsm = { onSelectProfileViaGsm(profile.id) },
                            onSelectEthernet = { onSelectProfileViaEthernet(profile.id) },
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

            Text("Радио (УКВ)", style = MaterialTheme.typography.titleMedium)
            UhfSourceCard(activeSummary = state.uhfActiveSummary, onSelectUhf = onSelectUhf)

            Text("COM / TCP", style = MaterialTheme.typography.titleMedium)
            ComSourceCard(activeSummary = state.comActiveSummary, onSelectCom = onSelectCom)
            TcpSourceCard(
                activeSummary = state.tcpActiveSummary,
                onSelectTcpClient = onSelectTcpClient,
                onSelectTcpServer = onSelectTcpServer,
            )

            NewtonSecondaryButton(
                onClick = onManageProfiles,
                text = "Управление профилями…",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ApplyPendingCard() {
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Настройки не применены",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                "Приёмник ещё не переведён на приём RTCM по Bluetooth. Поправки " +
                    "передаются, но приёмник их игнорирует до нажатия «Применить» " +
                    "(system save) на экране диагностики.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
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
private fun GsmNtripPendingCard(gsmModemEnabled: Boolean) {
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "NTRIP через GSM приёмника — в очереди",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            val detail = if (gsmModemEnabled) {
                "Приёмник сам подключится к кастеру через свой GSM-модем после " +
                    "нажатия «Применить» (system save) на экране диагностики. Интернет " +
                    "телефона не нужен."
            } else {
                "Сначала включите GSM-модем и задайте APN на экране «GSM модем», затем " +
                    "нажмите «Применить» — обе настройки уйдут в приёмник вместе. Без APN " +
                    "модем не выйдет в сеть."
            }
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = if (gsmModemEnabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}

@Composable
private fun UhfSourceCard(
    activeSummary: String?,
    onSelectUhf: (Double, String, Int) -> Unit,
) {
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (activeSummary != null) {
                Text(
                    "В очереди: $activeSummary",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Встроенное радио приёмника. Нажмите «Применить» на экране " +
                        "диагностики, чтобы источник вступил в силу. Можно поменять частоту ниже.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            } else {
                Text(
                    "Приём поправок встроенным УКВ-радио приёмника — интернет и " +
                        "Bluetooth не нужны. Задайте частоту, протокол и скорость базы.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }

            var freqText by remember { mutableStateOf("438.0") }
            var protocol by remember { mutableStateOf(UhfProtocol.SATEL) }
            var baud by remember { mutableStateOf(9600) }

            OutlinedTextField(
                value = freqText,
                onValueChange = { freqText = it },
                label = { Text("Частота, МГц") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Протокол", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                UhfProtocol.entries.forEach { p ->
                    FilterChip(
                        selected = protocol == p,
                        onClick = { protocol = p },
                        label = { Text(p.code) },
                    )
                }
            }

            Text("Скорость, бод", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(4800, 9600, 19200, 38400, 115200).forEach { b ->
                    FilterChip(
                        selected = baud == b,
                        onClick = { baud = b },
                        label = { Text(b.toString()) },
                    )
                }
            }

            NewtonPrimaryButton(
                onClick = {
                    val freq = freqText.trim().replace(',', '.').toDoubleOrNull()
                    if (freq != null) onSelectUhf(freq, protocol.code, baud)
                },
                text = "Выбрать УКВ",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProfileCard(
    profile: NtripProfile,
    isActiveController: Boolean,
    isActiveGsm: Boolean,
    isActiveEthernet: Boolean,
    onSelectController: () -> Unit,
    onSelectGsm: () -> Unit,
    onSelectEthernet: () -> Unit,
) {
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(profile.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${profile.host}:${profile.port}/${profile.mountpoint}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            val activeLabel = when {
                isActiveGsm -> "Активен: NTRIP через GSM приёмника"
                isActiveEthernet -> "Активен: NTRIP по Ethernet приёмника"
                isActiveController -> "Активен: через контроллер (Bluetooth)"
                else -> null
            }
            if (activeLabel != null) {
                Text(
                    activeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonPrimaryButton(
                        onClick = onSelectGsm,
                        text = "GSM приёмника",
                        modifier = Modifier.weight(1f),
                    )
                    NewtonSecondaryButton(
                        onClick = onSelectController,
                        text = "Через контроллер",
                        modifier = Modifier.weight(1f),
                    )
                }
                NewtonSecondaryButton(
                    onClick = onSelectEthernet,
                    text = "Ethernet приёмника",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ComSourceCard(
    activeSummary: String?,
    onSelectCom: (Int, Int) -> Unit,
) {
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (activeSummary != null) {
                Text(
                    "В очереди: $activeSummary",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    "Поправки с внешнего устройства по последовательному порту приёмника.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
            var port by remember { mutableStateOf(1) }
            var baud by remember { mutableStateOf(115200) }
            Text("Порт", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1, 2).forEach { p ->
                    FilterChip(
                        selected = port == p,
                        onClick = { port = p },
                        label = { Text("COM$p") },
                    )
                }
            }
            Text("Скорость, бод", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(9600, 19200, 38400, 57600, 115200).forEach { b ->
                    FilterChip(
                        selected = baud == b,
                        onClick = { baud = b },
                        label = { Text(b.toString()) },
                    )
                }
            }
            NewtonPrimaryButton(
                onClick = { onSelectCom(port, baud) },
                text = "Выбрать COM",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TcpSourceCard(
    activeSummary: String?,
    onSelectTcpClient: (String, Int) -> Unit,
    onSelectTcpServer: (Int) -> Unit,
) {
    NewtonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (activeSummary != null) {
                Text(
                    "В очереди: $activeSummary",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    "Поправки по TCP сетью приёмника: клиент (подключиться к хосту) " +
                        "или сервер (слушать порт).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                )
            }
            var serverMode by remember { mutableStateOf(false) }
            var host by remember { mutableStateOf("") }
            var portText by remember { mutableStateOf("8000") }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !serverMode,
                    onClick = { serverMode = false },
                    label = { Text("Клиент") },
                )
                FilterChip(
                    selected = serverMode,
                    onClick = { serverMode = true },
                    label = { Text("Сервер") },
                )
            }
            if (!serverMode) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Хост") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it },
                label = { Text("Порт") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            NewtonPrimaryButton(
                onClick = {
                    val p = portText.trim().toIntOrNull() ?: return@NewtonPrimaryButton
                    if (serverMode) {
                        onSelectTcpServer(p)
                    } else if (host.isNotBlank()) {
                        onSelectTcpClient(host.trim(), p)
                    }
                },
                text = if (serverMode) "Выбрать TCP-сервер" else "Выбрать TCP-клиент",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
