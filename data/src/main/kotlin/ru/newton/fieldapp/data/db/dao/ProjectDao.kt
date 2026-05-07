package ru.newton.fieldapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.data.db.entity.ProjectEntity

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updated_at_utc DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE project_id = :id")
    fun observeById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE project_id = :id")
    suspend fun byId(id: Long): ProjectEntity?

    @Insert
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE project_id = :id")
    suspend fun deleteById(id: Long)
}
