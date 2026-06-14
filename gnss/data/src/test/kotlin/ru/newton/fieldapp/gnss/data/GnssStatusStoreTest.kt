package ru.newton.fieldapp.gnss.data

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.gnss.data.parsers.NmeaParsed
import ru.newton.fieldapp.gnss.data.parsers.SatelliteInView

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
    fun `no-fix Gga clears position and stale accuracy`() = runTest {
        val store = GnssStatusStore()
        // A good fixed-RTK epoch with σ from GST.
        store.submit(NmeaParsed.Gga(55.0, 37.0, 4, 12, 0.8, 100.0, 0.5, 0L))
        store.submit(NmeaParsed.Gst(sigmaLat = 0.01, sigmaLon = 0.01, sigmaAlt = 0.02))
        assertEquals(0.02, store.status.value.sigmaH)

        // Fix lost: a quality-0 GGA with empty (null) position. The frozen cm
        // accuracy must NOT linger, and position must clear — otherwise the strip
        // shows a confident fix over stale coordinates.
        store.submit(NmeaParsed.Gga(null, null, 0, 0, null, null, null, 0L))
        val s = store.status.value
        assertEquals(FixQuality.NoFix, s.fix)
        assertEquals(null, s.latitude)
        assertEquals(null, s.longitude)
        assertEquals(null, s.sigmaH)
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
    fun `Gsv populates satsVisible from the merged skyplot, summed across constellations`() = runTest {
        val store = GnssStatusStore()
        // GPS: single-sentence batch with 2 sats in view.
        store.submit(
            NmeaParsed.Gsv(
                totalMessages = 1,
                messageNumber = 1,
                totalSatsInView = 2,
                satellites = listOf(sat(1, "GP"), sat(2, "GP")),
            ),
        )
        assertEquals(2, store.status.value.satsVisible)

        // GLONASS arrives separately — satsVisible must sum both constellations,
        // matching the skyplot list size rather than one GSV's field 3.
        store.submit(
            NmeaParsed.Gsv(
                totalMessages = 1,
                messageNumber = 1,
                totalSatsInView = 3,
                satellites = listOf(sat(65, "GL"), sat(66, "GL"), sat(67, "GL")),
            ),
        )
        assertEquals(5, store.status.value.satsVisible)
        assertEquals(5, store.status.value.satellitesInView.size)
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

    private fun sat(prn: Int, constellation: String) = SatelliteInView(
        prn = prn,
        elevationDeg = 45,
        azimuthDeg = 180,
        snrDbHz = 40,
        constellation = constellation,
    )
}
