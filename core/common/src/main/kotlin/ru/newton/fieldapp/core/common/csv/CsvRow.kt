package ru.newton.fieldapp.core.common.csv

/**
 * Decoded CSV row. Geometry fields are mandatory; metadata fields are nullable
 * (an absent column is represented as `null`, an empty cell also as `null`).
 *
 * Lives in `:core:common` so both `:domain` use-cases and `:features:*` UI
 * (preview, error messages) can speak in the same shape.
 */
data class CsvRow(
    val name: String,
    val n: Double,
    val e: Double,
    val h: Double,
    val code: String? = null,
    val layer: String? = null,
    val externalRef: String? = null,
)

/** What went wrong on a specific input row, with enough detail to surface to the user. */
data class CsvParseIssue(
    val lineNumber: Int,
    val rawLine: String,
    val message: String,
)

/** Outcome of a [CsvSerializer.parse] run. */
data class CsvParseResult(
    val rows: List<CsvRow>,
    val issues: List<CsvParseIssue>,
)
