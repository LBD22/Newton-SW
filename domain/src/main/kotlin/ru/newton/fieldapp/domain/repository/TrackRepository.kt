package ru.newton.fieldapp.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.domain.model.TrackPointSample
import ru.newton.fieldapp.domain.model.TrackSession

/**
 * Persistence gateway for track-recording sessions. Implementation in `:data`.
 */
interface TrackRepository {
    fun observeSessions(projectId: Long): Flow<List<TrackSession>>
    fun observePointCount(sessionId: Long): Flow<Int>
    suspend fun startSession(projectId: Long, name: String): Long
    suspend fun stopSession(sessionId: Long)
    suspend fun appendPoint(sessionId: Long, sample: TrackPointSample)
    suspend fun pointsForSession(sessionId: Long): List<TrackPointSample>
    suspend fun deleteSession(sessionId: Long)
}
