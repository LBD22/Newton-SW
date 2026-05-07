package ru.newton.fieldapp.gnss.data.parsers

/**
 * Parser for `$xxGGA` — Global Positioning System Fix Data.
 *
 * Field layout (after the talker tag, comma-separated):
 *   1: UTC time `hhmmss.sss`
 *   2: Latitude `ddmm.mmmm`
 *   3: N | S
 *   4: Longitude `dddmm.mmmm`
 *   5: E | W
 *   6: Fix quality 0..9 (see [ru.newton.fieldapp.gnss.data.FixQuality])
 *   7: Satellites used
 *   8: HDOP
 *   9: Altitude above geoid (m) — receiver-configured: WGS-84 ellipsoidal or
 *      orthometric depending on `coordsystem set geoid` mode.
 *  10: M (units)
 *  11: Geoid separation (m)
 *  12: M (units)
 *  13: Age of differential corrections, seconds
 *  14: Differential reference station ID
 */
internal object GgaParser {
    fun parse(fields: List<String>): NmeaParsed {
        if (fields.size < 10) return NmeaParsed.Malformed("GGA expects ≥10 fields, got ${fields.size}")

        val lat = NmeaParseHelpers.parseDegMin(fields[2], fields[3])
            ?: return NmeaParsed.Malformed("GGA: bad latitude '${fields[2]}/${fields[3]}'")
        val lon = NmeaParseHelpers.parseDegMin(fields[4], fields[5])
            ?: return NmeaParsed.Malformed("GGA: bad longitude '${fields[4]}/${fields[5]}'")

        val fixQuality = NmeaParseHelpers.nullableInt(fields[6])
            ?: return NmeaParsed.Malformed("GGA: bad fix quality '${fields[6]}'")
        val sats = NmeaParseHelpers.nullableInt(fields[7]) ?: 0
        val hdop = NmeaParseHelpers.nullableDouble(fields[8])
        val altitude = NmeaParseHelpers.nullableDouble(fields[9])

        val correctionAge = if (fields.size > 13) {
            NmeaParseHelpers.nullableDouble(fields[13])
        } else {
            null
        }

        return NmeaParsed.Gga(
            latitude = lat,
            longitude = lon,
            fixQuality = fixQuality,
            satsUsed = sats,
            hdop = hdop,
            ellipsoidalHeight = altitude,
            correctionAgeSec = correctionAge,
            timestampUtcMs = NmeaParseHelpers.parseTimeOfDayMs(fields[1]),
        )
    }
}
