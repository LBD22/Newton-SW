package ru.newton.fieldapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a surveyed/imported/manual point.
 *
 * Uniqueness on `(project_id, name, revision)` enforces the re-measurement
 * convention: same name within a project is allowed only across different
 * revision numbers (PointRepository increments `revision` on save).
 *
 * Cascade-deleting from [ProjectEntity] keeps point storage clean when the
 * parent project is removed.
 */
@Entity(
    tableName = "points",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["project_id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["project_id", "name", "revision"], unique = true),
        Index(value = ["project_id"]),
    ],
)
data class PointEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "point_id")
    val pointId: Long = 0,
    @ColumnInfo(name = "project_id")
    val projectId: Long,
    val name: String,
    val code: String? = null,
    @ColumnInfo(name = "layer_id")
    val layerId: Long? = null,
    val revision: Int = 1,
    @ColumnInfo(name = "n")
    val northingM: Double,
    @ColumnInfo(name = "e")
    val eastingM: Double,
    @ColumnInfo(name = "h")
    val heightM: Double,
    /** Stored as `PointSource.name` ("SURVEY" | "IMPORT" | "MANUAL" | "CALC"). */
    @ColumnInfo(name = "source")
    val sourceName: String,
    @ColumnInfo(name = "external_ref")
    val externalRef: String? = null,
    /** Free-form surveyor note. v8 add. Default empty string for back-compat. */
    @ColumnInfo(name = "note", defaultValue = "")
    val note: String = "",
    /**
     * Relative path under [Context.getFilesDir]`/photos/` to a captured JPEG.
     * v8 add. Null means no photo attached. Stored relative so app reinstalls
     * keep working when the absolute filesDir path changes.
     */
    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null,
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: Long,
)
