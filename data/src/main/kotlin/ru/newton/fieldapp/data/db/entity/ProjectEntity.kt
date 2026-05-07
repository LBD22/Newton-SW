package ru.newton.fieldapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a project.
 *
 * Mapper to/from domain `Project` lives in `data/repository/ProjectRepositoryImpl`.
 * Keep this class free of domain types — pure Room model.
 */
@Entity(
    tableName = "projects",
    indices = [Index(value = ["name"])],
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "project_id")
    val projectId: Long = 0,
    val name: String,
    val comment: String? = null,
    /** Serialized CrsConfig (kotlinx.serialization JSON). */
    @ColumnInfo(name = "crs_config_json")
    val crsConfigJson: String,
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: Long,
    @ColumnInfo(name = "updated_at_utc")
    val updatedAtUtc: Long,
)
