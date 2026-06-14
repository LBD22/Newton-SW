package ru.newton.fieldapp.gnss.ntrip

import ru.newton.fieldapp.gnss.data.FixQuality
import ru.newton.fieldapp.gnss.data.GnssStatus
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Synthesises a NMEA `$GPGGA,...*XX\r\n` sentence from the current
 * [GnssStatus]. VRS NTRIP casters drive the virtual reference station from
 * this position fix, so we periodically upstream it on the same connection
 * we use to read RTCM (NTR-004).
 *
 * The sentence shape mirrors the Newton receiver's own GGA output (RMC-style
 * UTC time, ddmm.mmmm/dddmm.mmmm with hemispheres, integer fix quality,
 * meters-only height). Locale is forced to [Locale.US] so the decimal point
 * stays a `.` even on `ru_RU` devices — `,` would corrupt the sentence.
 *
 * Returns `null` if there is no usable position (no fix or missing lat/lon).
 */
object GpggaBuilder {
    fun fromStatus(status: GnssStatus, nowMillis: Long = System.currentTimeMillis()): String? {
        val lat = status.latitude ?: return null
        val lon = status.longitude ?: return null
        if (status.fix == FixQuality.NoFix) return null
        // GGA field 9 is orthometric height; we store ellipsoidal, so subtract the
        // geoid separation back out and report it honestly in field 11. VRS only
        // needs the position, but a correct GGA avoids confusing strict casters.
        val ellipsoidal = status.ellipsoidalHeight ?: 0.0
        val geoidSep = status.geoidSeparation
        val orthometric = if (geoidSep != null) ellipsoidal - geoidSep else ellipsoidal

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = nowMillis }
        val time = String.format(
            Locale.US,
            "%02d%02d%02d.00",
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND),
        )

        val (latStr, latHem) = degreesToDmm(lat, longitude = false)
        val (lonStr, lonHem) = degreesToDmm(lon, longitude = true)

        val fixField = when (status.fix) {
            FixQuality.NoFix -> 0
            FixQuality.Single -> 1
            FixQuality.DGnss -> 2
            FixQuality.FloatRtk -> 5
            FixQuality.FixedRtk -> 4
            is FixQuality.Ppp -> 1 // most casters treat unknown >2 as untrusted
        }
        val hdop = status.hdop?.let { String.format(Locale.US, "%.1f", it) } ?: ""
        val heightStr = String.format(Locale.US, "%.2f", orthometric)
        val sepStr = String.format(Locale.US, "%.2f", geoidSep ?: 0.0)
        val ageStr = status.correctionAgeSec?.let { String.format(Locale.US, "%.1f", it) } ?: ""

        // Field order: time, lat, NS, lon, EW, fix, sats, hdop, height, M, geoidSep, M, age, refId
        val body = "GPGGA,$time,$latStr,$latHem,$lonStr,$lonHem,$fixField,${status.satsUsed},$hdop,$heightStr,M,$sepStr,M,$ageStr,"
        val checksum = body.fold(0) { acc, c -> acc xor c.code }
        return "\$$body*${"%02X".format(checksum)}\r\n"
    }

    /** `lon=false` formats degrees as `ddmm.mmmm`; `lon=true` as `dddmm.mmmm`. */
    private fun degreesToDmm(deg: Double, longitude: Boolean): Pair<String, Char> {
        val absDeg = abs(deg)
        val d = absDeg.toInt()
        val m = (absDeg - d) * 60.0
        val hem = if (longitude) {
            if (deg >= 0) 'E' else 'W'
        } else {
            if (deg >= 0) 'N' else 'S'
        }
        val str = if (longitude) {
            String.format(Locale.US, "%03d%07.4f", d, m)
        } else {
            String.format(Locale.US, "%02d%07.4f", d, m)
        }
        return str to hem
    }
}
