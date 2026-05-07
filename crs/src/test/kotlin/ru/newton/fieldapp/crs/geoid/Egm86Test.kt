package ru.newton.fieldapp.crs.geoid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Egm86Test {
    @Test
    fun `falls back to zero everywhere when grid resource is missing`() {
        // Grid binary is not shipped yet (CRS-005 deliverable). Until it lands,
        // every query must return 0.0 to keep ellipsoidal-height projects safe.
        assertEquals(0.0, Egm86.undulationM(55.75, 37.62), 0.0)
        assertEquals(0.0, Egm86.undulationM(0.0, 0.0), 0.0)
        assertEquals(0.0, Egm86.undulationM(-89.0, 359.9), 0.0)
    }

    @Test
    fun `NoGeoid is identity`() {
        assertEquals(0.0, NoGeoid.undulationM(55.75, 37.62), 0.0)
        assertEquals(0.0, NoGeoid.undulationM(-12.34, 200.0), 0.0)
    }
}
