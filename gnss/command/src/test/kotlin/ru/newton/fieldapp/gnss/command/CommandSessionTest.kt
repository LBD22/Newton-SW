package ru.newton.fieldapp.gnss.command

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandSessionTest {
    private fun newSession(transport: FakeSppTransport, scheduler: kotlinx.coroutines.test.TestCoroutineScheduler) =
        CommandSession(transport, NoopAppLog(), UnconfinedTestDispatcher(scheduler))

    @Test
    fun `handshake when receiver already in command mode skips set-mode-on`() = runTest {
        val transport = FakeSppTransport()
        val session = newSession(transport, testScheduler)
        session.start()

        val handshakeJob = launch { session.handshake() }
        repeat(5) { yield() }
        transport.emitIncoming("OK\r\n")
        repeat(5) { yield() }
        transport.emitIncoming("on\r\n")
        handshakeJob.join()

        assertEquals(CommandSessionState.Ready, session.state.value)
        assertEquals(listOf("AT", "get command mode"), transport.writes)
        session.shutdown()
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun `handshake when receiver in mode off issues set-mode-on`() = runTest {
        val transport = FakeSppTransport()
        val session = newSession(transport, testScheduler)
        session.start()

        val job = launch { session.handshake() }
        repeat(5) { yield() }
        transport.emitIncoming("OK\r\n")
        repeat(5) { yield() }
        transport.emitIncoming("off\r\n")
        repeat(5) { yield() }
        transport.emitIncoming("OK+\r\n")
        job.join()

        assertEquals(CommandSessionState.Ready, session.state.value)
        assertEquals(listOf("AT", "get command mode", "set command mode on"), transport.writes)
        session.shutdown()
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun `send returns OK_QUEUED for arbitrary command`() = runTest {
        val transport = FakeSppTransport()
        val session = newSession(transport, testScheduler)
        session.start()

        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            session.send(NewtonCommandBuilder.surveySetMask(10))
        }
        repeat(5) { yield() }
        transport.emitIncoming("OK!\r\n")
        val ok = deferred.await()

        assertEquals(OkKind.OK_QUEUED, ok)
        assertEquals(listOf("survey set mask 10"), transport.writes)
        session.shutdown()
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun `four OK variants are distinguished`() = runTest {
        val transport = FakeSppTransport()
        val session = newSession(transport, testScheduler)
        session.start()

        val expected = listOf(
            OkKind.OK_HANDSHAKE to "OK\r\n",
            OkKind.OK_MODE_ON to "OK+\r\n",
            OkKind.OK_MODE_OFF to "OK-\r\n",
            OkKind.OK_QUEUED to "OK!\r\n",
        )
        for ((kind, payload) in expected) {
            val deferred = async(start = CoroutineStart.UNDISPATCHED) { session.send("dummy") }
            repeat(5) { yield() }
            transport.emitIncoming(payload)
            assertEquals(kind, deferred.await())
        }
        session.shutdown()
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun `info line preceding OK is logged but does not satisfy send`() = runTest {
        val transport = FakeSppTransport()
        val session = newSession(transport, testScheduler)
        session.start()

        val deferred = async(start = CoroutineStart.UNDISPATCHED) { session.send("system list") }
        repeat(5) { yield() }
        transport.emitIncoming("queue empty\r\n") // info line
        repeat(5) { yield() }
        assertTrue(!deferred.isCompleted)
        transport.emitIncoming("OK!\r\n")
        assertEquals(OkKind.OK_QUEUED, deferred.await())
        session.shutdown()
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun `disconnect rejects handshake`() = runTest {
        val transport = FakeSppTransport(initialLink = ru.newton.fieldapp.core.bluetooth.LinkState.Disconnected)
        val session = newSession(transport, testScheduler)
        session.start()

        val thrown = runCatching { session.handshake() }.exceptionOrNull()
        assertTrue(thrown is IllegalStateException) { "expected IllegalStateException, got $thrown" }
        session.shutdown()
        testScheduler.advanceUntilIdle()
    }
}
