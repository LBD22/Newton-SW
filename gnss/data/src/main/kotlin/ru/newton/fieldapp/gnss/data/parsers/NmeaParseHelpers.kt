package ru.newton.fieldapp.gnss.data.parsers

/**
 * Shared helpers for the typed NMEA parsers in this package.
 *
 * Parsers must remain pure and side-effect-free: malformed input becomes a
 * [NmeaParsed.Malformed] / [NmeaParsed.ChecksumError] case rather than a thrown
 * exception. The dispatcher logs; parsers do not.
 */
internal object NmeaParseHelpers {
    /**
     * NMEA latitude/longitude come as `ddmm.mmmm` (latitude) or `dddmm.mmmm`
     * (longitude). Convert to signed decimal degrees using the hemisphere
     * indicator. Empty input → null (e.g. fix not yet acquired).
     */
    fun parseDegMin(raw: String, hemisphere: String): Double? {
        if (raw.isBlank()) return null
        val value = raw.toDoubleOrNull() ?: return null
        val degrees = (value / 100).toInt()
        val minutes = value - degrees * 100
        val decimal = degrees + minutes / 60.0
        return when (hemisphere.uppercase()) {
            "S", "W" -> -decimal
            "N", "E", "" -> decimal
            else -> null
        }
    }

    /** Empty cell → null; otherwise parse double or null on failure. */
    fun nullableDouble(raw: String): Double? = if (raw.isBlank()) null else raw.toDoubleOrNull()

    /** Empty cell → null; otherwise parse int or null on failure. */
    fun nullableInt(raw: String): Int? = if (raw.isBlank()) null else raw.toIntOrNull()

    /**
     * Convert NMEA UTC time `hhmmss.sss` into millisecond-of-day. We keep this
     * coarse — the dispatcher stamps the actual epoch using `System.currentTimeMillis()`
     * because the receiver clock and phone clock can differ.
     */
    fun parseTimeOfDayMs(raw: String): Long {
        if (raw.length < 6) return 0L
        val h = raw.substring(0, 2).toIntOrNull() ?: return 0L
        val m = raw.substring(2, 4).toIntOrNull() ?: return 0L
        val sFraction = raw.substring(4).toDoubleOrNull() ?: return 0L
        return (((h * 3600) + (m * 60)) * 1000) + (sFraction * 1000).toLong()
    }
}
