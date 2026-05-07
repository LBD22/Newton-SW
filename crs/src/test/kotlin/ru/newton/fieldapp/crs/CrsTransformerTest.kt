package ru.newton.fieldapp.crs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs

class CrsTransformerTest {
    @Test
    fun `transform between identical CRSs is a no-op`() {
        val p = GeoPoint(55.7522, 37.6156, 156.3)
        val out = CrsTransformer.transformGeo(p, Crs.Wgs84Geo, Crs.Wgs84Geo)
        assertEquals(p, out)
    }

    @Test
    fun `WGS84 to GSK2011 shift on a Moscow point is sub-metre`() {
        // ГСК-2011 is essentially co-located with WGS-84 (Helmert params are cm-level).
        val p = GeoPoint(55.7522, 37.6156, 156.3)
        val out = CrsTransformer.transformGeo(p, Crs.Wgs84Geo, Crs.Gsk2011Geo)
        // Position shift in degrees should be < 1e-5 (~1 m at this latitude).
        assert(abs(out.latDeg - p.latDeg) < 1.0e-5) { "lat drift too large: $out vs $p" }
        assert(abs(out.lonDeg - p.lonDeg) < 1.0e-5) { "lon drift too large: $out vs $p" }
        assert(abs(out.ellipsoidalHeightM - p.ellipsoidalHeightM) < 1.0)
    }

    @Test
    fun `WGS84 to SK42 shift is roughly 100-200 m as documented`() {
        // SK-42 datum shift is well-known to be on the order of 150 m total in geocentric.
        // After projecting the difference back to lat/lon at Moscow, expect ~1 arcsecond shifts.
        val p = GeoPoint(55.7522, 37.6156, 156.3)
        val out = CrsTransformer.transformGeo(p, Crs.Wgs84Geo, Crs.Sk42Gk(zone = 7))
        // Δlat in degrees: at 55° N, 1 m ≈ 9e-6 deg. 100m → ~9e-4 deg.
        val deltaLatDeg = abs(out.latDeg - p.latDeg)
        val deltaLonDeg = abs(out.lonDeg - p.lonDeg)
        assert(deltaLatDeg in 1.0e-5..1.0e-2) { "Δlat suspicious: $deltaLatDeg" }
        assert(deltaLonDeg in 1.0e-5..1.0e-2) { "Δlon suspicious: $deltaLonDeg" }
    }

    @Test
    fun `WGS84 round-trip via SK42 returns original within 1 cm`() {
        val p = GeoPoint(55.7522, 37.6156, 156.3)
        val sk42 = Crs.Sk42Gk(zone = 7)
        val intermediate = CrsTransformer.transformGeo(p, Crs.Wgs84Geo, sk42)
        val back = CrsTransformer.transformGeo(intermediate, sk42, Crs.Wgs84Geo)
        // 1 cm at lat 55° ≈ 9e-8 deg.
        assert(abs(p.latDeg - back.latDeg) < 5.0e-8) { "lat round-trip drift: ${p.latDeg - back.latDeg}" }
        assert(abs(p.lonDeg - back.lonDeg) < 5.0e-8) { "lon round-trip drift: ${p.lonDeg - back.lonDeg}" }
        assertEquals(p.ellipsoidalHeightM, back.ellipsoidalHeightM, 1.0e-3)
    }

    @Test
    fun `project then unproject within zone gives back the original geographic`() {
        val sourceGeo = GeoPoint(55.7522, 37.6156, 156.3)
        val target = Crs.Gsk2011Gk(zone = 7) // CM = 39°
        val projected = CrsTransformer.project(sourceGeo, Crs.Gsk2011Geo, target)
        val back = CrsTransformer.unproject(projected, target)
        // Same datum, only TM round-trip.
        assert(abs(sourceGeo.latDeg - back.latDeg) < 1.0e-7)
        assert(abs(sourceGeo.lonDeg - back.lonDeg) < 1.0e-7)
        assertEquals(sourceGeo.ellipsoidalHeightM, back.ellipsoidalHeightM, 1.0e-9)
    }
}
