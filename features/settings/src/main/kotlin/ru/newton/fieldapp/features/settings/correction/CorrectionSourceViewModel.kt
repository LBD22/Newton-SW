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
            pendingChanges.patch,
        ) { profiles, ntripState, patch ->
            val gsm = patch.input as? InputConfig.GsmNtripClient
            val gsmProfile = gsm?.let { g ->
                profiles.firstOrNull { it.host == g.host && it.port == g.port && it.mountpoint == g.endpoint }
            }
            CorrectionSourceState(
                profiles = profiles,
                activeProfileId = forwarder.activeProfile?.id,
                ntripState = ntripState,
                // The `input set bluetooth` change is still pending while patch.input
                // is non-null; combined with an active forwarder it means RTCM is
                // flowing into a receiver that hasn't been told to accept it yet.
                inputApplyPending = patch.input != null && forwarder.activeProfile != null,
                gsmNtripActiveProfileId = gsmProfile?.id,
                gsmNtripApplyPending = gsm != null,
                gsmModemEnabled = patch.gsm?.enabled == true,
            )
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CorrectionSourceState())

        /** Controller-side NTRIP: the phone pulls RTCM and forwards it over Bluetooth. */
        fun onSelectProfile(profileId: Long) {
            val profile = state.value.profiles.firstOrNull { it.id == profileId } ?: return
            // Receiver still needs to listen on Bluetooth — RTCM is what we forward.
            pendingChanges.update { it.copy(input = InputConfig.Bluetooth) }
            forwarder.start(profile)
        }

        /**
         * Receiver-side NTRIP over the receiver's own GSM modem — no controller
         * internet, no Bluetooth forwarding. Stops any controller forwarder and
         * queues `input set gsmntripclient`; the user applies it (with the GSM/APN
         * config from the GSM screen) on the Apply screen.
         */
        fun onSelectProfileViaGsm(profileId: Long) {
            val profile = state.value.profiles.firstOrNull { it.id == profileId } ?: return
            forwarder.stop()
            pendingChanges.update {
                it.copy(
                    input = InputConfig.GsmNtripClient(
                        host = profile.host,
                        port = profile.port,
                        endpoint = profile.mountpoint,
                        login = profile.login,
                        password = profile.password,
                    ),
                )
            }
        }

        fun onDisable() {
            forwarder.stop()
        }

        @Suppress("unused")
        private fun launchOnce(block: suspend () -> Unit) {
            viewModelScope.launch { block() }
        }
    }
