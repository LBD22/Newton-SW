package ru.newton.fieldapp.features.settings.bluetooth

import ru.newton.fieldapp.core.bluetooth.LinkState

data class BluetoothConnectState(
    val pairedDevices: List<PairedDevice> = emptyList(),
    val link: LinkState = LinkState.Disconnected,
    val activeMac: String? = null,
    val needsPermission: Boolean = false,
    val permissionDenied: Boolean = false,
    val bluetoothMissing: Boolean = false,
    /** True when adapter exists but is currently switched off — UI should
     *  prompt the user to enable BT before bothering to refresh. */
    val bluetoothDisabled: Boolean = false,
)

data class PairedDevice(
    val mac: String,
    val name: String,
)
