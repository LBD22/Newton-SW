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
            includeQuality: Boolean = true,
        ): String {
            val points = pointRepository.observePoints(projectId).first()
            val observations = if (includeQuality) {
                pointRepository.observationsByProject(projectId)
            } else {
                emptyMap()
            }
            val rows = points.map { p ->
                val obs = observations[p.id]
                CsvRow(
                    name = p.name,
                    n = p.n,
                    e = p.e,
                    h = p.h,
                    code = p.code,
                    externalRef = p.externalRef,
                    fixType = obs?.fixType,
                    sigmaPlanM = obs?.let { horizontalSigma(it.sigmaN, it.sigmaE) },
                    sigmaHM = obs?.sigmaH,
                    epochs = obs?.epochs,
                )
            }
            // Append quality columns only when requested, so the default export
            // shape is unchanged for callers that don't want them.
            val effectiveFormat = if (includeQuality) {
                format.copy(columns = format.columns + CsvFormat.QUALITY_COLUMNS)
            } else {
                format
            }
            return CsvSerializer.write(rows.asSequence(), effectiveFormat)
        }

        private fun horizontalSigma(sigmaN: Double?, sigmaE: Double?): Double? {
            if (sigmaN == null || sigmaE == null) return null
            return kotlin.math.hypot(sigmaN, sigmaE)
        }
    }
