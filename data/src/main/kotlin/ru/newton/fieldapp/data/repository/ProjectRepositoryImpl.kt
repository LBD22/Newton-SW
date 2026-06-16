package ru.newton.fieldapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import ru.newton.fieldapp.data.db.dao.ProjectDao
import ru.newton.fieldapp.data.db.entity.ProjectEntity
import ru.newton.fieldapp.data.mapper.toDomain
import ru.newton.fieldapp.domain.model.CalibrationConfig
import ru.newton.fieldapp.domain.model.CrsConfig
import ru.newton.fieldapp.domain.model.GeoidConfig
import ru.newton.fieldapp.domain.model.HeightMode
import ru.newton.fieldapp.domain.model.Project
import ru.newton.fieldapp.domain.repository.ProjectRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl
    @Inject
    constructor(
        private val dao: ProjectDao,
        private val json: Json,
    ) : ProjectRepository {
        override fun observeAll(): Flow<List<Project>> = dao.observeAll().map { list -> list.map { it.toDomain(json) } }

        override fun observeById(id: Long): Flow<Project?> = dao.observeById(id).map { it?.toDomain(json) }

        override suspend fun create(
            name: String,
            comment: String?,
            crsPresetId: String,
        ): Project {
            val now = System.currentTimeMillis()
            // PRJ-002 MVP: name only — CRS picker (PRJ-003) lands in Sprint 2.
            // Until then, default to WGS-84 geographic, ellipsoidal heights, no geoid.
            val defaultCrs =
                CrsConfig(
                    presetId = crsPresetId,
                    geoid = GeoidConfig.None,
                    heightMode = HeightMode.ELLIPSOIDAL,
                )
            val entity =
                ProjectEntity(
                    name = name,
                    comment = comment,
                    crsConfigJson = json.encodeToString(CrsConfig.serializer(), defaultCrs),
                    createdAtUtc = now,
                    updatedAtUtc = now,
                )
            val newId = dao.insert(entity)
            return dao.byId(newId)?.toDomain(json)
                ?: error("Project insert(id=$newId) returned no row on read-back")
        }

        override suspend fun rename(
            id: Long,
            newName: String,
        ) {
            val current = dao.byId(id) ?: return
            dao.update(current.copy(name = newName, updatedAtUtc = System.currentTimeMillis()))
        }

        override suspend fun setCrs(id: Long, presetId: String) {
            val current = dao.byId(id) ?: return
            val cfg = json.decodeFromString(CrsConfig.serializer(), current.crsConfigJson)
                .copy(presetId = presetId)
            dao.update(
                current.copy(
                    crsConfigJson = json.encodeToString(CrsConfig.serializer(), cfg),
                    updatedAtUtc = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun setCalibration(id: Long, calibration: CalibrationConfig?) {
            val current = dao.byId(id) ?: return
            val cfg = json.decodeFromString(CrsConfig.serializer(), current.crsConfigJson)
                .copy(calibration = calibration)
            dao.update(
                current.copy(
                    crsConfigJson = json.encodeToString(CrsConfig.serializer(), cfg),
                    updatedAtUtc = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun delete(id: Long) = dao.deleteById(id)

        override suspend fun copy(
            id: Long,
            newName: String,
        ): Project {
            val source = dao.byId(id) ?: error("Cannot copy: project id=$id not found")
            val now = System.currentTimeMillis()
            val cloneId =
                dao.insert(
                    source.copy(
                        projectId = 0,
                        name = newName,
                        createdAtUtc = now,
                        updatedAtUtc = now,
                    ),
                )
            return dao.byId(cloneId)?.toDomain(json)
                ?: error("Project copy(id=$cloneId) returned no row on read-back")
        }
    }
