package ru.newton.fieldapp.gnss.command

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.newton.fieldapp.core.bluetooth.LinkState
import ru.newton.fieldapp.core.bluetooth.SppTransport

/**
 * In-memory fake transport for command-port unit tests. Mimics a connected
 * SPP channel without touching Android Bluetooth APIs — the test pushes bytes
 * via [emitIncoming] to simulate receiver replies, and inspects [writes] to
 * see what the session sent.
 */
internal class FakeSppTransport(
    initialLink: LinkState = LinkState.Connected("FakeNewton"),
) : SppTransport {
    private val _linkState = MutableStateFlow(initialLink)
    override val linkState: StateFlow<LinkState> = _linkState.asStateFlow()

    private val _incoming = MutableSharedFlow<ByteArray>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )
    override val incoming: Flow<ByteArray> = _incoming.asSharedFlow()

    val writes: MutableList<String> = mutableListOf()

    override suspend fun connect(deviceAddress: String) {
        _linkState.value = LinkState.Connected(deviceAddress)
    }

    override suspend fun disconnect() {
        _linkState.value = LinkState.Disconnected
    }

    override suspend fun write(bytes: ByteArray) {
        writes += String(bytes, Charsets.US_ASCII).trimEnd('\r', '\n')
    }

    suspend fun emitIncoming(text: String) {
        _incoming.emit(text.toByteArray(Charsets.US_ASCII))
    }

    fun setLink(state: LinkState) {
        _linkState.value = state
    }
}
