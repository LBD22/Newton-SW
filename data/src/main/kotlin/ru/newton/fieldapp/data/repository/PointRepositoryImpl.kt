package ru.newton.fieldapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.newton.fieldapp.data.db.dao.PointDao
import ru.newton.fieldapp.data.mapper.toDomain
import ru.newton.fieldapp.data.mapper.toEntity
import ru.newton.fieldapp.domain.model.NewPoint
import ru.newton.fieldapp.domain.model.Point
import ru.newton.fieldapp.domain.repository.PointRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PointRepositoryImpl
    @Inject
    constructor(
        private val dao: PointDao,
    ) : PointRepository {
        override fun observePoints(projectId: Long): Flow<List<Point>> =
            dao.observeByProject(projectId).map { list -> list.map { it.toDomain() } }

        override fun observeById(id: Long): Flow<Point?> =
            dao.observeById(id).map { it?.toDomain() }

        override suspend fun latestRevisionByName(projectId: Long, name: String): Point? =
            dao.latestRevisionByName(projectId, name)?.toDomain()

        override suspend fun nextAutoName(projectId: Long, prefix: String, padding: Int): String {
            val taken = dao.namesWithPrefix(projectId, prefix)
            // Parse the digits trailing the prefix; rows whose suffix isn't
            // numeric are ignored — the user has named those manually and
            // the auto-namer should jump over without colliding.
            val maxNumber = taken.mapNotNull { name ->
                name.removePrefix(prefix).toIntOrNull()
            }.maxOrNull() ?: 0
            val nextNumber = maxNumber + 1
            val suffix = if (padding > 0) {
                nextNumber.toString().padStart(padding, '0')
            } else {
                nextNumber.toString()
            }
            return "$prefix$suffix"
        }

        override suspend fun save(point: NewPoint): Long {
            val previous = dao.latestRevisionByName(point.projectId, point.name)
            val nextRevision = (previous?.revision ?: 0) + 1
            val entity = point.toEntity(revision = nextRevision, createdAtUtc = System.currentTimeMillis())
            return dao.insert(entity)
        }

        override suspend fun updateCoordinates(id: Long, n: Double, e: Double, h: Double) =
            dao.updateCoordinates(id, n, e, h)

        override suspend fun updateNote(id: Long, note: String) = dao.updateNote(id, note)

        override suspend fun updatePhotoPath(id: Long, photoPath: String?) =
            dao.updatePhotoPath(id, photoPath)

        override suspend fun delete(id: Long) = dao.deleteById(id)
    }
