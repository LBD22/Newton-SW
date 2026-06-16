package ru.newton.fieldapp.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.SystemClock
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

    // Serialises every outbound write on the one shared socket. RTCM chunks from
    // NTRIP (@CommandSpp) and Newton text commands reach write() concurrently;
    // without this lock the RFCOMM stack can splice an `AT\r\n` into the middle
    // of an RTCM frame — corrupting both the correction and the command. See
    // docs/protocol-newton.md § Bluetooth channel (one socket).
    private val writeMutex = Mutex()

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
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                active.outputStream.write(bytes)
                active.outputStream.flush()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectLoop(deviceAddress: String) {
        var attempt = 1
        while (scope.isActive && requestedAddress == deviceAddress) {
            _linkState.value = LinkState.Connecting(attempt)
            val opened = try {
                openSocket(deviceAddress)
            } catch (se: SecurityException) {
                // BLUETOOTH_CONNECT was revoked mid-run (or "Nearby devices"
                // auto-reset). Retrying can never succeed without the user
                // re-granting, so stop the loop instead of spinning the radio
                // forever at the 30 s ceiling.
                log.bt("connect aborted: Bluetooth permission missing", se)
                _linkState.value = LinkState.Error(
                    "Нет разрешения Bluetooth — выдайте доступ к устройствам рядом",
                    se,
                )
                return
            } catch (t: Throwable) {
                log.bt("connect attempt $attempt failed: ${t.message}", t)
                _linkState.value = LinkState.Error(t.message ?: "Open failed", t)
                delay(backoffMs(attempt))
                attempt = (attempt + 1).coerceAtMost(MAX_BACKOFF_ATTEMPTS)
                continue
            }
            socket = opened
            _linkState.value = LinkState.Connected(
                deviceName = opened.remoteDevice?.name ?: deviceAddress,
            )
            log.bt("connected to $deviceAddress")
            val connectedAtMs = SystemClock.elapsedRealtime()
            runReadLoop(opened)

            // Read loop returned → either we got cancelled (disconnect) or the
            // socket faulted. If the user still wants this address, loop back.
            socketMutex.withLock { closeSocketLocked() }
            if (requestedAddress != deviceAddress) return
            // Only reset backoff if the link actually stayed up. A socket that
            // connects then dies within STABLE_CONNECTION_MS — receiver mid-boot
            // after `config reset`, marginal range — must keep escalating instead
            // of churning connect/fail every ~1 s (radio drain, peer blacklist).
            val stayedUp = SystemClock.elapsedRealtime() - connectedAtMs >= STABLE_CONNECTION_MS
            attempt = if (stayedUp) 1 else (attempt + 1).coerceAtMost(MAX_BACKOFF_ATTEMPTS)
            _linkState.value = LinkState.Connecting(attempt)
            delay(backoffMs(attempt))
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
            // Only surface an error if this address is still wanted. A deliberate
            // disconnect() nulls requestedAddress and closes the socket, which
            // also throws IOException here — we must not flap the UI to Error
            // after a clean user-initiated disconnect.
            if (requestedAddress != null) {
                _linkState.value = LinkState.Error(e.message ?: "Read failed", e)
            }
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

        // Minimum time a socket must stay up before we treat the next drop as a
        // "fresh" failure and reset backoff. Below this it's a flapping link and
        // we keep escalating.
        private const val STABLE_CONNECTION_MS = 10_000L

        private fun backoffMs(attempt: Int): Long {
            val base = 1_000L shl (attempt - 1).coerceAtLeast(0)
            return base.coerceAtMost(30_000L)
        }
    }
}
