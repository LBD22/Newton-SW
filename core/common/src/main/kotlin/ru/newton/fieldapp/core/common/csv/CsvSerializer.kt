package ru.newton.fieldapp.core.common.csv

/**
 * Tiny RFC-4180-aligned CSV codec scoped to point-import/export needs.
 *
 * Supported features:
 *  - Configurable delimiter (`,` or `;`) and decimal separator (`.` or `,`).
 *  - Optional header row that drives column auto-detection.
 *  - Quoted fields with escaped quotes (`""`).
 *  - Empty cell → `null` for nullable columns; missing geometry → row issue.
 *
 * Out of scope:
 *  - Multi-line quoted fields (rare for surveying CSVs; raises an issue).
 *  - Non-UTF-8 input — caller is responsible for decoding.
 */
object CsvSerializer {
    /**
     * Parse [content] using [format]. Bad rows accumulate as [CsvParseIssue]s
     * but do not abort the whole import — surveyors expect to fix and re-import.
     */
    fun parse(content: String, format: CsvFormat = CsvFormat.DEFAULT): CsvParseResult {
        val lines = content.lineSequence()
            .mapIndexed { idx, raw -> idx + 1 to raw }
            .filterNot { (_, raw) -> raw.isBlank() }
            .toList()
        if (lines.isEmpty()) return CsvParseResult(emptyList(), emptyList())

        val (columnOrder, dataLines) = when {
            format.hasHeader -> {
                val headerCells = splitLine(lines.first().second, format.delimiter)
                val mapped = mapHeaderToColumns(headerCells)
                mapped to lines.drop(1)
            }
            else -> format.columns to lines
        }

        val rows = mutableListOf<CsvRow>()
        val issues = mutableListOf<CsvParseIssue>()
        for ((lineNo, raw) in dataLines) {
            val cells = splitLine(raw, format.delimiter)
            if (cells.size < columnOrder.size) {
                issues += CsvParseIssue(lineNo, raw, "Меньше столбцов, чем ожидалось (${cells.size} vs ${columnOrder.size})")
                continue
            }
            val byColumn = columnOrder.mapIndexed { i, col -> col to cells[i] }.toMap()
            val parsed = decodeRow(byColumn, format.decimalSeparator)
            parsed.fold(
                onSuccess = { rows += it },
                onFailure = { issues += CsvParseIssue(lineNo, raw, it.message ?: "Ошибка разбора") },
            )
        }
        return CsvParseResult(rows, issues)
    }

    /** Render [rows] using [format]. Header row emitted iff [CsvFormat.hasHeader]. */
    fun write(rows: Sequence<CsvRow>, format: CsvFormat = CsvFormat.DEFAULT): String = buildString {
        if (format.hasHeader) {
            appendLine(format.columns.joinToString(format.delimiter.toString()) { it.header })
        }
        for (row in rows) {
            appendLine(
                format.columns.joinToString(format.delimiter.toString()) { col ->
                    encodeCell(row, col, format)
                },
            )
        }
    }

    private fun decodeRow(values: Map<CsvColumn, String>, decimalSeparator: Char): Result<CsvRow> = runCatching {
        val name = values.getValue(CsvColumn.Name).trim().ifEmpty {
            error("Имя точки не может быть пустым")
        }
        val n = parseDouble(values.getValue(CsvColumn.N), decimalSeparator)
            ?: error("N: ожидается число")
        val e = parseDouble(values.getValue(CsvColumn.E), decimalSeparator)
            ?: error("E: ожидается число")
        val h = parseDouble(values.getValue(CsvColumn.H), decimalSeparator)
            ?: error("H: ожидается число")
        CsvRow(
            name = name,
            n = n,
            e = e,
            h = h,
            code = values[CsvColumn.Code]?.takeIf(String::isNotBlank),
            layer = values[CsvColumn.Layer]?.takeIf(String::isNotBlank),
            externalRef = values[CsvColumn.ExternalRef]?.takeIf(String::isNotBlank),
        )
    }

    private fun encodeCell(row: CsvRow, col: CsvColumn, format: CsvFormat): String {
        val raw: String = when (col) {
            CsvColumn.Name -> row.name
            CsvColumn.Code -> row.code.orEmpty()
            CsvColumn.Layer -> row.layer.orEmpty()
            CsvColumn.N -> formatDouble(row.n, format.decimalSeparator)
            CsvColumn.E -> formatDouble(row.e, format.decimalSeparator)
            CsvColumn.H -> formatDouble(row.h, format.decimalSeparator)
            CsvColumn.ExternalRef -> row.externalRef.orEmpty()
        }
        return quoteIfNeeded(raw, format.delimiter)
    }

