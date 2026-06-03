package ru.newton.fieldapp.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.newton.fieldapp.core.logging.AppLog
import java.io.IOException
import java.util.UUID

/**
 * Concrete [SppTransport] backed by Android Bluetooth Classic SPP.
 *
 * The Newton receiver advertises **one** RFCOMM channel under the standard SPP
 * UUID — NMEA/RTCM downstream and the text command pipe are multiplexed by the
 * firmware over the same socket. There is exactly one instance in the Hilt
 * graph; both `@DataSpp` and `@CommandSpp` qualifiers point at it.
 *
 * Reconnect strategy (BT-003):
 *  - socket exception during read → enter `Connecting(attempt=N)`
 *  - sleep `min(1s · 2^(N-1), 30s)` (exponential backoff with ceiling)
 *  - retry until [disconnect] is called
 *
 * Threading: read loop runs on `Dispatchers.IO`; socket open/close serialised
 * by [socketMutex] to keep `connect`/`disconnect` race-free.
 */
class BluetoothSppTransport(
    @ApplicationContext private val context: Context,
    private val log: AppLog,
) : SppTransport {
    private val parentJob = SupervisorJob()
    private val scope = CoroutineScope(parentJob + Dispatchers.IO)
    private val socketMutex = Mutex()

    @Volatile private var socket: BluetoothSocket? = null

    @Volatile private var requestedAddress: String? = null

    @Volatile private var readJob: Job? = null

    private val _linkState = MutableStateFlow<LinkState>(LinkState.Disconnected)
    override val linkState: StateFlow<LinkState> = _linkState.asStateFlow()

    private val _incoming = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val incoming: Flow<ByteArray> = _incoming.asSharedFlow()

    @SuppressLint("MissingPermission")
    override suspend fun connect(deviceAddress: String) {
        socketMutex.withLock {
            // Idempotent on repeated calls for the same address — multiple
            // injection sites may invoke connect() symmetrically without
            // coordinating, and we must not start a second loop.
            if (requestedAddress == deviceAddress && readJob?.isActive == true) return
            requestedAddress = deviceAddress
            closeSocketLocked()
            readJob?.cancel()
            readJob = scope.launch { connectLoop(deviceAddress) }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() {
        socketMutex.withLock {
            requestedAddress = null
            readJob?.cancel()
            readJob = null
            closeSocketLocked()
            _linkState.value = LinkState.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun write(bytes: ByteArray) {
        val active = socket
        check(_linkState.value is LinkState.Connected && active != null) {
            "write() called while link is ${_linkState.value}"
        }
        withContext(Dispatchers.IO) {
            active.outputStream.write(bytes)
            active.outputStream.flush()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectLoop(deviceAddress: String) {
        var attempt = 1
        while (scope.isActive && requestedAddress == deviceAddress) {
            _linkState.value = LinkState.Connecting(attempt)
            val opened = runCatching { openSocket(deviceAddress) }.getOrElse { t ->
                log.bt("connect attempt $attempt failed: ${t.message}", t)
                _linkState.value = LinkState.Error(t.message ?: "Open failed", t)
                delay(backoffMs(attempt))
                attempt = (attempt + 1).coerceAtMost(MAX_BACKOFF_ATTEMPTS)
                continue
            }
            attempt = 1
            socket = opened
            _linkState.value = LinkState.Connected(
                deviceName = opened.remoteDevice?.name ?: deviceAddress,
                rssi = null,
            )
            log.bt("connected to $deviceAddress")
            runReadLoop(opened)

            // Read loop returned → either we got cancelled (disconnect) or the
            // socket faulted. If the user still wants this address, loop back.
            socketMutex.withLock { closeSocketLocked() }
            if (requestedAddress != deviceAddress) return
            _linkState.value = LinkState.Connecting(1)
            delay(backoffMs(1))
        }
    }

    @SuppressLint("MissingPermission")
    private fun openSocket(deviceAddress: String): BluetoothSocket {
        val adapter = adapter() ?: error("Bluetooth not available on this device")
        val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            ?: error("Device $deviceAddress not paired")
        val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
        // Cancel discovery first — Android's stack drops sockets opened mid-scan.
        runCatching { adapter.cancelDiscovery() }
        socket.connect()
        return socket
    }

    private suspend fun runReadLoop(active: BluetoothSocket) {
        val buffer = ByteArray(READ_CHUNK)
        try {
            val input = active.inputStream
            while (scope.isActive) {
                val read = input.read(buffer)
                if (read <= 0) break
                _incoming.emit(buffer.copyOf(read))
            }
        } catch (e: IOException) {
            log.bt("read loop interrupted: ${e.message}", e)
            _linkState.value = LinkState.Error(e.message ?: "Read failed", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeSocketLocked() {
        runCatching { socket?.close() }
        socket = null
    }

    private fun adapter(): BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    companion object {
        // Standard RFCOMM SPP service UUID. The receiver firmware advertises a
        // single SDP record under this UUID — see `docs/protocol-newton.md`
        // § Bluetooth channel for why we open exactly one socket.
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val READ_CHUNK = 1024
        private const val MAX_BACKOFF_ATTEMPTS = 6 // 1,2,4,8,16,30s ceiling

        private fun backoffMs(attempt: Int): Long {
            val base = 1_000L shl (attempt - 1).coerceAtLeast(0)
            return base.coerceAtMost(30_000L)
        }
    }
}