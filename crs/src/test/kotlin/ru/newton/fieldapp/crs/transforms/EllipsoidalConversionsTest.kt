package ru.newton.fieldapp.crs.transforms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.crs.Ellipsoid
import ru.newton.fieldapp.crs.GeoPoint
import kotlin.math.abs

class EllipsoidalConversionsTest {
    private val toleranceMm = 1.0e-3 // 1 mm round-trip target

    @Test
    fun `geo to ECEF round-trip over a sweep of latitudes`() {
        val cases = listOf(
            GeoPoint(0.0, 0.0, 0.0),
            GeoPoint(0.0, 90.0, 0.0),
            GeoPoint(45.0, 45.0, 100.0),
            GeoPoint(55.7522, 37.6156, 156.3), // Moscow
            GeoPoint(-33.8688, 151.2093, 5.0), // Sydney
            GeoPoint(89.0, 0.0, 0.0), // near pole
        )
        for (input in cases) {
            val ecef = EllipsoidalConversions.geographicToGeocentric(input, Ellipsoid.WGS84)
            val back = EllipsoidalConversions.geocentricToGeographic(ecef, Ellipsoid.WGS84)
            assertCloseEnough(input.latDeg, back.latDeg, toleranceLat = 1e-9, label = "lat for $input")
            assertCloseEnough(input.lonDeg, back.lonDeg, toleranceLat = 1e-9, label = "lon for $input")
            assertEquals(input.ellipsoidalHeightM, back.ellipsoidalHeightM, toleranceMm, "h for $input")
        }
    }

    @Test
    fun `equator zero meridian gives X=a Y=0 Z=0`() {
        val p = GeoPoint(0.0, 0.0, 0.0)
        val ecef = EllipsoidalConversions.geographicToGeocentric(p, Ellipsoid.WGS84)
        assertEquals(Ellipsoid.WGS84.semiMajorM, ecef.xM, 1e-6)
        assertEquals(0.0, ecef.yM, 1e-6)
        assertEquals(0.0, ecef.zM, 1e-6)
    }

    @Test
    fun `north pole with zero ellipsoidal height gives Z=b`() {
        val p = GeoPoint(90.0, 0.0, 0.0)
        val ecef = EllipsoidalConversions.geographicToGeocentric(p, Ellipsoid.WGS84)
        assertEquals(0.0, ecef.xM, 1.0e-3)
        assertEquals(0.0, ecef.yM, 1.0e-3)
        assertEquals(Ellipsoid.WGS84.polarRadiusM, ecef.zM, 1.0e-3)
    }

    private fun assertCloseEnough(
        expected: Double,
        actual: Double,
        toleranceLat: Double,
        label: String,
    ) {
        assert(abs(expected - actual) < toleranceLat) {
            "$label: expected=$expected actual=$actual (Δ=${abs(expected - actual)})"
        }
    }
}
