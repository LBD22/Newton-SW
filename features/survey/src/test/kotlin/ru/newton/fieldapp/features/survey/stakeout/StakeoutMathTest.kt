package ru.newton.fieldapp.features.survey.stakeout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StakeoutMathTest {
    @Test
    fun `target due north has azimuth 0`() {
        val v = StakeoutVector.between(
            currentN = 0.0,
            currentE = 0.0,
            currentH = 0.0,
            targetN = 100.0,
            targetE = 0.0,
            targetH = 0.0,
        )
        assertEquals(100.0, v.distanceM, 1.0e-9)
        assertEquals(0.0, v.azimuthDeg, 1.0e-9)
    }

    @Test
    fun `target due east has azimuth 90`() {
        val v = StakeoutVector.between(0.0, 0.0, 0.0, 0.0, 50.0, 0.0)
        assertEquals(90.0, v.azimuthDeg, 1.0e-9)
    }

    @Test
    fun `target due south has azimuth 180`() {
        val v = StakeoutVector.between(0.0, 0.0, 0.0, -10.0, 0.0, 0.0)
        assertEquals(180.0, v.azimuthDeg, 1.0e-9)
    }

    @Test
    fun `target due west has azimuth 270`() {
        val v = StakeoutVector.between(0.0, 0.0, 0.0, 0.0, -10.0, 0.0)
        assertEquals(270.0, v.azimuthDeg, 1.0e-9)
    }

    @Test
    fun `target NE has azimuth 45 and correct distance`() {
        val v = StakeoutVector.between(0.0, 0.0, 0.0, 1.0, 1.0, 0.0)
        assertEquals(45.0, v.azimuthDeg, 1.0e-9)
        assertEquals(kotlin.math.sqrt(2.0), v.distanceM, 1.0e-9)
    }

    @Test
    fun `delta H reports vertical offset`() {
        val v = StakeoutVector.between(0.0, 0.0, 100.0, 0.0, 0.0, 105.5)
        assertEquals(5.5, v.deltaH, 1.0e-9)
    }
}
