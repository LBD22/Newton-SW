package ru.newton.fieldapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * DevHandbook §6.4 — per-command audit log of the receiver-apply flow.
 *
 * Each row is one Newton command sent to the receiver as part of an Apply
 * batch. `status` traces it through `pending → sending → applied | failed`;
 * `reply_text` carries whatever OK!/OK-/error string the receiver returned.
 *
 * The single-row [PendingPatchEntity] (CMD-004) is still the source of
 * truth for the *desired* configuration; this table records *what was
 * actually attempted* and is visible on SET-080 «Диагностика» for support.
 *
 * Pruning policy: rows older than 30 days are removed by a maintenance
 * task (not yet wired). Until then, the audit grows monotonically — this
 * is a feature for support, not a bug.
 */
@Entity(
    tableName = "command_queue",
    indices = [Index(value = ["created_at_utc"])],
)
data class CommandQueueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "command_text")
    val commandText: String,
    /** Screen id (e.g. "SET-010") or "Apply" for the implicit `system save`. */
    @ColumnInfo(name = "origin_screen_id")
    val originScreenId: String,
    @ColumnInfo(name = "description")
    val description: String,
    /** "pending" | "sending" | "applied" | "failed". */
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "reply_text")
    val replyText: String? = null,
    @ColumnInfo(name = "error_text")
    val errorText: String? = null,
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: Long,
)
