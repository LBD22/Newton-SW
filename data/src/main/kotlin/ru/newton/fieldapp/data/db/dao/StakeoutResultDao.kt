package ru.newton.fieldapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.data.db.entity.StakeoutResultEntity

@Dao
interface StakeoutResultDao {
    @Query(
        """
        SELECT * FROM stakeout_results
        WHERE project_id = :projectId
        ORDER BY saved_at_utc DESC
        """,
    )
    fun observeByProject(projectId: Long): Flow<List<StakeoutResultEntity>>

    @Insert
    suspend fun insert(entity: StakeoutResultEntity): Long

    @Query("DELETE FROM stakeout_results WHERE result_id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM stakeout_results WHERE project_id = :projectId")
    suspend fun deleteAllForProject(projectId: Long)
}
