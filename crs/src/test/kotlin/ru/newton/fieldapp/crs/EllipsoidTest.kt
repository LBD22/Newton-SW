package ru.newton.fieldapp.crs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs

class EllipsoidTest {
    @Test
    fun `WGS-84 derived constants match published values`() {
        val e = Ellipsoid.WGS84
        assertEquals(6_356_752.314245, e.polarRadiusM, 1.0e-3)
        // EPSG datasheet: e² = 0.00669437999014
        assertEquals(0.00669437999014, e.eccentricitySquared, 1.0e-12)
        // e'² = e²/(1-e²) ≈ 0.0067394967754
        assertEquals(0.0067394967754, e.secondEccentricitySquared, 1.0e-10)
    }

    @Test
    fun `prime vertical radius equals semi-major at equator`() {
        val e = Ellipsoid.WGS84
        val n = e.primeVerticalRadiusM(0.0)
        // At φ=0, sin(φ)=0 → N = a / sqrt(1) = a.
        assertEquals(e.semiMajorM, n, 1.0e-9)
    }

    @Test
    fun `prime vertical radius increases with latitude`() {
        val e = Ellipsoid.WGS84
        val nEquator = e.primeVerticalRadiusM(0.0)
        val nPolar = e.primeVerticalRadiusM(Math.PI / 2.0)
        // Pole: N(90°) = a / sqrt(1-e²) > a.
        assert(nPolar > nEquator)
        assert(abs(nPolar - e.semiMajorM / kotlin.math.sqrt(1.0 - e.eccentricitySquared)) < 1e-6)
    }

    @Test
    fun `Krasovsky-1940 has different semi-major than WGS-84`() {
        val k = Ellipsoid.KRASOVSKY1940
        val w = Ellipsoid.WGS84
        // Krasovsky semi-major is 108 m larger than WGS-84.
        assertEquals(108.0, k.semiMajorM - w.semiMajorM, 1.0e-9)
    }
}
