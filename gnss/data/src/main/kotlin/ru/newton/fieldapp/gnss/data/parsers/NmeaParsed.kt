package ru.newton.fieldapp.gnss.data.parsers

/**
 * Result of parsing one NMEA line.
 *
 * Every parser extends this via the dispatcher: new sentence types add a new
 * case here AND a handler in `GnssStatusStore.onLine`. Parsers NEVER throw;
 * malformed input returns one of the error cases below so the upstream
 * coroutine is not cancelled on a single bad line.
 */
sealed interface NmeaParsed {
    /** The line doesn't match a known sentence — logged and ignored. */
    data class Unknown(
        val line: String,
    ) : NmeaParsed

    /** Checksum mismatch. Logged with full line for diagnostics. */
    data class ChecksumError(
        val line: String,
    ) : NmeaParsed

    /** Field count or format mismatch for an otherwise-known sentence. */
    data class Malformed(
        val message: String,
    ) : NmeaParsed

    // --- typed results, one per supported sentence ---

    /** GPGGA: position, fix quality, correction age. */
    data class Gga(
        // Null when the receiver has no fix (quality 0 → empty lat/lon cells).
        // A no-fix GGA is a valid, meaningful sentence — it tells the UI the fix
        // was lost — so it must NOT be dropped as Malformed.
        val latitude: Double?,
        val longitude: Double?,
        val fixQuality: Int, // raw GGA field 6; map to FixQuality at the store
        val satsUsed: Int,
        val hdop: Double?,
        /**
         * True WGS-84 ellipsoidal height = GGA field 9 (orthometric) + field 11
         * (geoid separation). The parser reconstructs it so downstream CRS code
         * always receives an ellipsoidal value regardless of the receiver's
         * `coordsystem geoid` mode. Falls back to raw field 9 if separation absent.
         */
        val ellipsoidalHeight: Double?,
        val correctionAgeSec: Double?,
        val timestampUtcMs: Long,
        /** GGA field 11, geoid separation (N) in metres. Null if not provided. */
        val geoidSeparation: Double? = null,
    ) : NmeaParsed

    /** GPGST: accuracy estimates. */
    data class Gst(
        val sigmaLat: Double,
        val sigmaLon: Double,
        val sigmaAlt: Double,
    ) : NmeaParsed

    /** GPGSA: DOP values, satellites in solution. */
    data class Gsa(
        val pdop: Double?,
        val hdop: Double?,
        val vdop: Double?,
        val satPrns: List<Int>,
    ) : NmeaParsed

    /** GPTRA: heading, pitch, roll (for dual-antenna or IMU). */
    data class Tra(
        val headingDeg: Double?,
        val pitchDeg: Double?,
        val rollDeg: Double?,
    ) : NmeaParsed

    /**
     * GPGSV: satellites in view — a single sentence carries 1..4 satellites,
     * with [messageNumber] of [totalMessages] forming a group. The aggregator
     * concatenates groups into one full skyplot in [GnssStatusStore].
     */
    data class Gsv(
        val totalMessages: Int,
        val messageNumber: Int,
        val totalSatsInView: Int,
        val satellites: List<SatelliteInView>,
    ) : NmeaParsed

    /** PTNLVHD: heading + pitch + heading-time-stamp from dual-antenna RTK. */
    data class Vhd(
        val headingDeg: Double?,
        val pitchDeg: Double?,
        val rollDeg: Double?,
    ) : NmeaParsed

    /** PTNLAVR: heading + RTK quality from dual-antenna or IMU. */
    data class Avr(
        val headingDeg: Double?,
        val tiltDeg: Double?,
        val rollDeg: Double?,
        val rangeM: Double?,
        val fixQuality: Int?,
    ) : NmeaParsed

    // TODO: add more as the MVP requires. Each new sentence requires:
    //   1) a new case here
    //   2) a parser in parsers/ with a fixture + test in src/test/resources/fixtures/
    //   3) a dispatcher case in NmeaDispatcher.dispatch
    //   4) (optional) a GnssStatus.apply<Type> method if the sentence affects status
}

/**
 * Verifies checksum and routes the line to the right parser.
 *
 * Parser functions should be pure (no IO, no logging) — dispatcher logs
 * errors, not parsers.
 */
class NmeaDispatcher {
    fun dispatch(line: String): NmeaParsed {
        val trimmed = line.trim().trimEnd('\r', '\n')
        if (trimmed.isEmpty()) return NmeaParsed.Unknown(trimmed)
        if (!trimmed.startsWith('$') && !trimmed.startsWith('!')) return NmeaParsed.Unknown(trimmed)

        val starIdx = trimmed.lastIndexOf('*')
        if (starIdx < 0) return NmeaParsed.ChecksumError(trimmed)

        val payload = trimmed.substring(1, starIdx)
        val expected = trimmed.substring(starIdx + 1).trim().toIntOrNull(16)
            ?: return NmeaParsed.ChecksumError(trimmed)

        val actual = payload.fold(0) { acc, c -> acc xor c.code }
        if (actual != expected) return NmeaParsed.ChecksumError(trimmed)

        val fields = payload.split(',')
        return when (fields[0]) {
            "GPGGA", "GNGGA", "GLGGA", "GAGGA", "BDGGA" -> GgaParser.parse(fields)
            "GPGST", "GNGST" -> GstParser.parse(fields)
            "GPGSA", "GNGSA" -> GsaParser.parse(fields)
            "GPTRA" -> TraParser.parse(fields)
            "GPGSV", "GNGSV", "GLGSV", "GAGSV", "BDGSV" -> GsvParser.parse(fields)
            "PTNLVHD" -> VhdParser.parse(fields)
            "PTNLAVR" -> AvrParser.parse(fields)
            else -> NmeaParsed.Unknown(trimmed)
        }
    }
}
