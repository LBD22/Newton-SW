package ru.newton.fieldapp.features.settings.correction

import ru.newton.fieldapp.gnss.ntrip.NtripProfile
import ru.newton.fieldapp.gnss.ntrip.NtripState

data class CorrectionSourceState(
    val profiles: List<NtripProfile> = emptyList(),
    /** Profile streamed through the CONTROLLER (phone pulls RTCM, forwards over BT). */
    val activeProfileId: Long? = null,
    val ntripState: NtripState = NtripState.Idle,
    /**
     * True when RTCM is being forwarded but the `input set bluetooth` change is
     * still queued (not yet flushed by Apply). The receiver ignores RTCM until
     * `system save`, so without applying, the stream looks active while the
     * receiver discards every byte (field reports Баг-002/005).
     */
    val inputApplyPending: Boolean = false,
    /** Profile queued for RECEIVER-GSM NTRIP (`input set gsmntripclient`), if any. */
    val gsmNtripActiveProfileId: Long? = null,
    /** A receiver-GSM NTRIP input change is queued and awaiting Apply. */
    val gsmNtripApplyPending: Boolean = false,
    /** The receiver's GSM modem is enabled in the pending config (APN set). */
    val gsmModemEnabled: Boolean = false,
)
