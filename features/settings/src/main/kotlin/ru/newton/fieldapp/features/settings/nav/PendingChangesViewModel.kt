package ru.newton.fieldapp.features.settings.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import ru.newton.fieldapp.data.receiver.CommandQueueRepository
import javax.inject.Inject

/**
 * Exposes the count of pending receiver commands so the settings index can
 * surface a [ru.newton.fieldapp.core.ui.components.PendingBanner] when the
 * queue is non-empty — the user gets a persistent prompt to apply queued
 * changes (`system save`), per Field Blue spec §6.8 and the Newton protocol
 * apply-flow rule (nothing applies until explicit save).
 */
@HiltViewModel
class PendingChangesViewModel
    @Inject
    constructor(
        commandQueue: CommandQueueRepository,
    ) : ViewModel() {
        val pendingCount: StateFlow<Int> = commandQueue
            .observePendingCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    }
