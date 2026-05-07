@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ru.newton.fieldapp.data.staticobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.newton.fieldapp.core.bluetooth.LinkState
import ru.newton.fieldapp.core.bluetooth.SppTransport
import java.io.File

class StaticNmeaRecorderTest {
    @Test
    fun `start creates a new file under the root and transitions to Active`(
        @TempDir tmp: File,
    ) = runTest {
        val transport = FakeTransport()
        val recorder = StaticNmeaRecorder(tmp, transport, CoroutineScope(StandardTestDispatcher(testScheduler)))

        recorder.start()
        advanceUntilIdle()

        val state = recorder.state.value
        assertTrue(state is StaticRecorderState.Active, "expected Active, was $state")
        val active = state as StaticRecorderState.Active
        assertTrue(active.recording.file.exists())
        assertEquals(tmp.absolutePath, active.recording.file.parentFile?.absolutePath)
        assertTrue(active.recording.file.name.startsWith("static_") && active.recording.file.name.endsWith(".nmea"))
        recorder.stop()
    }

    @Test
    fun `second start is a no-op while a recording is active`(
        @TempDir tmp: File,
    ) = runTest {
        val transport = FakeTransport()
        val recorder = StaticNmeaRecorder(tmp, transport, CoroutineScope(StandardTestDispatcher(testScheduler)))

        recorder.start()
        advanceUntilIdle()
        val firstState = recorder.state.value as StaticRecorderState.Active
        recorder.start()
        advanceUntilIdle()
        val secondState = recorder.state.value as StaticRecorderState.Active

        assertSame(firstState.recording.file, secondState.recording.file)
        recorder.stop()
    }

    @Test
    fun `incoming chunks accumulate bytesWritten and end up on disk after stop`(
        @TempDir tmp: File,
    ) = runTest {
        val transport = FakeTransport()
        val recorder = StaticNmeaRecorder(tmp, transport, CoroutineScope(StandardTestDispatcher(testScheduler)))

        recorder.start()
        advanceUntilIdle()
        transport.emit("\$GPGGA,123,...\r\n".toByteArray())
        transport.emit(byteArrayOf(0x01, 0x02, 0x03))
        advanceUntilIdle()
        val active = recorder.state.value as StaticRecorderState.Active
        val expectedBytes = "\$GPGGA,123,...\r\n".toByteArray().size + 3
        assertEquals(expectedBytes.toLong(), active.recording.bytesWritten)

        recorder.stop()

        // After stop the writer is flushed and the file should hold the bytes.
        assertEquals(expectedBytes, active.recording.file.readBytes().size)
        assertTrue(recorder.state.value is StaticRecorderState.Idle)
    }

    @Test
    fun `stop without start is safe`(
        @TempDir tmp: File,
    ) = runTest {
        val transport = FakeTransport()
        val recorder = StaticNmeaRecorder(tmp, transport, CoroutineScope(StandardTestDispatcher(testScheduler)))

        recorder.stop()

        assertTrue(recorder.state.value is StaticRecorderState.Idle)
    }

    @Test
    fun `delete refuses to remove the file currently being written to`(
        @TempDir tmp: File,
    ) = runTest {
        val transport = FakeTransport()
        val recorder = StaticNmeaRecorder(tmp, transport, CoroutineScope(StandardTestDispatcher(testScheduler)))

        recorder.start()
        advanceUntilIdle()
        val active = recorder.state.value as StaticRecorderState.Active
        recorder.delete(active.recording.file)

        assertTrue(active.recording.file.exists(), "active recording file must not be deletable")
        recorder.stop()
    }

    @Test
    fun `listPastSessions returns nmea files newest first`(
        @TempDir tmp: File,
    ) {
        val older = File(tmp, "static_20250101_000000.nmea").apply {
            writeText("a")
            setLastModified(1_000L)
        }
        val newer = File(tmp, "static_20250606_120000.nmea").apply {
            writeText("b")
            setLastModified(2_000L)
        }
        File(tmp, "ignore.txt").writeText("nope")

        val recorder = StaticNmeaRecorder(tmp, FakeTransport(), CoroutineScope(Dispatchers.Unconfined))

        val sessions = recorder.listPastSessions()
        assertEquals(listOf(newer, older), sessions)
    }

    @Test
    fun `listPastSessions returns empty when root does not exist`(
        @TempDir tmp: File,
    ) {
        val missingRoot = File(tmp, "does-not-exist")
        val recorder = StaticNmeaRecorder(missingRoot, FakeTransport(), CoroutineScope(Dispatchers.Unconfined))

        assertTrue(recorder.listPastSessions().isEmpty())
        assertFalse(missingRoot.exists())
    }
}

private class FakeTransport : SppTransport {
    override val linkState: StateFlow<LinkState> = MutableStateFlow(LinkState.Disconnected)
    private val flow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incoming: Flow<ByteArray> = flow

    suspend fun emit(bytes: ByteArray) { flow.emit(bytes) }

    override suspend fun connect(deviceAddress: String) {}
    override suspend fun disconnect() {}
    override suspend fun write(bytes: ByteArray) {}
}
