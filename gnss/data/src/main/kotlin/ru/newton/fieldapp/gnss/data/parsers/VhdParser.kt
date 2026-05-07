package ru.newton.fieldapp.gnss.data.parsers

/**
 * PTNLVHD — vector heading sentence emitted by Trimble-compatible firmware
 * for dual-antenna RTK.
 *
 * Layout (after the `$` and talker fields):
 * ```
 *   [0] PTNLVHD
 *   [1] UTC of position fix
 *   [2] date
 *   [3] heading in degrees
 *   [4] heading rate of change
 *   [5] pitch in degrees
 *   [6] pitch rate
 *   [7] range from primary to secondary antenna (m)
 *   [8] solution status (1 = autonomous, 4 = RTK fixed, 5 = RTK float)
 *   ...
 * ```
 *
 * We only care about heading and pitch in MVP — the rest is discarded but
 * documented above so a future contributor knows the semantics.
 */
internal object VhdParser {
    fun parse(fields: List<String>): NmeaParsed {
        if (fields.size < 7) return NmeaParsed.Malformed("PTNLVHD needs ≥7 fields, got ${fields.size}")
        val heading = NmeaParseHelpers.nullableDouble(fields[3])
        val pitch = NmeaParseHelpers.nullableDouble(fields[5])
        // PTNLVHD doesn't carry roll directly — we surface null and let the
        // store keep whatever roll it already has from TRA / IMU.
        return NmeaParsed.Vhd(headingDeg = heading, pitchDeg = pitch, rollDeg = null)
    }
}
