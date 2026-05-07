package ru.newton.fieldapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * CMD-004 — single-row table holding the pending [ReceiverConfigPatch] as a
 * kotlinx-serialization JSON blob.
 *
 * One row, fixed `id = 1`. `INSERT OR REPLACE` keeps the upsert trivial; we
 * don't need history of past patches because the queue invariant is "all
 * pending changes flushed atomically on Apply". Survives process death so a
 * crash mid-session doesn't lose the user's queued settings.
 */
@Entity(tableName = "pending_patch")
data class PendingPatchEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long = SINGLETON_ID,
    @ColumnInfo(name = "patch_json")
    val patchJson: String,
    @ColumnInfo(name = "updated_at_utc")
    val updatedAtUtc: Long,
) {
    companion object {
        const val SINGLETON_ID: Long = 1L
    }
}
