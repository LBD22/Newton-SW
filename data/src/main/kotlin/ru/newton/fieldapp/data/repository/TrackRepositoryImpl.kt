package ru.newton.fieldapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.newton.fieldapp.data.db.dao.TrackDao
import ru.newton.fieldapp.data.db.entity.TrackPointEntity
import ru.newton.fieldapp.data.db.entity.TrackSessionEntity
import ru.newton.fieldapp.domain.model.TrackPointSample
import ru.newton.fieldapp.domain.model.TrackSession
import ru.newton.fieldapp.domain.repository.TrackRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepositoryImpl
    @Inject
    constructor(
        private val dao: TrackDao,
    ) : TrackRepository {
        override fun observeSessions(projectId: Long): Flow<List<TrackSession>> =
            dao.observeSessionsByProject(projectId).map { rows -> rows.map { it.toDomain() } }

        override fun observePointCount(sessionId: Long): Flow<Int> = dao.observePointCount(sessionId)

        override suspend fun startSession(projectId: Long, name: String): Long {
            val now = System.currentTimeMillis()
            return dao.insertSession(
                TrackSessionEntity(projectId = projectId, name = name, startedAtUtc = now),
            )
        }

        override suspend fun stopSession(sessionId: Long) {
            dao.stopSession(sessionId, System.currentTimeMillis())
        }

        override suspend fun appendPoint(sessionId: Long, sample: TrackPointSample) {
            dao.insertPoint(
                TrackPointEntity(
                    sessionId = sessionId,
                    northingM = sample.n,
                    eastingM = sample.e,
                    heightM = sample.h,
                    fixQuality = sample.fixQuality,
                    timestampUtc = sample.timestampUtc,
                ),
            )
        }

        override suspend fun pointsForSession(sessionId: Long): List<TrackPointSample> =
            dao.pointsForSession(sessionId).map {
                TrackPointSample(
                    n = it.northingM,
                    e = it.eastingM,
                    h = it.heightM,
                    fixQuality = it.fixQuality,
                    timestampUtc = it.timestampUtc,
                )
            }

        override suspend fun deleteSession(sessionId: Long) = dao.deleteSession(sessionId)

        private fun TrackSessionEntity.toDomain(): TrackSession = TrackSession(
            id = sessionId,
            projectId = projectId,
            name = name,
            startedAtUtc = startedAtUtc,
            stoppedAtUtc = stoppedAtUtc,
        )
    }
