package ru.newton.fieldapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.newton.fieldapp.data.db.dao.LayerDao
import ru.newton.fieldapp.data.db.entity.LayerEntity
import ru.newton.fieldapp.domain.model.Layer
import ru.newton.fieldapp.domain.model.NewLayer
import ru.newton.fieldapp.domain.repository.LayerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LayerRepositoryImpl
    @Inject
    constructor(
        private val dao: LayerDao,
    ) : LayerRepository {
        override fun observeByProject(projectId: Long): Flow<List<Layer>> =
            dao.observeByProject(projectId).map { rows -> rows.map { it.toDomain() } }

        override suspend fun byId(id: Long): Layer? = dao.byId(id)?.toDomain()

        override suspend fun byName(projectId: Long, name: String): Layer? =
            dao.byName(projectId, name)?.toDomain()

        override suspend fun create(layer: NewLayer): Layer {
            val now = System.currentTimeMillis()
            val entity = LayerEntity(
                projectId = layer.projectId,
                name = layer.name.trim(),
                colorRgb = layer.colorRgb,
                visible = layer.visible,
                createdAtUtc = now,
            )
            val id = dao.insert(entity)
            return if (id == -1L) {
                // Conflict → row already exists; return the existing one so
                // the importer doesn't need to special-case duplicates.
                dao.byName(layer.projectId, entity.name)?.toDomain()
                    ?: error("Insert returned -1 but lookup also failed for '${layer.name}'")
            } else {
                entity.copy(layerId = id).toDomain()
            }
        }

        override suspend fun update(layer: Layer) {
            dao.update(
                LayerEntity(
                    layerId = layer.id,
                    projectId = layer.projectId,
                    name = layer.name.trim(),
                    colorRgb = layer.colorRgb,
                    visible = layer.visible,
                    createdAtUtc = layer.createdAtUtc,
                ),
            )
        }

        override suspend fun delete(id: Long) = dao.deleteById(id)

        override suspend fun ensure(projectId: Long, name: String): Layer {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) error("Имя слоя не может быть пустым")
            return dao.byName(projectId, trimmed)?.toDomain()
                ?: create(NewLayer(projectId = projectId, name = trimmed))
        }

        private fun LayerEntity.toDomain() = Layer(
            id = layerId,
            projectId = projectId,
            name = name,
            colorRgb = colorRgb,
            visible = visible,
            createdAtUtc = createdAtUtc,
        )
    }
