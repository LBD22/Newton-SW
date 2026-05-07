package ru.newton.fieldapp.features.settings.ntrip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.newton.fieldapp.gnss.ntrip.NtripProfile
import ru.newton.fieldapp.gnss.ntrip.NtripProfileRepository
import javax.inject.Inject

@HiltViewModel
class NtripProfileEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: NtripProfileRepository,
    ) : ViewModel() {
        private val editingId: Long = savedStateHandle.get<Long>("profileId") ?: 0L

        private val _state = MutableStateFlow(NtripProfileEditState(id = editingId))
        val state: StateFlow<NtripProfileEditState> = _state.asStateFlow()

        init {
            if (editingId != 0L) {
                viewModelScope.launch {
                    repository.byId(editingId)?.let { existing ->
                        _state.value = NtripProfileEditState(
                            id = existing.id,
                            name = existing.name,
                            host = existing.host,
                            portText = existing.port.toString(),
                            mountpoint = existing.mountpoint,
                            login = existing.login,
                            password = existing.password,
                            sendNmea = existing.sendNmea,
                            useTls = existing.useTls,
                        )
                    }
                }
            }
        }

        fun onNameChanged(value: String) = update { copy(name = value, errors = errors.copy(name = null)) }
        fun onHostChanged(value: String) = update { copy(host = value, errors = errors.copy(host = null)) }
        fun onPortChanged(value: String) = update { copy(portText = value, errors = errors.copy(port = null)) }
        fun onMountpointChanged(value: String) = update { copy(mountpoint = value, errors = errors.copy(mountpoint = null)) }
        fun onLoginChanged(value: String) = update { copy(login = value) }
        fun onPasswordChanged(value: String) = update { copy(password = value) }
        fun onSendNmeaChanged(value: Boolean) = update { copy(sendNmea = value) }
        fun onUseTlsChanged(value: Boolean) = update { copy(useTls = value) }

        fun onSaveClicked() {
            val current = _state.value
            val errors = NtripProfileEditState.FieldErrors(
                name = if (current.name.trim().isEmpty()) "Имя обязательно" else null,
                host = if (current.host.trim().isEmpty()) "Хост обязателен" else null,
                port = current.portText.trim().toIntOrNull()
                    ?.takeIf { it in 1..65535 }
                    ?.let { null }
                    ?: "Порт: 1..65535",
                mountpoint = if (current.mountpoint.trim().isEmpty()) "Mountpoint обязателен" else null,
            )
            if (errors.any) {
                _state.value = current.copy(errors = errors)
                return
            }

            _state.value = current.copy(saving = true)
            viewModelScope.launch {
                runCatching {
                    repository.save(
                        NtripProfile(
                            id = current.id,
                            name = current.name.trim(),
                            host = current.host.trim(),
                            port = current.portText.trim().toInt(),
                            mountpoint = current.mountpoint.trim(),
                            login = current.login,
                            password = current.password,
                            sendNmea = current.sendNmea,
                            useTls = current.useTls,
                        ),
                    )
                }.onSuccess { id ->
                    _state.value = _state.value.copy(saving = false, savedId = id)
                }.onFailure {
                    _state.value = _state.value.copy(saving = false)
                }
            }
        }

        private inline fun update(transform: NtripProfileEditState.() -> NtripProfileEditState) {
            _state.value = _state.value.transform()
        }
    }
