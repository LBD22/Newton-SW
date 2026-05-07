package ru.newton.fieldapp.features.settings.output

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.newton.fieldapp.data.receiver.PendingChangesService
import ru.newton.fieldapp.domain.receiver.OutputMessageConfig
import ru.newton.fieldapp.domain.receiver.OutputStreamConfig
import ru.newton.fieldapp.domain.receiver.StreamTarget
import javax.inject.Inject

data class OutputConfigState(
    val messages: List<OutputMessageConfig> = emptyList(),
    val streams: List<OutputStreamConfig> = emptyList(),
    val showAddMessage: Boolean = false,
    val showAddStream: Boolean = false,
)

@HiltViewModel
class OutputConfigViewModel
    @Inject
    constructor(
        private val pendingChanges: PendingChangesService,
    ) : ViewModel() {
        val state: StateFlow<OutputConfigState> = pendingChanges.patch
            .map { patch ->
                OutputConfigState(
                    messages = patch.outputMessages.orEmpty(),
                    streams = patch.outputStreams.orEmpty(),
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OutputConfigState())

        fun applyMvpDefaults() {
            val sources = listOf("M", "R1", "R2", "PPP")
            val msgTypes = listOf("GPGGA", "GPRMC", "GPGSA", "GPGSV", "GPGST")
            val defaultMessages = msgTypes.map { type ->
                OutputMessageConfig(source = "M", type = type, rate = "1HZ", format = "A")
            }
            val defaultStreams = listOf(
                OutputStreamConfig(sources = sources, target = StreamTarget.Bluetooth),
            )
            pendingChanges.update { it.copy(outputMessages = defaultMessages, outputStreams = defaultStreams) }
        }

        fun addMessage(config: OutputMessageConfig) {
            pendingChanges.update { current ->
                val list = (current.outputMessages.orEmpty() + config)
                current.copy(outputMessages = list)
            }
        }

        fun removeMessage(index: Int) {
            pendingChanges.update { current ->
                val list = current.outputMessages.orEmpty().toMutableList()
                if (index in list.indices) list.removeAt(index)
                current.copy(outputMessages = list.toList())
            }
        }

        fun addStream(config: OutputStreamConfig) {
            pendingChanges.update { current ->
                val list = (current.outputStreams.orEmpty() + config)
                current.copy(outputStreams = list)
            }
        }

        fun removeStream(index: Int) {
            pendingChanges.update { current ->
                val list = current.outputStreams.orEmpty().toMutableList()
                if (index in list.indices) list.removeAt(index)
                current.copy(outputStreams = list.toList())
            }
        }

        fun clearAll() {
            pendingChanges.update {
                it.copy(outputMessages = emptyList(), outputStreams = emptyList())
            }
        }
    }
