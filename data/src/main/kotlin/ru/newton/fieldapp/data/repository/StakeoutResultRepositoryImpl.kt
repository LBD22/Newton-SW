package ru.newton.fieldapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.newton.fieldapp.data.db.dao.StakeoutResultDao
import ru.newton.fieldapp.data.db.entity.StakeoutResultEntity
import ru.newton.fieldapp.domain.model.StakeoutMode
import ru.newton.fieldapp.domain.model.StakeoutResult
import ru.newton.fieldapp.domain.repository.StakeoutResultRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StakeoutResultRepositoryImpl
    @Inject
    constructor(
        private val dao: StakeoutResultDao,
    ) : StakeoutResultRepository {
        override fun observeByProject(projectId: Long): Flow<List<StakeoutResult>> =
            dao.observeByProject(projectId).map { rows -> rows.map { it.toDomain() } }

        override suspend fun record(
            projectId: Long,
            targetPointId: Long?,
            targetLabel: String,
            mode: StakeoutMode,
            targetN: Double,
            targetE: Double,
            targetH: Double,
            actualN: Double,
            actualE: Double,
            actualH: Double,
            deltaHorizontalM: Double,
            deltaVerticalM: Double,
        ): Long = dao.insert(
            StakeoutResultEntity(
                projectId = projectId,
                targetPointId = targetPointId,
                targetLabel = targetLabel,
                mode = mode.name,
                targetN = targetN,
                targetE = targetE,
                targetH = targetH,
                actualN = actualN,
                actualE = actualE,
                actualH = actualH,
                deltaHorizontalM = deltaHorizontalM,
                deltaVerticalM = deltaVerticalM,
                savedAtUtc = System.currentTimeMillis(),
            ),
        )

        override suspend fun delete(id: Long) = dao.deleteById(id)

        private fun StakeoutResultEntity.toDomain(): StakeoutResult = StakeoutResult(
            id = resultId,
            projectId = projectId,
            targetPointId = targetPointId,
            targetLabel = targetLabel,
            mode = StakeoutMode.valueOf(mode),
            targetN = targetN,
            targetE = targetE,
            targetH = targetH,
            actualN = actualN,
            actualE = actualE,
            actualH = actualH,
            deltaHorizontalM = deltaHorizontalM,
            deltaVerticalM = deltaVerticalM,
            savedAtUtc = savedAtUtc,
        )
    }
