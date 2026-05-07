package ru.newton.fieldapp.data.staticobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.bluetooth.DataSpp
import ru.newton.fieldapp.core.bluetooth.SppTransport
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SET-021 — raw NMEA byte recorder for PPK / debug post-processing.
 *
 * Subscribes to the [DataSpp] byte stream and writes everything to a
 * timestamped file under the configured root dir (production:
 * `filesDir/static/`). We do NOT generate RINEX — that requires raw
 * observation messages from the binary COMNAV set (BESTPOS, RANGECMP,
 * etc.) and a non-trivial converter. What we do provide is a faithful
 * recording of whatever the receiver was emitting, which surveyors can
 * post-process externally or hand to support.
 *
 * Singleton — there's at most one active recording at any time. Multiple
 * starts are no-ops; stop() flushes and closes the writer.
 */
data class StaticRecording(
    val file: File,
    val startedAtUtc: Long,
    val bytesWritten: Long,
)

sealed interface StaticRecorderState {
    data object Idle : StaticRecorderState
    data class Active(val recording: StaticRecording) : StaticRecorderState
    data class Failed(val message: String) : StaticRecorderState
}

@Singleton
class StaticNmeaRecorder
    @Inject
    constructor(
        @StaticRecorderRoot private val rootDir: File,
        @DataSpp private val dataSpp: SppTransport,
        @StaticRecorderScope private val scope: CoroutineScope,
    ) {
        private val _state = MutableStateFlow<StaticRecorderState>(StaticRecorderState.Idle)
        val state: StateFlow<StaticRecorderState> = _state.asStateFlow()

        private var job: Job? = null
        private var writer: OutputStream? = null

        fun start() {
            if (_state.value is StaticRecorderState.Active) return
            rootDir.mkdirs()
            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(java.util.Date())
            val file = File(rootDir, "static_$ts.nmea")
            try {
                writer = BufferedOutputStream(file.outputStream())
            } catch (t: Throwable) {
                _state.value = StaticRecorderState.Failed(
                    "Не удалось создать файл: ${t.message ?: t.javaClass.simpleName}",
                )
                return
            }
            val started = System.currentTimeMillis()
            _state.value = StaticRecorderState.Active(
                StaticRecording(file = file, startedAtUtc = started, bytesWritten = 0L),
            )
            job?.cancel()
            job = scope.launch {
                dataSpp.incoming.collect { chunk ->
                    runCatching {
                        writer?.write(chunk)
                        val current = _state.value
                        if (current is StaticRecorderState.Active) {
                            _state.value = StaticRecorderState.Active(
                                current.recording.copy(
                                    bytesWritten = current.recording.bytesWritten + chunk.size,
                                ),
                            )
                        }
                    }.onFailure { err ->
                        _state.value = StaticRecorderState.Failed(
                            err.message ?: "Ошибка записи",
                        )
                    }
                }
            }
        }

        fun stop() {
            job?.cancel()
            job = null
            runCatching {
                writer?.flush()
                writer?.close()
            }
            writer = null
            _state.value = StaticRecorderState.Idle
        }

        /** All previously-recorded files, newest first. */
        fun listPastSessions(): List<File> {
            if (!rootDir.exists()) return emptyList()
            return rootDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".nmea") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }

        fun delete(file: File) {
            // Don't allow deleting the file currently being written to.
            val active = (_state.value as? StaticRecorderState.Active)?.recording?.file
            if (active != null && active.absolutePath == file.absolutePath) return
            runCatching { file.delete() }
        }
    }
