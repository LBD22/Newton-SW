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
            val uhf = patch.input as? InputConfig.Uhf
            val ethernet = patch.input as? InputConfig.NtripClient
            val ethernetProfile = ethernet?.let { n ->
                profiles.firstOrNull { it.host == n.host && it.port == n.port && it.mountpoint == n.endpoint }
            }
            val com = patch.input as? InputConfig.Com
            val tcpClient = patch.input as? InputConfig.TcpClient
            val tcpServer = patch.input as? InputConfig.TcpServer
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
                uhfActiveSummary = uhf?.let {
                    "УКВ ${it.frequencyMHz} МГц · ${it.protocol} · ${it.baudrate}"
                },
                ethernetNtripActiveProfileId = ethernetProfile?.id,
                comActiveSummary = com?.let { "COM${it.index} · ${it.baudrate} бод" },
                tcpActiveSummary = when {
                    tcpClient != null -> "TCP-клиент ${tcpClient.host}:${tcpClient.port}"
                    tcpServer != null -> "TCP-сервер :${tcpServer.port}"
                    else -> null
                },
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

        /**
         * UHF radio as the correction source (`input set uhf <freq> <protocol> <baud>`):
         * the receiver's internal radio listens for corrections. No controller
         * internet/Bluetooth forwarding. Queues the input change for Apply.
         */
        fun onSelectUhf(frequencyMHz: Double, protocolCode: String, baudrate: Int) {
            forwarder.stop()
            pendingChanges.update {
                it.copy(
                    input = InputConfig.Uhf(
                        frequencyMHz = frequencyMHz,
                        protocol = protocolCode,
                        baudrate = baudrate,
                    ),
                )
            }
        }

        /**
         * Receiver-side NTRIP over the receiver's wired network (`input set
         * ntripclient`) — for a docked/base receiver on LAN. No GSM, no controller.
         */
        fun onSelectProfileViaEthernet(profileId: Long) {
            val profile = state.value.profiles.firstOrNull { it.id == profileId } ?: return
            forwarder.stop()
            pendingChanges.update {
                it.copy(
                    input = InputConfig.NtripClient(
                        host = profile.host,
                        port = profile.port,
                        endpoint = profile.mountpoint,
                        login = profile.login,
                        password = profile.password,
                    ),
                )
            }
        }

        /** Serial (COM) port as the correction input (`input set com<N> <baud>`). */
        fun onSelectCom(index: Int, baudrate: Int) {
            forwarder.stop()
            pendingChanges.update { it.copy(input = InputConfig.Com(index = index, baudrate = baudrate)) }
        }

        /** TCP client correction input (`input set tcpclient <host> <port>`). */
        fun onSelectTcpClient(host: String, port: Int) {
            forwarder.stop()
            pendingChanges.update { it.copy(input = InputConfig.TcpClient(host = host, port = port)) }
        }

        /** TCP server correction input (`input set tcpserver <port>`). */
        fun onSelectTcpServer(port: Int) {
            forwarder.stop()
            pendingChanges.update { it.copy(input = InputConfig.TcpServer(port = port)) }
        }

        fun onDisable() {
            forwarder.stop()
        }

        @Suppress("unused")
        private fun launchOnce(block: suspend () -> Unit) {
            viewModelScope.launch { block() }
        }
    }
