package ru.newton.fieldapp.features.settings.apply

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonInfoBadge
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSectionLabel
import ru.newton.fieldapp.core.ui.components.NewtonStatusPill
import ru.newton.fieldapp.core.ui.components.NewtonSuccessBadge
import ru.newton.fieldapp.core.ui.components.NewtonSuccessButton
import ru.newton.fieldapp.core.ui.theme.LocalFixStatusColors
import ru.newton.fieldapp.data.receiver.CommandQueueItem
import ru.newton.fieldapp.data.receiver.CommandStatus
import ru.newton.fieldapp.data.receiver.PreparedCommand
import ru.newton.fieldapp.domain.receiver.ApplyProgress

@Composable
fun ApplyScreen(
    onBack: () -> Unit,
    viewModel: ApplyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ApplyContent(
        state = state,
        onBack = onBack,
        onApply = viewModel::onApplyClicked,
        onExportLogs = viewModel::onExportLogsClicked,
        onClearAudit = viewModel::onClearAudit,
        onRemoveAuditItem = viewModel::onRemoveAuditItem,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplyContent(
    state: ApplyState,
    onBack: () -> Unit,
    onApply: () -> Unit,
    onExportLogs: () -> Unit,
    onClearAudit: () -> Unit,
    onRemoveAuditItem: (Long) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Применение и диагностика") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NewtonSecondaryButton(
                    onClick = onExportLogs,
                    text = "Журнал",
                    icon = Icons.Default.Download,
                    modifier = Modifier.weight(1f),
                )
                NewtonSuccessButton(
                    onClick = onApply,
                    text = if (state.isApplying) "Применение…" else "Применить",
                    icon = Icons.Default.CheckCircle,
                    enabled = state.isDirty && !state.isApplying,
                    modifier = Modifier.weight(2f),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProgressBlock(state.progress)
            PendingBlock(state.pending, isDirty = state.isDirty)
            AuditBlock(state.auditLog, onClearAudit, onRemoveAuditItem)
            state.lastExportPath?.let { path ->
                NewtonCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        NewtonSectionLabel("Журнал сохранён")
                        Text(path, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditBlock(
    items: List<CommandQueueItem>,
    onClear: () -> Unit,
    onRemove: (Long) -> Unit,
) {
    if (items.isEmpty()) return
    val fixColors = LocalFixStatusColors.current
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NewtonSectionLabel("Журнал команд")
                    NewtonInfoBadge("${items.size}")
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Download, contentDescription = "Очистить", tint = MaterialTheme.colorScheme.primary)
                }
            }
            // Show only the last 25 items inline — older entries scroll past
            // the LazyColumn cap below; full history lives in the database
            // and is exported via the «Журнал» button in the bottom bar.
            items.take(25).forEach { item ->
                AuditRow(item, fixColors, onRemove)
            }
        }
    }
}

@Composable
private fun AuditRow(
    item: CommandQueueItem,
    fixColors: ru.newton.fieldapp.core.ui.theme.FixStatusColors,
    onRemove: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(item.originScreenId, style = MaterialTheme.typography.labelMedium)
                StatusBadge(item.status, fixColors)
            }
            Text(item.description, style = MaterialTheme.typography.titleSmall)
            Text(
                item.commandText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.errorText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        IconButton(onClick = { onRemove(item.id) }) {
            Icon(Icons.Default.Download, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusBadge(status: CommandStatus, fixColors: ru.newton.fieldapp.core.ui.theme.FixStatusColors) {
    when (status) {
        CommandStatus.PENDING -> NewtonInfoBadge("в очереди")
        CommandStatus.SENDING -> NewtonStatusPill(
            text = "идёт",
            background = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
        )
        CommandStatus.APPLIED -> NewtonSuccessBadge(text = "применено")
        CommandStatus.FAILED -> NewtonStatusPill(
            text = "ошибка",
            background = fixColors.noFix.copy(alpha = 0.16f),
            contentColor = fixColors.noFix,
        )
    }
}

@Composable
private fun ProgressBlock(progress: ApplyProgress) {
    when (progress) {
        ApplyProgress.Idle -> Unit
        is ApplyProgress.Sending -> NewtonCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                NewtonSectionLabel("Прогресс")
                Text(
                    "[${progress.current}/${progress.total}] ${progress.description}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(progress.commandText, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { progress.current.toFloat() / progress.total.toFloat().coerceAtLeast(1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        ApplyProgress.Succeeded -> NewtonCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    "Изменения применены. system save → OK!",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        is ApplyProgress.Failed -> NewtonCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NewtonSectionLabel("Ошибка применения")
                Text(progress.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                progress.atStep?.let { Text("На шаге $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun PendingBlock(commands: List<PreparedCommand>, isDirty: Boolean) {
    NewtonCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NewtonSectionLabel(if (isDirty) "Ожидающие изменения" else "Нет изменений")
            if (commands.isEmpty()) {
                Text(
                    "Откройте Настройки ровера или Источник коррекций — изменения появятся здесь.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(commands) { cmd -> PreparedCommandRow(cmd) }
                }
            }
        }
    }
}

@Composable
private fun PreparedCommandRow(cmd: PreparedCommand) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(cmd.description, style = MaterialTheme.typography.titleSmall)
        Text(
            cmd.command,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
