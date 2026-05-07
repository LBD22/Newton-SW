package ru.newton.fieldapp.features.survey.stakeout

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LineStakeoutVectorTest {
    @Test
    fun `point on the line gives zero off and along inside segment`() {
        // Line A(0,0) → B(0,10), point at (0,5) — exactly on the line.
        val v = LineStakeoutVector.between(
            currentN = 0.0,
            currentE = 5.0,
            currentH = 0.0,
            aN = 0.0,
            aE = 0.0,
            aH = 0.0,
            bN = 0.0,
            bE = 10.0,
            bH = 0.0,
        )
        assertEquals(0.0, v.offM, 1e-9)
        assertEquals(5.0, v.alongM, 1e-9)
        assertEquals(0.0, v.distanceToFootM, 1e-9)
        assertNull(v.nearestEnd)
    }

    @Test
    fun `point off to the right is positive off, foot in segment`() {
        // Line A(0,0) → B(0,10), point at (-2, 5): N is 2 less than line, E mid-segment.
        // AB direction: (0,10), AP: (-2,5). cross(AB,AP) = 0*5 - 10*(-2) = 20.
        // off = 20 / 10 = 2.0 → positive (right side walking from A to B).
        val v = LineStakeoutVector.between(
            currentN = -2.0,
            currentE = 5.0,
            currentH = 0.0,
            aN = 0.0,
            aE = 0.0,
            aH = 0.0,
            bN = 0.0,
            bE = 10.0,
            bH = 0.0,
        )
        assertEquals(2.0, v.offM, 1e-9)
        assertEquals(5.0, v.alongM, 1e-9)
        assertEquals(2.0, v.distanceToFootM, 1e-9)
        assertNull(v.nearestEnd)
    }

    @Test
    fun `point off to the left is negative off`() {
        // Line A(0,0) → B(0,10), point at (2, 5): N is 2 more than line.
        // cross = 0*5 - 10*2 = -20. off = -2.
        val v = LineStakeoutVector.between(
            currentN = 2.0,
            currentE = 5.0,
            currentH = 0.0,
            aN = 0.0,
            aE = 0.0,
            aH = 0.0,
            bN = 0.0,
            bE = 10.0,
            bH = 0.0,
        )
        assertEquals(-2.0, v.offM, 1e-9)
    }

    @Test
    fun `point past A reports nearestEnd A and distance to A`() {
        // Line A(0,0) → B(0,10). Point at (0,-5) — before A by 5m on the same line.
        val v = LineStakeoutVector.between(
            currentN = 0.0,
            currentE = -5.0,
            currentH = 0.0,
            aN = 0.0,
            aE = 0.0,
            aH = 0.0,
            bN = 0.0,
            bE = 10.0,
            bH = 0.0,
        )
        assertEquals(LineStakeoutVector.Endpoint.A, v.nearestEnd)
        assertEquals(5.0, v.distanceToFootM, 1e-9)
    }

    @Test
    fun `point past B reports nearestEnd B`() {
        val v = LineStakeoutVector.between(
            currentN = 0.0,
            currentE = 15.0,
            currentH = 0.0,
            aN = 0.0,
            aE = 0.0,
            aH = 0.0,
            bN = 0.0,
            bE = 10.0,
            bH = 0.0,
        )
        assertEquals(LineStakeoutVector.Endpoint.B, v.nearestEnd)
        assertEquals(5.0, v.distanceToFootM, 1e-9)
    }

    @Test
    fun `degenerate line A equals B falls back to point stakeout against A`() {
        val v = LineStakeoutVector.between(
            currentN = 3.0,
            currentE = 4.0,
            currentH = 0.0,
            aN = 0.0,
            aE = 0.0,
            aH = 0.0,
            bN = 0.0,
            bE = 0.0,
            bH = 0.0,
        )
        assertEquals(5.0, v.distanceToFootM, 1e-9)
        assertEquals(LineStakeoutVector.Endpoint.A, v.nearestEnd)
    }

    @Test
    fun `delta H interpolates linearly between endpoints`() {
        // A at H=100, B at H=110. Foot at midpoint → expected H 105.
        val v = LineStakeoutVector.between(
            currentN = 0.0,
            currentE = 5.0,
            currentH = 100.0,
            aN = 0.0,
            aE = 0.0,
            aH = 100.0,
            bN = 0.0,
            bE = 10.0,
            bH = 110.0,
        )
        assertEquals(5.0, v.deltaH, 1e-9)
    }
}
