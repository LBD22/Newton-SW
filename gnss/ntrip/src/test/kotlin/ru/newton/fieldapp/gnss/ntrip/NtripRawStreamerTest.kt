package ru.newton.fieldapp.gnss.ntrip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.core.logging.AppLog
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Socket-level coverage for a key Баг-002 fix: a wrong mountpoint makes the
 * caster answer with its source table ("SOURCETABLE 200 OK" / Content-Type
 * gnss/sourcetable) instead of an RTCM stream. The old `contains("200")` check
 * accepted that and pumped the table into the receiver as if it were RTCM. The
 * streamer must now abort with a terminal Failed state and emit nothing.
 */
class NtripRawStreamerTest {
    private val noopLog = object : AppLog {
        override fun bt(message: String, throwable: Throwable?) = Unit
        override fun gnss(message: String, throwable: Throwable?) = Unit
        override fun cmd(message: String, throwable: Throwable?) = Unit
        override fun ntrip(message: String, throwable: Throwable?) = Unit
        override fun ui(message: String, throwable: Throwable?) = Unit
        override fun general(message: String, throwable: Throwable?) = Unit
        override suspend fun exportArchive(daysBack: Int): String = ""
    }

    @Test
    fun `wrong mountpoint (SOURCETABLE response) aborts without streaming`() {
        val server = ServerSocket(0)
        val accepting = thread {
            server.accept().use { client ->
                drainRequest(client)
                client.getOutputStream().apply {
                    write("SOURCETABLE 200 OK\r\nContent-Type: gnss/sourcetable\r\n\r\nSTR;A;;;;;\r\n".toByteArray())
                    flush()
                }
            }
        }

        val profile = NtripProfile(
            id = 1,
            name = "test",
            host = "127.0.0.1",
            port = server.localPort,
            mountpoint = "WRONG",
            login = "",
            password = "",
            sendNmea = true,
            useTls = false,
        )
        val streamer = NtripRawStreamer(
            ioDispatcher = Dispatchers.IO,
            log = noopLog,
            gpggaProvider = { null },
        )
        val chunks = runBlocking {
            withTimeout(5_000) { streamer.streamMountpoint(profile).toList() }
        }

        accepting.join(2_000)
        server.close()

        assertTrue(chunks.isEmpty(), "no RTCM should be emitted for a source-table response")
        assertTrue(streamer.state.value is NtripState.Failed, "state should be Failed, was ${streamer.state.value}")
    }

    /** Read the HTTP request up to the blank line so the response can be written. */
    private fun drainRequest(client: Socket) {
        val input = client.getInputStream()
        val sb = StringBuilder()
        while (true) {
            val ch = input.read()
            if (ch == -1) break
            sb.append(ch.toChar())
            if (sb.endsWith("\r\n\r\n")) break
        }
    }
}
