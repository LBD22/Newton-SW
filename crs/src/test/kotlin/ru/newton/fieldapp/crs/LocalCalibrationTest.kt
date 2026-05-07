package ru.newton.fieldapp.crs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class LocalCalibrationTest {
    @Test
    fun `pure translation recovers dx dy dz exactly`() {
        val pairs = listOf(
            LocalCalibration.Pair2D(0.0, 0.0, 100.0, 5.0, -3.0, 102.0),
            LocalCalibration.Pair2D(10.0, 0.0, 100.0, 15.0, -3.0, 102.0),
            LocalCalibration.Pair2D(0.0, 10.0, 100.0, 5.0, 7.0, 102.0),
        )
        val result = LocalCalibration.solve(pairs)
        assertEquals(5.0, result.params.dx, 1e-9)
        assertEquals(-3.0, result.params.dy, 1e-9)
        assertEquals(2.0, result.params.dz, 1e-9)
        assertEquals(1.0, result.params.scale, 1e-9)
        assertEquals(0.0, result.params.rotationDeg, 1e-9)
        assertEquals(0.0, result.rmsPlanar, 1e-9)
    }

    @Test
    fun `45-degree rotation around origin is recovered`() {
        // Apply a known 45° rotation + scale 1 + zero translation to construct pairs.
        val angle = PI / 4
        val measured = listOf(
            10.0 to 0.0,
            0.0 to 10.0,
            10.0 to 10.0,
            -5.0 to 5.0,
        )
        val pairs = measured.map { (mn, me) ->
            val kn = cos(angle) * mn - sin(angle) * me
            val ke = sin(angle) * mn + cos(angle) * me
            LocalCalibration.Pair2D(mn, me, 0.0, kn, ke, 0.0)
        }
        val result = LocalCalibration.solve(pairs)
        assertEquals(45.0, result.params.rotationDeg, 1e-7)
        assertEquals(1.0, result.params.scale, 1e-9)
        assertTrue(result.rmsPlanar < 1e-9)
    }

    @Test
    fun `noisy fit produces non-zero residuals but stays close`() {
        val angle = 0.5 // ~28.6°
        val s = 1.001
        val tx = 100.0
        val ty = -50.0
        val raw = listOf(
            0.0 to 0.0,
            10.0 to 0.0,
            0.0 to 10.0,
            10.0 to 10.0,
            5.0 to 5.0,
        )
        val noise = listOf(0.001 to 0.000, -0.002 to 0.001, 0.000 to -0.001, 0.001 to 0.002, -0.001 to -0.001)
        val pairs = raw.zip(noise).map { (m, ε) ->
            val (mn, me) = m
            val (en, ee) = ε
            val kn = s * (cos(angle) * mn - sin(angle) * me) + tx + en
            val ke = s * (sin(angle) * mn + cos(angle) * me) + ty + ee
            LocalCalibration.Pair2D(mn, me, 0.0, kn, ke, 0.0)
        }
        val result = LocalCalibration.solve(pairs)
        // Recovery within 0.5 mm of the perfect transform under sub-mm noise.
        assertEquals(s, result.params.scale, 1e-3)
        assertEquals(angle, Math.toRadians(result.params.rotationDeg), 1e-3)
        assertTrue(result.rmsPlanar < 0.005)
    }

    @Test
    fun `apply roundtrips through the transform`() {
        val pairs = listOf(
            LocalCalibration.Pair2D(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
            LocalCalibration.Pair2D(10.0, 0.0, 0.0, 11.0, 1.0, 1.0),
            LocalCalibration.Pair2D(0.0, 10.0, 0.0, 1.0, 11.0, 1.0),
        )
        val params = LocalCalibration.solve(pairs).params
        val (n, e, h) = params.apply(5.0, 5.0, 0.0)
        assertEquals(6.0, n, 1e-9)
        assertEquals(6.0, e, 1e-9)
        assertEquals(1.0, h, 1e-9)
    }

    @Test
    fun `single pair throws — too few constraints`() {
        assertThrows<IllegalArgumentException> {
            LocalCalibration.solve(listOf(LocalCalibration.Pair2D(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)))
        }
    }

    @Test
    fun `coincident measured points throw — rotation undefined`() {
        val pairs = listOf(
            LocalCalibration.Pair2D(5.0, 5.0, 0.0, 1.0, 1.0, 0.0),
            LocalCalibration.Pair2D(5.0, 5.0, 0.0, 2.0, 2.0, 0.0),
        )
        assertThrows<IllegalArgumentException> { LocalCalibration.solve(pairs) }
    }

    @Test
    fun `residuals quantify planar fit quality`() {
        val pairs = listOf(
            LocalCalibration.Pair2D(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            LocalCalibration.Pair2D(10.0, 0.0, 0.0, 10.0, 0.0, 0.0),
            // Inject a 1-cm error on the third pair.
            LocalCalibration.Pair2D(0.0, 10.0, 0.0, 0.0, 10.01, 0.0),
        )
        val result = LocalCalibration.solve(pairs)
        // Each residual is the perpendicular distance after best-fit alignment;
        // total planar squared error is 0.01² split across three points.
        val maxPlanar = result.residuals.maxOf { hypot(it.deltaN, it.deltaE) }
        assertTrue(maxPlanar > 0.0)
        assertTrue(maxPlanar < 0.011)
    }
}
