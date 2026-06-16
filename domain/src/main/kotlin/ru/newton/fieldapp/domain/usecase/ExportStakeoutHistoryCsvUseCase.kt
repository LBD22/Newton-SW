package ru.newton.fieldapp.domain.usecase

import kotlinx.coroutines.flow.first
import ru.newton.fieldapp.domain.repository.StakeoutResultRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Export the stake-out history for a project as a QA control sheet: target vs
 * actual coordinates and the residuals the surveyor achieved.
 *
 * Schema (semicolon-separated, dot decimals):
 * ```
 * target;mode;targetN;targetE;targetH;actualN;actualE;actualH;dHoriz;dVert;time_utc
 * ```
 */
class ExportStakeoutHistoryCsvUseCase
    @Inject
    constructor(
        private val repository: StakeoutResultRepository,
    ) {
        suspend operator fun invoke(projectId: Long): String {
            val results = repository.observeByProject(projectId).first()
            return buildString {
                appendLine("target;mode;targetN;targetE;targetH;actualN;actualE;actualH;dHoriz;dVert;time_utc")
                for (r in results) {
                    appendLine(
                        listOf(
                            r.targetLabel,
                            r.mode.name,
                            format(r.targetN),
                            format(r.targetE),
                            format(r.targetH),
                            format(r.actualN),
                            format(r.actualE),
                            format(r.actualH),
                            format(r.deltaHorizontalM),
                            format(r.deltaVerticalM),
                            isoUtc(r.savedAtUtc),
                        ).joinToString(";"),
                    )
                }
            }
        }

        private fun format(v: Double): String = String.format(Locale.US, "%.4f", v)

        private fun isoUtc(ms: Long): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date(ms))
    }
