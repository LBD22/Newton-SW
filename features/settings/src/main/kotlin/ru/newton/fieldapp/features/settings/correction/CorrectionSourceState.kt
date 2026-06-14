package ru.newton.fieldapp.features.settings.correction

import ru.newton.fieldapp.gnss.ntrip.NtripProfile
import ru.newton.fieldapp.gnss.ntrip.NtripState

data class CorrectionSourceState(
    val profiles: List<NtripProfile> = emptyList(),
    val activeProfileId: Long? = null,
    val ntripState: NtripState = NtripState.Idle,
    /**
     * True when RTCM is being forwarded but the `input set bluetooth` change is
     * still queued (not yet flushed by Apply). The receiver ignores RTCM until
     * `system save`, so without applying, the stream looks active while the
     * receiver discards every byte (field reports Баг-002/005).
     */
    val inputApplyPending: Boolean = false,
)
