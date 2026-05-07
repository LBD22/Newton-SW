package ru.newton.fieldapp.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.data.db.entity.NtripProfileEntity

@Dao
interface NtripProfileDao {
    @Query("SELECT * FROM ntrip_profiles ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<NtripProfileEntity>>

    @Query("SELECT * FROM ntrip_profiles WHERE profile_id = :id")
    suspend fun byId(id: Long): NtripProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: NtripProfileEntity): Long

    @Update
    suspend fun update(entity: NtripProfileEntity)

    @Query("DELETE FROM ntrip_profiles WHERE profile_id = :id")
    suspend fun deleteById(id: Long)
}
