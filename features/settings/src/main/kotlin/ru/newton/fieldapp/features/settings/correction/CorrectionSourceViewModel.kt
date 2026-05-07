package ru.newton.fieldapp.features.settings.correction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.data.receiver.PendingChangesService
import ru.newton.fieldapp.domain.receiver.InputConfig
import ru.newton.fieldapp.gnss.ntrip.NtripForwarder
import ru.newton.fieldapp.gnss.ntrip.NtripProfileRepository
import javax.inject.Inject

@HiltViewModel
class CorrectionSourceViewModel
    @Inject
    constructor(
        repository: NtripProfileRepository,
        private val forwarder: NtripForwarder,
        private val pendingChanges: PendingChangesService,
    ) : ViewModel() {
        val state: StateFlow<CorrectionSourceState> = combine(
            repository.observeAll(),
            forwarder.state,
        ) { profiles, ntripState ->
            CorrectionSourceState(
                profiles = profiles,
                activeProfileId = forwarder.activeProfile?.id,
                ntripState = ntripState,
            )
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CorrectionSourceState())

        fun onSelectProfile(profileId: Long) {
            val profile = state.value.profiles.firstOrNull { it.id == profileId } ?: return
            // Receiver still needs to listen on Bluetooth — RTCM is what we forward.
            pendingChanges.update { it.copy(input = InputConfig.Bluetooth) }
            forwarder.start(profile)
        }

        fun onDisable() {
            forwarder.stop()
        }

        @Suppress("unused")
        private fun launchOnce(block: suspend () -> Unit) {
            viewModelScope.launch { block() }
        }
    }