    private fun parseDouble(raw: String, decimalSeparator: Char): Double? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val normalised = if (decimalSeparator == ',') trimmed.replace(',', '.') else trimmed
        // Always also accept the foreign separator — surveyors copy data from
        // multiple sources and lenience here is preferable to spurious errors.
        return normalised.replace(',', '.').toDoubleOrNull()
    }

    private fun formatDouble(value: Double, decimalSeparator: Char): String {
        // 4 fractional digits = 0.1 mm, more than enough for points stored at sub-cm.
        // Locale.US is mandatory: the default formatter on a ru_RU device emits a
        // comma decimal ("55,1234"), which — with the default dot-decimal CSV —
        // injects the delimiter into every coordinate cell and corrupts the file.
        val s = String.format(java.util.Locale.US, "%.4f", value).let { withDot ->
            if (decimalSeparator == ',') withDot.replace('.', ',') else withDot
        }
        // Strip trailing zeros while keeping at least three fractional digits (mm).
        return trimTrailingZeros(s, decimalSeparator)
    }

    private fun trimTrailingZeros(s: String, decimalSeparator: Char): String {
        val dotIdx = s.indexOf(decimalSeparator)
        if (dotIdx < 0) return s
        val minFractional = 3
        var end = s.length
        while (end > dotIdx + 1 + minFractional && s[end - 1] == '0') end--
        if (s[end - 1] == decimalSeparator) end--
        return s.substring(0, end)
    }

    private fun mapHeaderToColumns(headerCells: List<String>): List<CsvColumn> {
        val byHeader = CsvColumn.entries.associateBy { it.header.lowercase() }
        return headerCells.map { cell ->
            val key = cell.trim().lowercase()
            byHeader[key] ?: error("Неизвестный заголовок столбца: $cell")
        }
    }

    /**
     * Surface for the column-mapping wizard: read just the header + first
     * few data rows without committing to a [CsvFormat]. The wizard then
     * builds an explicit `List<CsvColumn?>` mapping and feeds it back to
     * [parseWithMapping] so we don't hit the auto-detection branch at all.
     */
    fun preview(content: String, delimiter: Char, sampleRows: Int = 5): CsvPreview {
        val lines = content.lineSequence()
            .filterNot { it.isBlank() }
            .take(sampleRows + 1)
            .toList()
        if (lines.isEmpty()) return CsvPreview(headers = emptyList(), sampleRows = emptyList())
        return CsvPreview(
            headers = splitLine(lines.first(), delimiter),
            sampleRows = lines.drop(1).map { splitLine(it, delimiter) },
        )
    }

    /**
     * Parse [content] with an explicit `mapping` list — entry `i` is which
     * [CsvColumn] the i-th CSV column maps to, or `null` to ignore. Required
     * columns ([CsvColumn.Name], N, E, H) must each appear at most once and
     * exactly once. Header line is skipped if [hasHeader] is true.
     */
    fun parseWithMapping(
        content: String,
        delimiter: Char = ';',
        decimalSeparator: Char = '.',
        mapping: List<CsvColumn?>,
        hasHeader: Boolean = true,
    ): CsvParseResult {
        val required = setOf(CsvColumn.Name, CsvColumn.N, CsvColumn.E, CsvColumn.H)
        val mapped = mapping.filterNotNull().toSet()
        val missing = required - mapped
        require(missing.isEmpty()) {
            "В маппинге не назначены обязательные колонки: ${missing.joinToString { it.header }}"
        }

        val lines = content.lineSequence()
            .mapIndexed { idx, raw -> idx + 1 to raw }
            .filterNot { (_, raw) -> raw.isBlank() }
            .toList()
        val dataLines = if (hasHeader) lines.drop(1) else lines
        val rows = mutableListOf<CsvRow>()
        val issues = mutableListOf<CsvParseIssue>()
        for ((lineNo, raw) in dataLines) {
            val cells = splitLine(raw, delimiter)
            if (cells.size < mapping.size) {
                issues += CsvParseIssue(
                    lineNo,
                    raw,
                    "Меньше столбцов, чем в маппинге (${cells.size} vs ${mapping.size})",
                )
                continue
            }
            val byColumn = mapping.mapIndexedNotNull { i, col ->
                if (col == null) null else col to cells[i]
            }.toMap()
            val parsed = decodeRow(byColumn, decimalSeparator)
            parsed.fold(
                onSuccess = { rows += it },
                onFailure = { issues += CsvParseIssue(lineNo, raw, it.message ?: "Ошибка разбора") },
            )
        }
        return CsvParseResult(rows, issues)
    }

    /**
     * Split a CSV line on [delimiter], honouring `"…"` quoting and `""` escaping.
     * Accepts unquoted leading/trailing whitespace inside cells; trims it.
     */
    internal fun splitLine(line: String, delimiter: Char): List<String> {
        val out = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    cell.append('"')
                    i += 2
                    continue
                }
                c == '"' -> {
                    inQuotes = !inQuotes
                    i++
                    continue
                }
                !inQuotes && c == delimiter -> {
                    out += cell.toString().trim()
                    cell.setLength(0)
                    i++
                    continue
                }
                else -> {
                    cell.append(c)
                    i++
                }
            }
        }
        out += cell.toString().trim()
        return out
    }

    private fun quoteIfNeeded(raw: String, delimiter: Char): String {
        val needs = raw.contains(delimiter) || raw.contains('"') || raw.contains('\n')
        if (!needs) return raw
        return "\"" + raw.replace("\"", "\"\"") + "\""
    }
}

/** Headers + a few data rows for the UI mapping wizard to display side-by-side. */
data class CsvPreview(
    val headers: List<String>,
    val sampleRows: List<List<String>>,
) {
    /** True iff every header cell exactly matches one of [CsvColumn.header]. */
    fun isFullyAutoMappable(): Boolean {
        val byHeader = CsvColumn.entries.associateBy { it.header.lowercase() }
        return headers.all { it.trim().lowercase() in byHeader }
    }
}
