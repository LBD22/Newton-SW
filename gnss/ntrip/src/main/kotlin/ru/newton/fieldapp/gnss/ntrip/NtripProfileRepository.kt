package ru.newton.fieldapp.gnss.ntrip

import kotlinx.coroutines.flow.Flow

/**
 * Persistence interface for [NtripProfile]s. Lives in `:gnss:ntrip` so the
 * NTRIP feature module can consume it without a `:data` dependency. The
 * implementation in `:data` does the envelope encryption.
 */
interface NtripProfileRepository {
    fun observeAll(): Flow<List<NtripProfile>>
    suspend fun byId(id: Long): NtripProfile?
    suspend fun save(profile: NtripProfile): Long
    suspend fun delete(id: Long)
}
