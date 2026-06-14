package ru.newton.fieldapp.gnss.command

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.core.bluetooth.LinkState

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
    fun `late reply from a timed-out command does not satisfy the next command`() = runTest {
        val transport = FakeSppTransport()
        val session = newSession(transport, testScheduler)
        session.start()

        // Command A gets no reply within the window and times out.
        val a = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { session.send("commandA") }
        }
        testScheduler.advanceTimeBy(CommandSession.REPLY_TIMEOUT_MS + 1)
        testScheduler.runCurrent()
        assertTrue(a.await().isFailure) { "commandA should have timed out" }

        // A's OK! now arrives late and lands in the reply channel.
        transport.emitIncoming("OK!\r\n")
        repeat(5) { yield() }

        // Command B must wait for its OWN reply — the stale OK! must be drained,
        // not consumed as B's acknowledgement (off-by-one regression guard).
        val b = async(start = CoroutineStart.UNDISPATCHED) { session.send("commandB") }
        repeat(5) { yield() }
        assertFalse(b.isCompleted) { "commandB consumed a stale reply" }
        transport.emitIncoming("OK!\r\n")
        assertEquals(OkKind.OK_QUEUED, b.await())

        session.shutdown()
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun `link leaving Connected resets a Ready session to Idle`() = runTest {
        val transport = FakeSppTransport()
        val session = newSession(transport, testScheduler)
        session.start()

        val handshake = launch { session.handshake() }
        repeat(5) { yield() }
        transport.emitIncoming("OK\r\n")
        repeat(5) { yield() }
        transport.emitIncoming("on\r\n")
        handshake.join()
        assertEquals(CommandSessionState.Ready, session.state.value)

        // BT drop / receiver power-cycle: command mode is gone on the receiver,
        // so the session must drop out of Ready to force a re-handshake on the
        // next Apply rather than firing commands into a dead command port.
        transport.setLink(LinkState.Disconnected)
        repeat(5) { yield() }
        assertEquals(CommandSessionState.Idle, session.state.value)

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
