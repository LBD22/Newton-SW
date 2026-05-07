package ru.newton.fieldapp.core.common.csv

/**
 * CSV serialisation configuration shared between import and export.
 *
 * Russian field exports often use `;` as the delimiter (Excel default for ru_RU
 * locale), so this is configurable per project / per file template. The
 * `decimalSeparator` covers the same ru_RU locale concern: `,` is common in
 * legacy data, `.` is required for round-tripping with most engineering tools.
 */
data class CsvFormat(
    val delimiter: Char = ',',
    val decimalSeparator: Char = '.',
    val hasHeader: Boolean = true,
    /**
     * Order of columns produced by export and expected by import (when there is
     * no header). Must contain at least the four geometry fields: `name`, `n`,
     * `e`, `h`.
     */
    val columns: List<CsvColumn> = listOf(
        CsvColumn.Name,
        CsvColumn.N,
        CsvColumn.E,
        CsvColumn.H,
        CsvColumn.Code,
    ),
) {
    init {
        require(CsvColumn.Name in columns) { "CSV columns must include Name" }
        require(CsvColumn.N in columns) { "CSV columns must include N" }
        require(CsvColumn.E in columns) { "CSV columns must include E" }
        require(CsvColumn.H in columns) { "CSV columns must include H" }
    }

    companion object {
        val DEFAULT: CsvFormat = CsvFormat()

        /** Excel-RU defaults: `;` delimiter, `,` decimal separator, header row. */
        val EXCEL_RU: CsvFormat = CsvFormat(delimiter = ';', decimalSeparator = ',')
    }
}

/** Logical column in a CSV row. Order in [CsvFormat.columns] sets the wire layout. */
enum class CsvColumn(val header: String) {
    Name("name"),
    Code("code"),
    Layer("layer"),
    N("n"),
    E("e"),
    H("h"),
    ExternalRef("external_ref"),
}
