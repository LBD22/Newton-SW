package ru.newton.fieldapp.domain.usecase

import kotlinx.coroutines.flow.first
import ru.newton.fieldapp.core.common.csv.CsvFormat
import ru.newton.fieldapp.core.common.csv.CsvRow
import ru.newton.fieldapp.core.common.csv.CsvSerializer
import ru.newton.fieldapp.domain.repository.PointRepository
import javax.inject.Inject

/**
 * Reads points for [projectId] and renders them as a CSV string ready to
 * stream out via SAF. Pure formatting — no IO, no Android types.
 */
class ExportPointsToCsvUseCase
    @Inject
    constructor(
        private val pointRepository: PointRepository,
    ) {
        suspend operator fun invoke(
            projectId: Long,
            format: CsvFormat = CsvFormat.DEFAULT,
        ): String {
            val points = pointRepository.observePoints(projectId).first()
            val rows = points.map { p ->
                CsvRow(
                    name = p.name,
                    n = p.n,
                    e = p.e,
                    h = p.h,
                    code = p.code,
                    externalRef = p.externalRef,
                )
            }
            return CsvSerializer.write(rows.asSequence(), format)
        }
    }
