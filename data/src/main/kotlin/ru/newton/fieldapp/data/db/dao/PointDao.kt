package ru.newton.fieldapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.data.db.entity.PointEntity

@Dao
interface PointDao {
    @Query(
        """
        SELECT * FROM points
        WHERE project_id = :projectId
        ORDER BY created_at_utc DESC, point_id DESC
        """,
    )
    fun observeByProject(projectId: Long): Flow<List<PointEntity>>

    /**
     * Latest revision of a point with the given name within a project.
     * Used by save() to decide whether to bump the revision counter.
     */
    @Query(
        """
        SELECT * FROM points
        WHERE project_id = :projectId AND name = :name
        ORDER BY revision DESC LIMIT 1
        """,
    )
    suspend fun latestRevisionByName(projectId: Long, name: String): PointEntity?

    @Query("SELECT * FROM points WHERE point_id = :id")
    suspend fun byId(id: Long): PointEntity?

    /**
     * Names of all points in [projectId] starting with [prefix]. Used by the
     * auto-name generator to find the next free numeric suffix without
     * pulling the entire row set into memory.
     */
    @Query("SELECT name FROM points WHERE project_id = :projectId AND name LIKE :prefix || '%'")
    suspend fun namesWithPrefix(projectId: Long, prefix: String): List<String>

    @Query("SELECT * FROM points WHERE point_id = :id")
    fun observeById(id: Long): Flow<PointEntity?>

    @Insert
    suspend fun insert(entity: PointEntity): Long

    /**
     * In-place coordinate update — used by the CRS-change flow (PRJ-004) where
     * we re-project every point of a project. Does NOT bump revision because
     * the survey identity is unchanged; only the chosen coordinate system
     * encoding is. Bumping revision here would explode the points table.
     */
    @Query("UPDATE points SET n = :n, e = :e, h = :h WHERE point_id = :id")
    suspend fun updateCoordinates(id: Long, n: Double, e: Double, h: Double)

    @Query("UPDATE points SET note = :note WHERE point_id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Query("UPDATE points SET photo_path = :photoPath WHERE point_id = :id")
    suspend fun updatePhotoPath(id: Long, photoPath: String?)

    @Query("DELETE FROM points WHERE point_id = :id")
    suspend fun deleteById(id: Long)
}
