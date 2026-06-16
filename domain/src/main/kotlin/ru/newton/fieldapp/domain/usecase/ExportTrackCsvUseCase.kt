package ru.newton.fieldapp.domain.usecase

import ru.newton.fieldapp.domain.repository.TrackRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Export one track-recording session as CSV. Coordinates are in the project CRS
 * (n/e/h) — the same frame surveyed points are stored in — so the file imports
 * straight into the office GIS/CAD that already knows the project's system.
 *
 * Schema (semicolon-separated, dot decimals):
 * ```
 * idx;n;e;h;fix;time_utc
 * ```
 */
class ExportTrackCsvUseCase
    @Inject
    constructor(
        private val trackRepository: TrackRepository,
    ) {
        suspend operator fun invoke(sessionId: Long): String {
            val points = trackRepository.pointsForSession(sessionId)
            return buildString {
                appendLine("idx;n;e;h;fix;time_utc")
                points.forEachIndexed { i, p ->
                    appendLine(
                        listOf(
                            (i + 1).toString(),
                            format(p.n),
                            format(p.e),
                            format(p.h),
                            p.fixQuality,
                            isoUtc(p.timestampUtc),
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
