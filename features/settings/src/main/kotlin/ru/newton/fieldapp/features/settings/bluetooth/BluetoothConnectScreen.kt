package ru.newton.fieldapp.features.settings.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.bluetooth.LinkState
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.components.NewtonStatusPill
import ru.newton.fieldapp.core.ui.components.NewtonSuccessBadge
import ru.newton.fieldapp.core.ui.components.NewtonSuccessButton
import ru.newton.fieldapp.core.ui.theme.LocalFixStatusColors

@Composable
fun BluetoothConnectScreen(
    onBack: () -> Unit = {},
    viewModel: BluetoothConnectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onPermissionResult(granted) }

    // Re-read paired devices after returning from system Bluetooth settings —
    // the user typically goes there to pair the receiver, then comes back.
    val systemSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refresh() }

    LaunchedEffect(state.needsPermission) {
        if (state.needsPermission && !state.permissionDenied) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    BluetoothConnectContent(
        state = state,
        onBack = onBack,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
        onConnect = viewModel::onConnectClicked,
        onDisconnect = viewModel::onDisconnectClicked,
        onRefresh = viewModel::refresh,
        onOpenSystemSettings = {
            systemSettingsLauncher.launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        },
        onEnableBluetooth = {
            systemSettingsLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothConnectContent(
    state: BluetoothConnectState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onEnableBluetooth: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Подключение Bluetooth") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChannelStatusCard(state)

            if (state.bluetoothMissing) {
                NewtonCard {
                    Text(
                        "Bluetooth недоступен на устройстве",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                return@Column
            }

            if (state.needsPermission) {
                NewtonCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Требуется разрешение на Bluetooth, чтобы увидеть сопряжённые устройства.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        NewtonPrimaryButton(
                            onClick = onRequestPermission,
                            text = "Запросить разрешение",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                return@Column
            }

            if (state.bluetoothDisabled) {
                NewtonCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Bluetooth выключен. Включите его, чтобы увидеть сопряжённые устройства.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        NewtonPrimaryButton(
                            onClick = onEnableBluetooth,
                            text = "Включить Bluetooth",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                return@Column
            }

            if (state.activeMac != null) {
                NewtonSecondaryButton(
                    onClick = onDisconnect,
                    text = "Отключить активный сеанс",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            DevicesSection(
                devices = state.pairedDevices,
                activeMac = state.activeMac,
                onRefresh = onRefresh,
                onConnect = onConnect,
                onOpenSystemSettings = onOpenSystemSettings,
            )
        }
    }
}

@Composable
private fun ChannelStatusCard(state: BluetoothConnectState) {
    val fixColors = LocalFixStatusColors.current
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NewtonSectionLabel("Канал SPP")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Bluetooth", style = MaterialTheme.typography.titleMedium)
                when (val link = state.link) {
                    is LinkState.Disconnected -> NewtonStatusPill(
                        text = "не подключён",
                        background = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    is LinkState.Connecting -> NewtonStatusPill(
                        text = "подключение… попытка ${link.attempt}",
                        background = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                    is LinkState.Connected -> NewtonSuccessBadge(text = link.deviceName)
                    is LinkState.Error -> NewtonStatusPill(
                        text = link.message.take(40),
                        background = fixColors.noFix.copy(alpha = 0.15f),
                        contentColor = fixColors.noFix,
                    )
                }
            }
        }
    }
}

@Composable
private fun DevicesSection(
    devices: List<PairedDevice>,
    activeMac: String?,
    onRefresh: () -> Unit,
    onConnect: (String) -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                NewtonSectionLabel("Сопряжённые устройства")
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (devices.isEmpty()) {
                Text(
                    "Список пуст. Сопрягите приёмник «Ньютон» в системных " +
                        "настройках Bluetooth (Android не позволяет приложению " +
                        "делать это самому), затем вернитесь сюда.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NewtonPrimaryButton(
                    onClick = onOpenSystemSettings,
                    text = "Открыть настройки Bluetooth",
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices, key = { it.mac }) { device ->
                        DeviceRow(
                            name = device.name,
                            mac = device.mac,
                            isActive = device.mac == activeMac,
                            onConnect = { onConnect(device.mac) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    name: String,
    mac: String,
    isActive: Boolean,
    onConnect: () -> Unit,
) {
    NewtonCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    mac,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isActive) {
                NewtonInfoBadge("Активно")
            } else {
                NewtonSuccessButton(
                    onClick = onConnect,
                    text = "Подключить",
                )
            }
        }
    }
}
