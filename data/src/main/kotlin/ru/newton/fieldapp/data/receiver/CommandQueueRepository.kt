package ru.newton.fieldapp.data.receiver

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.newton.fieldapp.data.db.dao.CommandQueueDao
import ru.newton.fieldapp.data.db.entity.CommandQueueEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DevHandbook §6.4 — facade over the [CommandQueueEntity] table. Lives in
 * `:data` because it mediates Room <-> domain types.
 *
 * Public surface kept minimal: enqueue, observe, transition status, prune.
 * Apply-orchestration logic is in [ApplyReceiverConfigUseCase] — this
 * repository just stores rows.
 */
data class CommandQueueItem(
    val id: Long,
    val commandText: String,
    val originScreenId: String,
    val description: String,
    val status: CommandStatus,
    val replyText: String?,
    val errorText: String?,
    val createdAtUtc: Long,
)

enum class CommandStatus(val code: String) {
    PENDING("pending"),
    SENDING("sending"),
    APPLIED("applied"),
    FAILED("failed"),
    ;

    companion object {
        fun fromCode(s: String): CommandStatus =
            entries.firstOrNull { it.code == s } ?: PENDING
    }
}

@Singleton
class CommandQueueRepository
    @Inject
    constructor(
        private val dao: CommandQueueDao,
    ) {
        fun observeAll(): Flow<List<CommandQueueItem>> =
            dao.observeAll().map { list -> list.map { it.toDomain() } }

        fun observePendingCount(): Flow<Int> = dao.observePendingCount()

        suspend fun pending(): List<CommandQueueItem> =
            dao.pending().map { it.toDomain() }

        suspend fun enqueue(commandText: String, originScreenId: String, description: String): Long =
            dao.insert(
                CommandQueueEntity(
                    commandText = commandText,
                    originScreenId = originScreenId,
                    description = description,
                    status = CommandStatus.PENDING.code,
                    createdAtUtc = System.currentTimeMillis(),
                ),
            )

        suspend fun markStatus(
            id: Long,
            status: CommandStatus,
            replyText: String? = null,
            errorText: String? = null,
        ) = dao.setStatus(id, status.code, replyText, errorText)

        suspend fun delete(id: Long) = dao.deleteById(id)
        suspend fun clearApplied() = dao.clearApplied()
        suspend fun clearAll() = dao.clearAll()

        private fun CommandQueueEntity.toDomain() = CommandQueueItem(
            id = id,
            commandText = commandText,
            originScreenId = originScreenId,
            description = description,
            status = CommandStatus.fromCode(status),
            replyText = replyText,
            errorText = errorText,
            createdAtUtc = createdAtUtc,
        )
    }
