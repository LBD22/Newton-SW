package ru.newton.fieldapp.crs.transforms

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.crs.GeocentricPoint
import ru.newton.fieldapp.crs.HelmertParams

class HelmertTransformTest {
    private val sample = GeocentricPoint(2_852_700.0, 2_204_700.0, 5_249_800.0)

    @Test
    fun `identity params produce no shift`() {
        val out = HelmertTransform.apply(sample, HelmertParams.IDENTITY)
        assertEquals(sample.xM, out.xM, 1.0e-9)
        assertEquals(sample.yM, out.yM, 1.0e-9)
        assertEquals(sample.zM, out.zM, 1.0e-9)
    }

    @Test
    fun `forward then inverse round-trip within mm`() {
        // SK-42 has the largest shift among our datums (~140m on Y); it's a tougher round-trip.
        val params = HelmertParams.WGS84_TO_SK42
        val shifted = HelmertTransform.apply(sample, params)
        val restored = HelmertTransform.apply(shifted, params.inverse())

        // Inverse of small-rotation Helmert is itself a small-rotation Helmert
        // with negated parameters; tolerable round-trip error is ≤ 1 mm.
        assertEquals(sample.xM, restored.xM, 1.0e-3)
        assertEquals(sample.yM, restored.yM, 1.0e-3)
        assertEquals(sample.zM, restored.zM, 1.0e-3)
    }

    @Test
    fun `pure translation with no rotation or scale matches dx dy dz exactly`() {
        val origin = GeocentricPoint(0.0, 0.0, 0.0)
        val params = HelmertParams(dxM = 1.0, dyM = 2.0, dzM = 3.0, rxArcSec = 0.0, ryArcSec = 0.0, rzArcSec = 0.0, scalePpm = 0.0)
        val out = HelmertTransform.apply(origin, params)
        assertEquals(1.0, out.xM, 1.0e-12)
        assertEquals(2.0, out.yM, 1.0e-12)
        assertEquals(3.0, out.zM, 1.0e-12)
    }

    @Test
    fun `1 ppm scale on a 6-million-metre radius adds about 6 metres`() {
        val params = HelmertParams(dxM = 0.0, dyM = 0.0, dzM = 0.0, rxArcSec = 0.0, ryArcSec = 0.0, rzArcSec = 0.0, scalePpm = 1.0)
        val point = GeocentricPoint(6_000_000.0, 0.0, 0.0)
        val out = HelmertTransform.apply(point, params)
        assertEquals(6_000_006.0, out.xM, 1.0e-6)
    }
}
