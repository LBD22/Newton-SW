package ru.newton.fieldapp.features.settings.apply

import ru.newton.fieldapp.data.receiver.CommandQueueItem
import ru.newton.fieldapp.data.receiver.PreparedCommand
import ru.newton.fieldapp.domain.receiver.ApplyProgress

data class ApplyState(
    val pending: List<PreparedCommand> = emptyList(),
    val isDirty: Boolean = false,
    val progress: ApplyProgress = ApplyProgress.Idle,
    val isApplying: Boolean = false,
    val lastExportPath: String? = null,
    /** DevHandbook §6.4 — most-recent-first audit log of Apply attempts. */
    val auditLog: List<CommandQueueItem> = emptyList(),
)
