package ru.newton.fieldapp.features.settings.correction

import ru.newton.fieldapp.gnss.ntrip.NtripProfile
import ru.newton.fieldapp.gnss.ntrip.NtripState

data class CorrectionSourceState(
    val profiles: List<NtripProfile> = emptyList(),
    val activeProfileId: Long? = null,
    val ntripState: NtripState = NtripState.Idle,
)
