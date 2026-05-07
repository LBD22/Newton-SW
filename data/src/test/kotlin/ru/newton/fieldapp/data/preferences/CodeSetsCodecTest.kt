package ru.newton.fieldapp.data.preferences

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CodeSetsCodecTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decode round-trips through encode`() {
        val sets = listOf(
            CodeSet(name = "Топосъёмка", codes = listOf("BRD", "POL", "WAL")),
            CodeSet(name = "Стройка", codes = emptyList()),
        )

        val raw = encodeSets(json, sets)
        assertEquals(sets, decodeSets(json, raw))
    }

    @Test
    fun `decode returns empty list on null input`() {
        assertEquals(emptyList<CodeSet>(), decodeSets(json, null))
    }

    @Test
    fun `decode returns empty list on blank input`() {
        assertEquals(emptyList<CodeSet>(), decodeSets(json, "   "))
    }

    @Test
    fun `decode swallows malformed JSON instead of crashing`() {
        // PRJ-020/021 — a bad blob in DataStore must not propagate.
        assertEquals(emptyList<CodeSet>(), decodeSets(json, "{not even json"))
    }

    @Test
    fun `decode swallows schema mismatch`() {
        // Wrong shape — array of objects with unrelated fields. Saved sets are
        // optional surveyor state; we lose them silently rather than crash.
        assertEquals(emptyList<CodeSet>(), decodeSets(json, """[{"foo":"bar"}]"""))
    }

    @Test
    fun `mergeSet appends when name is new`() {
        val existing = listOf(CodeSet("Roads", listOf("R1")))
        val merged = mergeSet(existing, CodeSet("Buildings", listOf("B1")))
        assertEquals(listOf("Roads", "Buildings"), merged.map { it.name })
    }

    @Test
    fun `mergeSet replaces in place on case-insensitive name match`() {
        val existing = listOf(
            CodeSet("Roads", listOf("R1")),
            CodeSet("Buildings", listOf("B1")),
        )
        val merged = mergeSet(existing, CodeSet("ROADS", listOf("X", "Y")))
        assertEquals(2, merged.size)
        assertEquals("ROADS", merged[0].name)
        assertEquals(listOf("X", "Y"), merged[0].codes)
        assertEquals("Buildings", merged[1].name)
    }

    @Test
    fun `removeSet drops by case-insensitive name`() {
        val existing = listOf(
            CodeSet("Roads", listOf("R1")),
            CodeSet("Buildings", listOf("B1")),
        )
        assertEquals(listOf("Buildings"), removeSet(existing, "roads").map { it.name })
    }

    @Test
    fun `removeSet leaves list unchanged when name is unknown`() {
        val existing = listOf(CodeSet("Roads", listOf("R1")))
        assertEquals(existing, removeSet(existing, "Pipes"))
    }
}
