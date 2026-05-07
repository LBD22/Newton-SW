package ru.newton.fieldapp.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.data.db.entity.PendingPatchEntity

/**
 * CMD-004 — DAO for the singleton pending-patch row.
 *
 * `observe()` is a Flow so the service can reactively pick up updates if a
 * future feature (e.g. cloud sync) writes the patch from another module.
 */
@Dao
interface PendingPatchDao {
    @Query("SELECT * FROM pending_patch WHERE id = :id LIMIT 1")
    fun observe(id: Long = PendingPatchEntity.SINGLETON_ID): Flow<PendingPatchEntity?>

    @Query("SELECT * FROM pending_patch WHERE id = :id LIMIT 1")
    suspend fun get(id: Long = PendingPatchEntity.SINGLETON_ID): PendingPatchEntity?

    @Upsert
    suspend fun upsert(entity: PendingPatchEntity)

    @Query("DELETE FROM pending_patch WHERE id = :id")
    suspend fun deleteById(id: Long = PendingPatchEntity.SINGLETON_ID)
}
