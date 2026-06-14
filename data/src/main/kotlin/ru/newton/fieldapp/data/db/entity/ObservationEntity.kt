package ru.newton.fieldapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Quality metadata captured the moment a [PointEntity] was measured (v11 add).
 *
 * One row per point — the unique index on `point_id` enforces it. Lets exports
 * and the point detail prove *how* a coordinate was obtained (fix type, σ,
 * epoch count, antenna height), so a Fixed-RTK point can be told apart from a
 * Single point with metre-level error. Cascade-deletes with its parent point.
 *
 * Imported / manual points have no observation row; readers treat absence as
 * "unknown provenance".
 */
@Entity(
    tableName = "observations",
    foreignKeys = [
        ForeignKey(
            entity = PointEntity::class,
            parentColumns = ["point_id"],
            childColumns = ["point_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["point_id"], unique = true),
    ],
)
data class ObservationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "observation_id")
    val observationId: Long = 0,
    @ColumnInfo(name = "point_id")
    val pointId: Long,
    /** "single" | "float" | "fixed" | "dgnss" | "ppp_*" | "none". */
    @ColumnInfo(name = "fix_type")
    val fixType: String,
    @ColumnInfo(name = "sigma_n")
    val sigmaN: Double? = null,
    @ColumnInfo(name = "sigma_e")
    val sigmaE: Double? = null,
    @ColumnInfo(name = "sigma_h")
    val sigmaH: Double? = null,
    val hdop: Double? = null,
    val pdop: Double? = null,
    @ColumnInfo(name = "sats_used")
    val satsUsed: Int? = null,
    @ColumnInfo(name = "correction_age_sec")
    val correctionAgeSec: Double? = null,
    val epochs: Int,
    @ColumnInfo(name = "antenna_height_m")
    val antennaHeightM: Double,
    /** "VERTICAL" | "SLANT" — see [ru.newton.fieldapp.domain.model.AntennaMethod]. */
    @ColumnInfo(name = "antenna_method")
    val antennaMethod: String,
    @ColumnInfo(name = "tilt_applied")
    val tiltApplied: Boolean,
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: Long,
)
