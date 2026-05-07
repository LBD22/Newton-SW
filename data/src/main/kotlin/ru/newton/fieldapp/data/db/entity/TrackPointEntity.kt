package ru.newton.fieldapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One sample inside a track session. Written by `TrackController` at ~1 Hz
 * when fix quality is at-least Single. `n`/`e`/`h` are stored in the project's
 * CRS, matching the convention used for [PointEntity] — this lets exports
 * round-trip without coordinate transforms.
 */
@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = TrackSessionEntity::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["session_id"])],
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "track_point_id")
    val trackPointId: Long = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: Long,
    @ColumnInfo(name = "n")
    val northingM: Double,
    @ColumnInfo(name = "e")
    val eastingM: Double,
    @ColumnInfo(name = "h")
    val heightM: Double,
    @ColumnInfo(name = "fix_quality")
    val fixQuality: String,
    @ColumnInfo(name = "timestamp_utc")
    val timestampUtc: Long,
)
