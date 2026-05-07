package ru.newton.fieldapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One track-recording session — surveyor walks a contour, samples are written
 * by [TrackPointEntity] children at ~1 Hz. `stopped_at_utc` is null while the
 * session is active.
 */
@Entity(
    tableName = "track_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["project_id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["project_id"])],
)
data class TrackSessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "session_id")
    val sessionId: Long = 0,
    @ColumnInfo(name = "project_id")
    val projectId: Long,
    val name: String,
    @ColumnInfo(name = "started_at_utc")
    val startedAtUtc: Long,
    @ColumnInfo(name = "stopped_at_utc")
    val stoppedAtUtc: Long? = null,
)
