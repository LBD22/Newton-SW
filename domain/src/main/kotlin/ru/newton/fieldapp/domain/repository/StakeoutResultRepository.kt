package ru.newton.fieldapp.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.domain.model.StakeoutMode
import ru.newton.fieldapp.domain.model.StakeoutResult

interface StakeoutResultRepository {
    fun observeByProject(projectId: Long): Flow<List<StakeoutResult>>
    suspend fun record(
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
    ): Long
    suspend fun delete(id: Long)
}
