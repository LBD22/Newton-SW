package ru.newton.fieldapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.data.db.entity.CommandQueueEntity

@Dao
interface CommandQueueDao {
    @Query("SELECT * FROM command_queue ORDER BY created_at_utc DESC")
    fun observeAll(): Flow<List<CommandQueueEntity>>

    @Query("SELECT * FROM command_queue WHERE status = 'pending' ORDER BY id ASC")
    suspend fun pending(): List<CommandQueueEntity>

    @Query("SELECT COUNT(*) FROM command_queue WHERE status = 'pending'")
    fun observePendingCount(): Flow<Int>

    @Insert
    suspend fun insert(entity: CommandQueueEntity): Long

    @Query("UPDATE command_queue SET status = :status, reply_text = :reply, error_text = :error WHERE id = :id")
    suspend fun setStatus(id: Long, status: String, reply: String?, error: String?)

    @Query("DELETE FROM command_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM command_queue WHERE status = 'applied'")
    suspend fun clearApplied()

    @Query("DELETE FROM command_queue")
    suspend fun clearAll()
}
