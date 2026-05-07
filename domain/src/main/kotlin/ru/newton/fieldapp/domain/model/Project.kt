package ru.newton.fieldapp.domain.model

/**
 * A field survey project. Top-level aggregate in the domain.
 */
data class Project(
    val id: Long,
    val name: String,
    val comment: String?,
    val crsConfig: CrsConfig,
    val createdAtUtc: Long,
    val updatedAtUtc: Long,
)

/**
 * A surveyed or imported point.
 *
 * Note: the SAME name can repeat within a project across different revisions
 * (re-measurement scenario). Uniqueness is enforced on (projectId, name, revision).
 */
data class Point(
    val id: Long,
    val projectId: Long,
    val name: String,
    val code: String?,
    val layerId: Long?,
    val revision: Int,
    val n: Double,
    val e: Double,
    val h: Double,
    val source: PointSource,
    val externalRef: String? = null,
    /** Free-form surveyor note. Empty string when nothing has been entered. */
    val note: String = "",
    /** Path under filesDir/photos/ — null when no photo is attached. */
    val photoPath: String? = null,
    val createdAtUtc: Long,
)

enum class PointSource { SURVEY, IMPORT, MANUAL, CALC }

/** Minimum required data to save a new point (id assigned by repository). */
data class NewPoint(
    val projectId: Long,
    val name: String,
    val code: String?,
    val layerId: Long?,
    val n: Double,
    val e: Double,
    val h: Double,
    val source: PointSource,
    val externalRef: String? = null,
    val note: String = "",
    val photoPath: String? = null,
)
