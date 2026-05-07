package ru.newton.fieldapp.domain.usecase

import kotlinx.coroutines.flow.first
import ru.newton.fieldapp.domain.repository.PointRepository
import java.util.Locale
import javax.inject.Inject

/**
 * J7 acceptance — re-survey delta report.
 *
 * Groups points by name and emits one CSV row per (rev_first, rev_last) pair
 * with N/E/H of each plus the deltas. Names that exist only once (no
 * re-measure) are excluded — the report is specifically about *changes*.
 *
 * Output schema (semicolon-separated, ru-RU friendly):
 * ```
 * name;rev_first;N0;E0;H0;rev_last;N1;E1;H1;dN;dE;dH;dPlanar
 * ```
 *
 * Distance/time isn't dropped — `dPlanar` is sqrt(dN² + dE²), the most
 * useful single-number summary for a control sheet.
 */
class ExportResurveyDeltaCsvUseCase
    @Inject
    constructor(
        private val pointRepository: PointRepository,
    ) {
        suspend operator fun invoke(projectId: Long): String {
            val points = pointRepository.observePoints(projectId).first()
            val byName = points.groupBy { it.name }
                .filterValues { it.size >= 2 }
            return buildString {
                appendLine("name;rev_first;N0;E0;H0;rev_last;N1;E1;H1;dN;dE;dH;dPlanar")
                for ((name, revisions) in byName) {
                    val sorted = revisions.sortedBy { it.revision }
                    val first = sorted.first()
                    val last = sorted.last()
                    val dN = last.n - first.n
                    val dE = last.e - first.e
                    val dH = last.h - first.h
                    val dPlanar = kotlin.math.hypot(dN, dE)
                    val row = listOf(
                        name,
                        first.revision.toString(),
                        format(first.n),
                        format(first.e),
                        format(first.h),
                        last.revision.toString(),
                        format(last.n),
                        format(last.e),
                        format(last.h),
                        format(dN),
                        format(dE),
                        format(dH),
                        format(dPlanar),
                    ).joinToString(";")
                    appendLine(row)
                }
            }
        }

        /** Force `.` decimal separator regardless of system locale. */
        private fun format(v: Double): String = String.format(Locale.US, "%.4f", v)
    }
