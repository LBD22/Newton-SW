package ru.newton.fieldapp.gnss.data

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.gnss.data.parsers.NmeaParsed

class GnssStatusStoreTest {
    @Test
    fun `initial status reports no fix and zero satellites`() = runTest {
        val store = GnssStatusStore()
        assertEquals(FixQuality.NoFix, store.status.value.fix)
        assertEquals(0, store.status.value.satsUsed)
    }

    @Test
    fun `Gga fixed-RTK promotes fix and updates position`() = runTest {
        val store = GnssStatusStore()
        store.submit(
            NmeaParsed.Gga(
                latitude = 55.7522,
                longitude = 37.6156,
                fixQuality = 4,
                satsUsed = 15,
                hdop = 0.6,
                ellipsoidalHeight = 234.567,
                correctionAgeSec = 1.2,
                timestampUtcMs = 0L,
            ),
        )
        val s = store.status.value
        assertEquals(FixQuality.FixedRtk, s.fix)
        assertEquals(55.7522, s.latitude)
        assertEquals(15, s.satsUsed)
        assertEquals(1.2, s.correctionAgeSec)
    }

    @Test
    fun `Gst sigma values land in sigmaN sigmaE sigmaH`() = runTest {
        val store = GnssStatusStore()
        store.submit(NmeaParsed.Gst(sigmaLat = 0.012, sigmaLon = 0.009, sigmaAlt = 0.025))
        val s = store.status.value
        assertEquals(0.012, s.sigmaN)
        assertEquals(0.009, s.sigmaE)
        assertEquals(0.025, s.sigmaH)
    }

    @Test
    fun `Tra without heading marks IMU invalid`() = runTest {
        val store = GnssStatusStore()
        store.submit(NmeaParsed.Tra(headingDeg = null, pitchDeg = null, rollDeg = null))
        assertEquals(false, store.status.value.imuValid)
    }

    @Test
    fun `reset returns to initial`() = runTest {
        val store = GnssStatusStore()
        store.submit(
            NmeaParsed.Gga(55.0, 37.0, 4, 12, 0.8, 100.0, 0.5, 0L),
        )
        store.reset()
        assertEquals(FixQuality.NoFix, store.status.value.fix)
        assertEquals(null, store.status.value.latitude)
    }

    @Test
    fun `subscribers see every commit as a distinct emission`() = runTest {
        val store = GnssStatusStore()
        store.status.test {
            // Initial.
            assertEquals(FixQuality.NoFix, awaitItem().fix)
            store.submit(NmeaParsed.Gga(0.0, 0.0, 1, 5, 5.0, 10.0, null, 0L))
            assertEquals(FixQuality.Single, awaitItem().fix)
            store.submit(NmeaParsed.Gga(0.0, 0.0, 4, 12, 0.6, 10.0, 1.0, 0L))
            assertEquals(FixQuality.FixedRtk, awaitItem().fix)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Unknown and error parses do not mutate state`() = runTest {
        val store = GnssStatusStore()
        store.submit(NmeaParsed.Gga(55.0, 37.0, 4, 12, 0.8, 100.0, 0.5, 0L))
        val before = store.status.value
        store.submit(NmeaParsed.Unknown("garbage"))
        store.submit(NmeaParsed.ChecksumError("bad"))
        store.submit(NmeaParsed.Malformed("oops"))
        assertEquals(before, store.status.value)
    }
}
