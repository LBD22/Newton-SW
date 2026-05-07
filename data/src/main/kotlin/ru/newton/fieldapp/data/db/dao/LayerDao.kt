package ru.newton.fieldapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.data.db.entity.LayerEntity

@Dao
interface LayerDao {
    @Query("SELECT * FROM layers WHERE project_id = :projectId ORDER BY name ASC")
    fun observeByProject(projectId: Long): Flow<List<LayerEntity>>

    @Query("SELECT * FROM layers WHERE layer_id = :id LIMIT 1")
    suspend fun byId(id: Long): LayerEntity?

    @Query("SELECT * FROM layers WHERE project_id = :projectId AND name = :name LIMIT 1")
    suspend fun byName(projectId: Long, name: String): LayerEntity?

    /** Insert; OnConflict=IGNORE so an idempotent ensure() can no-op. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(layer: LayerEntity): Long

    @Update
    suspend fun update(layer: LayerEntity)

    @Query("DELETE FROM layers WHERE layer_id = :id")
    suspend fun deleteById(id: Long)
}
