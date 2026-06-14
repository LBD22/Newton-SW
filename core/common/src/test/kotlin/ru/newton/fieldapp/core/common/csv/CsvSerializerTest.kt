package ru.newton.fieldapp.core.common.csv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CsvSerializerTest {
    @Test
    fun `round-trips a plain header CSV`() {
        val rows = listOf(
            CsvRow(name = "P1", n = 1234.567, e = 7654.321, h = 12.3, code = "tree"),
            CsvRow(name = "P2", n = 1235.0, e = 7655.0, h = 13.0),
        )
        val text = CsvSerializer.write(rows.asSequence())
        val parsed = CsvSerializer.parse(text)
        assertTrue(parsed.issues.isEmpty()) { "unexpected issues: ${parsed.issues}" }
        assertEquals(rows, parsed.rows)
    }

    @Test
    fun `round-trips Excel-RU format with semicolon delimiter and comma decimals`() {
        val rows = listOf(CsvRow(name = "Mark", n = 1234.5, e = 6789.0, h = -1.25, code = "wall"))
        val text = CsvSerializer.write(rows.asSequence(), CsvFormat.EXCEL_RU)
        // Sanity: produced text uses ; and , and no .
        assertTrue(text.contains(';')) { "expected semicolon delimiter, got: $text" }
        assertTrue(text.contains(','))
        assertTrue(!text.contains('.'))
        val parsed = CsvSerializer.parse(text, CsvFormat.EXCEL_RU)
        assertEquals(rows, parsed.rows)
    }

    @Test
    fun `default dot-decimal export stays dot-decimal under ru_RU locale`() {
        val saved = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ru-RU"))
            val rows = listOf(CsvRow(name = "P1", n = 1234.5678, e = 7654.321, h = 12.3, code = "tree"))
            val text = CsvSerializer.write(rows.asSequence())
            // The numeric cells must use a dot — a comma would inject the default
            // delimiter and shift every column. Round-trip must also survive.
            assertTrue(text.contains("1234.5678")) { "expected dot decimal, got: $text" }
            val parsed = CsvSerializer.parse(text)
            assertTrue(parsed.issues.isEmpty()) { "unexpected issues: ${parsed.issues}" }
            assertEquals(rows, parsed.rows)
        } finally {
            java.util.Locale.setDefault(saved)
        }
    }

    @Test
    fun `parses a 1000-point round-trip with diff zero`() {
        // Synthesises 1000 deterministic points, writes them, re-parses, compares.
        val rows = List(1000) { i ->
            CsvRow(
                name = "P${i + 1}",
                n = 1000.0 + i * 0.5,
                e = 5_000.0 + i * 0.25,
                h = (i % 100) * 0.1,
                code = if (i % 10 == 0) "stake" else null,
            )
        }
        val text = CsvSerializer.write(rows.asSequence())
        val parsed = CsvSerializer.parse(text)
        assertTrue(parsed.issues.isEmpty()) { "issues: ${parsed.issues.size}" }
        assertEquals(rows.size, parsed.rows.size)
        for ((expected, actual) in rows.zip(parsed.rows)) {
            assertEquals(expected.name, actual.name)
            assertEquals(expected.n, actual.n, 1.0e-3)
            assertEquals(expected.e, actual.e, 1.0e-3)
            assertEquals(expected.h, actual.h, 1.0e-3)
            assertEquals(expected.code, actual.code)
        }
    }

    @Test
    fun `bad rows surface as issues without aborting the whole file`() {
        val text =
            """
            name,n,e,h,code
            Good,100.0,200.0,1.0,
            BadN,abc,200.0,1.0,
            ,100.0,200.0,1.0,
            Good2,101.0,201.0,2.0,marker
            """.trimIndent()
        val parsed = CsvSerializer.parse(text)
        assertEquals(2, parsed.rows.size)
        assertEquals("Good", parsed.rows[0].name)
        assertEquals("Good2", parsed.rows[1].name)
        assertEquals(2, parsed.issues.size)
    }

    @Test
    fun `quoted fields with embedded delimiter are preserved`() {
        // Built piecewise — triple-quoted strings can't safely host literal `"""`.
        val q = "\""
        val text = "name,n,e,h,code\n" +
            "${q}P, complicated$q,100.0,200.0,1.0,${q}with ${q}${q}quotes${q}${q}$q"
        val parsed = CsvSerializer.parse(text)
        assertTrue(parsed.issues.isEmpty())
        assertEquals(1, parsed.rows.size)
        assertEquals("P, complicated", parsed.rows[0].name)
        assertEquals("with \"quotes\"", parsed.rows[0].code)
    }

    @Test
    fun `headerless format reads in column order`() {
        val format = CsvFormat(hasHeader = false)
        val text = "P1,1.0,2.0,3.0,code1\nP2,1.5,2.5,3.5,code2"
        val parsed = CsvSerializer.parse(text, format)
        assertTrue(parsed.issues.isEmpty())
        assertEquals(2, parsed.rows.size)
        assertEquals("P1", parsed.rows[0].name)
        assertEquals(1.0, parsed.rows[0].n)
        assertEquals("code1", parsed.rows[0].code)
    }
}
