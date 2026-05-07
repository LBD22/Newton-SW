package ru.newton.fieldapp.features.settings.apply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.logging.AppLog
import ru.newton.fieldapp.data.receiver.ApplyReceiverConfigUseCase
import ru.newton.fieldapp.data.receiver.CommandQueueRepository
import ru.newton.fieldapp.data.receiver.PatchToCommands
import ru.newton.fieldapp.data.receiver.PendingChangesService
import ru.newton.fieldapp.domain.receiver.ApplyProgress
import javax.inject.Inject

@HiltViewModel
class ApplyViewModel
    @Inject
    constructor(
        pendingChanges: PendingChangesService,
        private val applyUseCase: ApplyReceiverConfigUseCase,
        private val commandQueue: CommandQueueRepository,
        private val log: AppLog,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ApplyState())
        val state: StateFlow<ApplyState> = _state.asStateFlow()

        init {
            pendingChanges.patch
                .onEach { patch ->
                    val commands = PatchToCommands.build(patch)
                    _state.value = _state.value.copy(
                        pending = commands,
                        isDirty = commands.isNotEmpty(),
                    )
                }
                .launchIn(viewModelScope)
            commandQueue.observeAll()
                .onEach { items -> _state.value = _state.value.copy(auditLog = items) }
                .launchIn(viewModelScope)
        }

        fun onClearAudit() {
            viewModelScope.launch { commandQueue.clearAll() }
        }

        fun onRemoveAuditItem(id: Long) {
            viewModelScope.launch { commandQueue.delete(id) }
        }

        fun onApplyClicked() {
            if (_state.value.isApplying) return
            _state.value = _state.value.copy(isApplying = true, progress = ApplyProgress.Idle)
            viewModelScope.launch {
                applyUseCase()
                    .onEach { progress -> _state.value = _state.value.copy(progress = progress) }
                    .collect()
                _state.value = _state.value.copy(isApplying = false)
            }
        }

        fun onExportLogsClicked() {
            viewModelScope.launch {
                runCatching { log.exportArchive(daysBack = 7) }
                    .onSuccess { path -> _state.value = _state.value.copy(lastExportPath = path) }
                    .onFailure { _state.value = _state.value.copy(lastExportPath = null) }
            }
        }
    }
