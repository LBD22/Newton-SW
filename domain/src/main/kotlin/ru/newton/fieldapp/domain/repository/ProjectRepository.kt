package ru.newton.fieldapp.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.domain.model.NewObservation
import ru.newton.fieldapp.domain.model.NewPoint
import ru.newton.fieldapp.domain.model.Observation
import ru.newton.fieldapp.domain.model.Point
import ru.newton.fieldapp.domain.model.Project

/**
 * Gateway to project metadata. Interface lives in :domain, implementation in :data.
 *
 * UI and use-cases depend on this interface; they do not import Room.
 */
interface ProjectRepository {
    fun observeAll(): Flow<List<Project>>

    fun observeById(id: Long): Flow<Project?>

    suspend fun create(
        name: String,
        comment: String?,
        crsPresetId: String,
    ): Project

    suspend fun rename(
        id: Long,
        newName: String,
    )

    /**
     * Replace the project's CRS preset id. PRJ-004: caller is expected to have
     * already re-projected every point's stored (n, e, h) into the new CRS via
     * [PointRepository.updateCoordinates] — this method only flips the metadata.
     */
    suspend fun setCrs(id: Long, presetId: String)

    /** Persist (or clear with null) the project's local site calibration. */
    suspend fun setCalibration(id: Long, calibration: ru.newton.fieldapp.domain.model.CalibrationConfig?)

    suspend fun delete(id: Long)

    suspend fun copy(
        id: Long,
        newName: String,
    ): Project
}

/**
 * Gateway to point data with revision semantics.
 */
interface PointRepository {
    fun observePoints(projectId: Long): Flow<List<Point>>

    /** Single-point Flow — emits null if the row was deleted. PRJ-011. */
    fun observeById(id: Long): Flow<Point?>

    suspend fun latestRevisionByName(
        projectId: Long,
        name: String,
    ): Point?

    /**
     * Returns the next unused name in the form `<prefix><N padded to padding digits>`
     * for the given [projectId]. Inspects only existing names with the
     * supplied [prefix] so concurrent surveys with different prefixes don't
     * fight for numbers.
     */
    suspend fun nextAutoName(
        projectId: Long,
        prefix: String,
        padding: Int,
    ): String

    /**
     * Persists a new point and, optionally, the quality [observation] captured
     * with it — both in one transaction. If a point with the same name already
     * exists, creates a new revision (revision + 1), preserving history.
     *
     * Returns the id of the saved point.
     */
    suspend fun save(point: NewPoint, observation: NewObservation? = null): Long

    /** Quality metadata captured with [pointId]; null for imported/manual points. */
    suspend fun observationForPoint(pointId: Long): Observation?

    /** All observations in a project keyed by point id — used by exports. */
    suspend fun observationsByProject(projectId: Long): Map<Long, Observation>

    /**
     * Updates the (n, e, h) coordinates of an existing point in-place — does
     * not bump revision. Reserved for the PRJ-004 CRS-change flow; survey and
     * stakeout always go through [save] which manages revisions properly.
     */
    suspend fun updateCoordinates(id: Long, n: Double, e: Double, h: Double)

    suspend fun updateNote(id: Long, note: String)

    suspend fun updatePhotoPath(id: Long, photoPath: String?)

    suspend fun delete(id: Long)
}
