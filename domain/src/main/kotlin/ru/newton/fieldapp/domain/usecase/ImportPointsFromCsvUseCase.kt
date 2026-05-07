package ru.newton.fieldapp.domain.usecase

import ru.newton.fieldapp.core.common.csv.CsvColumn
import ru.newton.fieldapp.core.common.csv.CsvFormat
import ru.newton.fieldapp.core.common.csv.CsvParseIssue
import ru.newton.fieldapp.core.common.csv.CsvParseResult
import ru.newton.fieldapp.core.common.csv.CsvSerializer
import ru.newton.fieldapp.domain.model.NewPoint
import ru.newton.fieldapp.domain.model.PointSource
import ru.newton.fieldapp.domain.repository.PointRepository
import javax.inject.Inject

/**
 * Imports points from a CSV [content] string into [projectId].
 *
 * Returns a summary the UI surfaces to the user — count of saved rows and
 * a list of skipped rows with explanations. The use-case does NOT throw on
 * parse errors; surveyors expect to fix and re-run.
 */
class ImportPointsFromCsvUseCase
    @Inject
    constructor(
        private val pointRepository: PointRepository,
    ) {
        suspend operator fun invoke(
            projectId: Long,
            content: String,
            format: CsvFormat = CsvFormat.DEFAULT,
        ): Result = persist(projectId, CsvSerializer.parse(content, format))

        /**
         * PRJ-013 — explicit mapping path. The wizard hands us a list saying
         * which CSV column goes to which [CsvColumn]; we bypass header
         * auto-detection entirely.
         */
        suspend fun importWithMapping(
            projectId: Long,
            content: String,
            mapping: List<CsvColumn?>,
            delimiter: Char,
            decimalSeparator: Char,
            hasHeader: Boolean = true,
        ): Result = persist(
            projectId,
            CsvSerializer.parseWithMapping(
                content = content,
                delimiter = delimiter,
                decimalSeparator = decimalSeparator,
                mapping = mapping,
                hasHeader = hasHeader,
            ),
        )

        private suspend fun persist(projectId: Long, parsed: CsvParseResult): Result {
            var saved = 0
            for (row in parsed.rows) {
                pointRepository.save(
                    NewPoint(
                        projectId = projectId,
                        name = row.name,
                        code = row.code,
                        layerId = null, // resolution by layer name lands when LayerRepository ships
                        n = row.n,
                        e = row.e,
                        h = row.h,
                        source = PointSource.IMPORT,
                        externalRef = row.externalRef,
                    ),
                )
                saved++
            }
            return Result(saved = saved, issues = parsed.issues)
        }

        data class Result(val saved: Int, val issues: List<CsvParseIssue>)
    }
