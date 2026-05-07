package ru.newton.fieldapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.data.db.entity.TrackPointEntity
import ru.newton.fieldapp.data.db.entity.TrackSessionEntity

@Dao
interface TrackDao {
    @Query(
        """
        SELECT * FROM track_sessions
        WHERE project_id = :projectId
        ORDER BY started_at_utc DESC
        """,
    )
    fun observeSessionsByProject(projectId: Long): Flow<List<TrackSessionEntity>>

    @Query("SELECT * FROM track_sessions WHERE session_id = :id")
    suspend fun sessionById(id: Long): TrackSessionEntity?

    @Insert
    suspend fun insertSession(session: TrackSessionEntity): Long

    @Query("UPDATE track_sessions SET stopped_at_utc = :stoppedAtUtc WHERE session_id = :id")
    suspend fun stopSession(id: Long, stoppedAtUtc: Long)

    @Query("SELECT * FROM track_points WHERE session_id = :sessionId ORDER BY timestamp_utc ASC")
    suspend fun pointsForSession(sessionId: Long): List<TrackPointEntity>

    @Query("SELECT COUNT(*) FROM track_points WHERE session_id = :sessionId")
    fun observePointCount(sessionId: Long): Flow<Int>

    @Insert
    suspend fun insertPoint(point: TrackPointEntity): Long

    @Query("DELETE FROM track_sessions WHERE session_id = :id")
    suspend fun deleteSession(id: Long)
}
