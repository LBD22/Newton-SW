package ru.newton.fieldapp.features.settings.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.bluetooth.DataSpp
import ru.newton.fieldapp.core.bluetooth.SppTransport
import ru.newton.fieldapp.data.preferences.LastDeviceStore
import ru.newton.fieldapp.gnss.data.GnssForegroundService
import javax.inject.Inject

@HiltViewModel
class BluetoothConnectViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @DataSpp spp: SppTransport,
        private val lastDevice: LastDeviceStore,
    ) : ViewModel() {
        private val _state = MutableStateFlow(BluetoothConnectState())
        val state: StateFlow<BluetoothConnectState> = _state.asStateFlow()

        init {
            spp.linkState
                .onEach { link -> _state.value = _state.value.copy(link = link) }
                .launchIn(viewModelScope)
            refresh()
        }

        fun onPermissionResult(granted: Boolean) {
            _state.value = _state.value.copy(
                needsPermission = !granted,
                permissionDenied = !granted,
            )
            if (granted) refresh()
        }

        fun onConnectClicked(mac: String) {
            _state.value = _state.value.copy(activeMac = mac)
            ContextCompat.startForegroundService(context, GnssForegroundService.startIntent(context, mac))
            // Remember the MAC so MainActivity can auto-restart the link on cold boot.
            viewModelScope.launch { lastDevice.set(mac) }
        }

        fun onDisconnectClicked() {
            _state.value = _state.value.copy(activeMac = null)
            context.startService(GnssForegroundService.stopIntent(context))
            // Forget the MAC so we don't auto-reconnect to a device the user explicitly left.
            viewModelScope.launch { lastDevice.clear() }
        }

        fun refresh() {
            if (!hasConnectPermission()) {
                _state.value = _state.value.copy(needsPermission = true)
                return
            }
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter
            if (adapter == null) {
                _state.value = _state.value.copy(bluetoothMissing = true)
                return
            }
            if (!adapter.isEnabled) {
                _state.value = _state.value.copy(
                    needsPermission = false,
                    permissionDenied = false,
                    bluetoothMissing = false,
                    bluetoothDisabled = true,
                    pairedDevices = emptyList(),
                )
                return
            }
            _state.value = _state.value.copy(
                needsPermission = false,
                permissionDenied = false,
                bluetoothMissing = false,
                bluetoothDisabled = false,
                pairedDevices = readPairedDevices(adapter),
            )
        }

        private fun hasConnectPermission(): Boolean = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED

        @SuppressLint("MissingPermission")
        private fun readPairedDevices(adapter: android.bluetooth.BluetoothAdapter): List<PairedDevice> =
            adapter.bondedDevices.orEmpty().map { device ->
                PairedDevice(mac = device.address, name = device.name ?: device.address)
            }.sortedBy { it.name.lowercase() }
    }
