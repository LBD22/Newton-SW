package ru.newton.fieldapp.features.settings.rover

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.newton.fieldapp.data.receiver.PendingChangesService
import ru.newton.fieldapp.domain.receiver.RoverMode
import javax.inject.Inject

@HiltViewModel
class RoverSettingsViewModel
    @Inject
    constructor(
        private val pendingChanges: PendingChangesService,
    ) : ViewModel() {
        private val _state = MutableStateFlow(initialFromPatch())
        val state: StateFlow<RoverSettingsState> = _state.asStateFlow()

        fun onModeSelected(mode: RoverMode) {
            _state.value = _state.value.copy(mode = mode)
            pendingChanges.update { it.copy(roverMode = mode) }
        }

        fun onMaskChanged(value: String) {
            _state.value = _state.value.copy(maskText = value, maskError = null)
        }

        fun onRtcmIdChanged(value: String) {
            _state.value = _state.value.copy(rtcmIdText = value)
            value.trim().toIntOrNull()?.let { id -> pendingChanges.update { it.copy(rtcmId = id) } }
        }

        /**
         * Validate mask once the user finishes editing. The protocol accepts
         * 0..30°; we narrow to 5..30 because <5° is rarely useful and increases
         * multipath noise — matches the surveyor checklist.
         */
        fun onMaskCommit() {
            val raw = _state.value.maskText.trim()
            val parsed = raw.toIntOrNull()
            if (parsed == null || parsed !in 0..30) {
                _state.value = _state.value.copy(maskError = "Маска: целое число 0..30")
                return
            }
            pendingChanges.update { it.copy(surveyMaskDeg = parsed) }
            _state.value = _state.value.copy(maskError = null)
        }

        private fun initialFromPatch(): RoverSettingsState {
            val patch = pendingChanges.patch.value
            return RoverSettingsState(
                mode = patch.roverMode ?: RoverMode.ROVER,
                maskText = patch.surveyMaskDeg?.toString() ?: "10",
                rtcmIdText = patch.rtcmId?.toString() ?: "",
            )
        }
    }
