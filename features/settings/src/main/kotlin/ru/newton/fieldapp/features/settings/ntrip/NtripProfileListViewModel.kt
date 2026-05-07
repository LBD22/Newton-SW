package ru.newton.fieldapp.features.settings.ntrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.gnss.ntrip.NtripProfileRepository
import javax.inject.Inject

@HiltViewModel
class NtripProfileListViewModel
    @Inject
    constructor(
        private val repository: NtripProfileRepository,
    ) : ViewModel() {
        val state: StateFlow<NtripProfileListState> = repository.observeAll()
            .map<List<ru.newton.fieldapp.gnss.ntrip.NtripProfile>, NtripProfileListState> {
                NtripProfileListState.Content(it)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NtripProfileListState.Loading)

        fun delete(id: Long) {
            viewModelScope.launch { repository.delete(id) }
        }
    }
