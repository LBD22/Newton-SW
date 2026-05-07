package ru.newton.fieldapp.features.settings.ntrip

import ru.newton.fieldapp.gnss.ntrip.NtripProfile

sealed interface NtripProfileListState {
    data object Loading : NtripProfileListState
    data class Content(val profiles: List<NtripProfile>) : NtripProfileListState
}
