package ru.newton.fieldapp.crs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CrsPresetsTest {
    @Test
    fun `parse round-trips all canonical preset IDs`() {
        val cases = listOf(
            Crs.Wgs84Geo,
            Crs.Gsk2011Geo,
            Crs.WebMercator,
            Crs.Wgs84Utm(zone = 36, northern = true),
            Crs.Wgs84Utm(zone = 36, northern = false),
            Crs.Gsk2011Gk(zone = 8),
            Crs.Sk42Gk(zone = 7),
            Crs.Sk95Gk(zone = 12),
        )
        for (input in cases) {
            val parsed = CrsPresets.parse(input.presetId)
            assertNotNull(parsed) { "Failed to parse ${input.presetId}" }
            assertEquals(input.presetId, parsed!!.presetId)
        }
    }

    @Test
    fun `parse returns null for unknown ids`() {
        assertNull(CrsPresets.parse("FOO_BAR"))
        assertNull(CrsPresets.parse(""))
        assertNull(CrsPresets.parse("WGS84_UTM_99N"))   // out-of-range zone
        assertNull(CrsPresets.parse("WGS84_UTM_36X"))   // invalid hemisphere
        assertNull(CrsPresets.parse("GSK2011_GK_99"))   // out-of-range zone
        assertNull(CrsPresets.parse("GSK2011_GK_abc"))  // non-numeric zone
    }

    @Test
    fun `MVP catalogue contains expected anchor entries`() {
        val ids = CrsPresets.mvpCatalogue.map { it.presetId }
        assert("WGS84_GEO" in ids)
        assert("GSK2011_GEO" in ids)
        assert("WGS84_UTM_36N" in ids)
        assert("GSK2011_GK_8" in ids)
        assert("SK42_GK_8" in ids)
        assert("SK95_GK_8" in ids)
    }

    @Test
    fun `Gsk2011 GK zone 8 has correct central meridian and false easting`() {
        val crs = Crs.Gsk2011Gk(zone = 8)
        assertEquals(45.0, crs.centralMeridianDeg, 1.0e-12)
        assertEquals(8_500_000.0, crs.falseEastingM, 1.0e-9)
        assertEquals(1.0, crs.scaleOnCentralMeridian, 1.0e-12)
    }

    @Test
    fun `Wgs84 UTM zone 36 has CM 33 degrees east`() {
        val crs = Crs.Wgs84Utm(zone = 36, northern = true)
        assertEquals(33.0, crs.centralMeridianDeg, 1.0e-12)
        assertEquals(500_000.0, crs.falseEastingM, 1.0e-9)
        assertEquals(0.9996, crs.scaleOnCentralMeridian, 1.0e-12)
    }
}
