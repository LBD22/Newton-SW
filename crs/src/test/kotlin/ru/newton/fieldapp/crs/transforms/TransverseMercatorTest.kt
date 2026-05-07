package ru.newton.fieldapp.crs.transforms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.crs.GeoPoint
import kotlin.math.abs

class TransverseMercatorTest {
    @Test
    fun `point on UTM 36N central meridian gives easting equal to false easting`() {
        val crs = Crs.Wgs84Utm(zone = 36, northern = true)
        val p = GeoPoint(latDeg = 55.0, lonDeg = crs.centralMeridianDeg, ellipsoidalHeightM = 0.0)
        val proj = TransverseMercator.forward(p, crs)
        assertEquals(crs.falseEastingM, proj.eastingM, 1.0e-6)
    }

    @Test
    fun `point on GK zone 8 central meridian gives easting 8500000`() {
        // ГСК-2011 GK zone 8: CM = 45° E, false easting includes zone prefix → 8_500_000.
        val crs = Crs.Gsk2011Gk(zone = 8)
        val p = GeoPoint(latDeg = 50.0, lonDeg = 45.0, ellipsoidalHeightM = 0.0)
        val proj = TransverseMercator.forward(p, crs)
        assertEquals(8_500_000.0, proj.eastingM, 1.0e-6)
    }

    @Test
    fun `equator on central meridian gives northing equal to false northing`() {
        val crs = Crs.Wgs84Utm(zone = 36, northern = true)
        val p = GeoPoint(latDeg = 0.0, lonDeg = crs.centralMeridianDeg, ellipsoidalHeightM = 0.0)
        val proj = TransverseMercator.forward(p, crs)
        assertEquals(crs.falseNorthingM, proj.northingM, 1.0e-3)
    }

    @Test
    fun `forward then inverse round-trip within 1 cm in zone interior`() {
        // Sweep latitudes typical of Russia and longitudes within ±2.5° of CM.
        val crs = Crs.Gsk2011Gk(zone = 7) // CM = 39° E
        val cases = buildList {
            for (lat in listOf(45.0, 55.0, 60.0, 67.0)) {
                for (deltaLon in listOf(-2.5, -1.0, 0.0, 1.0, 2.5)) {
                    add(GeoPoint(lat, crs.centralMeridianDeg + deltaLon, ellipsoidalHeightM = 0.0))
                }
            }
        }
        for (input in cases) {
            val proj = TransverseMercator.forward(input, crs)
            val back = TransverseMercator.inverse(proj, crs)
            // 1 cm at lat 60° ≈ 9e-8 deg in latitude, 1.8e-7 deg in longitude.
            assert(abs(input.latDeg - back.latDeg) < 1.0e-7) {
                "lat round-trip failed for $input → $back"
            }
            assert(abs(input.lonDeg - back.lonDeg) < 2.0e-7) {
                "lon round-trip failed for $input → $back"
            }
        }
    }

    @Test
    fun `UTM round-trip within 1 cm at zone edges`() {
        val crs = Crs.Wgs84Utm(zone = 36, northern = true) // CM = 33° E
        // UTM is widest near the equator; pick a tropical band.
        val input = GeoPoint(latDeg = 5.0, lonDeg = crs.centralMeridianDeg + 3.0, ellipsoidalHeightM = 12.5)
        val proj = TransverseMercator.forward(input, crs)
        val back = TransverseMercator.inverse(proj, crs)
        assert(abs(input.latDeg - back.latDeg) < 1.0e-7)
        assert(abs(input.lonDeg - back.lonDeg) < 2.0e-7)
        assertEquals(input.ellipsoidalHeightM, back.heightOf(proj), 1.0e-9)
    }

    private fun GeoPoint.heightOf(
        @Suppress("UNUSED_PARAMETER") proj: ru.newton.fieldapp.crs.ProjectedPoint,
    ): Double = this.ellipsoidalHeightM
}
