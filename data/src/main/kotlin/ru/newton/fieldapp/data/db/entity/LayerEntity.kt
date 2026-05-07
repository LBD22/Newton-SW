package ru.newton.fieldapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PRJ-030/031 — per-project layer of points / lines / polygons.
 *
 * Layers are how surveyors organise heterogeneous data on the same project:
 * "kerbs", "trees", "control points", "as-built", etc. The point's
 * [PointEntity.layerId] points here; layer-level visibility/colour drive
 * the map renderer.
 *
 * Cascade-delete from project keeps orphans away. Layer name is unique
 * within a project — stops accidental duplicates from CSV imports that
 * synthesise layer names from the `code` column.
 */
@Entity(
    tableName = "layers",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["project_id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["project_id", "name"], unique = true),
        Index(value = ["project_id"]),
    ],
)
data class LayerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "layer_id")
    val layerId: Long = 0,
    @ColumnInfo(name = "project_id")
    val projectId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    /**
     * RGB colour as 0xRRGGBB (no alpha). 0xFFFFFF = «default». UI may show
     * the colour as a chip/dot next to point names.
     */
    @ColumnInfo(name = "color_rgb")
    val colorRgb: Int = 0xFFFFFF,
    @ColumnInfo(name = "visible")
    val visible: Boolean = true,
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: Long,
)
