package ru.newton.fieldapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Recorded outcome of a stakeout attempt — target coordinates, actual surveyed
 * coordinates, the residual delta. SUR-132.
 *
 * For point stakeouts `target_point_id` references the source point. For line
 * stakeouts the target is described by `target_n/e/h` of the foot point and
 * `target_point_id` is nullable.
 *
 * Cascade delete from project: removing a project also removes its history.
 */
@Entity(
    tableName = "stakeout_results",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["project_id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["project_id"]),
        Index(value = ["saved_at_utc"]),
    ],
)
data class StakeoutResultEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "result_id")
    val resultId: Long = 0,
    @ColumnInfo(name = "project_id")
    val projectId: Long,
    @ColumnInfo(name = "target_point_id")
    val targetPointId: Long? = null,
    @ColumnInfo(name = "target_label")
    val targetLabel: String,
    /** "POINT" or "LINE" — describes which math produced this row. */
    @ColumnInfo(name = "mode")
    val mode: String,
    @ColumnInfo(name = "target_n")
    val targetN: Double,
    @ColumnInfo(name = "target_e")
    val targetE: Double,
    @ColumnInfo(name = "target_h")
    val targetH: Double,
    @ColumnInfo(name = "actual_n")
    val actualN: Double,
    @ColumnInfo(name = "actual_e")
    val actualE: Double,
    @ColumnInfo(name = "actual_h")
    val actualH: Double,
    /** Horizontal residual in metres (distance for POINT, perpendicular for LINE). */
    @ColumnInfo(name = "delta_h_horizontal")
    val deltaHorizontalM: Double,
    /** Vertical residual in metres (target_h − actual_h). */
    @ColumnInfo(name = "delta_h_vertical")
    val deltaVerticalM: Double,
    @ColumnInfo(name = "saved_at_utc")
    val savedAtUtc: Long,
)
