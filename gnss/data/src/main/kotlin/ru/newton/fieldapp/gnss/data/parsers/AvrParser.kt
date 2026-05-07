package ru.newton.fieldapp.gnss.data.parsers

/**
 * PTNLAVR — Trimble-style attitude vector for dual-antenna or IMU-RTK.
 *
 * Layout:
 * ```
 *   [0] PTNLAVR
 *   [1] UTC of fix
 *   [2] heading (yaw), deg
 *   [3] "Yaw"  (literal label)
 *   [4] tilt (pitch), deg
 *   [5] "Tilt"
 *   [6] roll, deg          (sometimes blank)
 *   [7] "Roll"
 *   [8] range to secondary antenna (m)
 *   [9] solution status (1 / 4 / 5 — see VHD parser)
 *   ...
 * ```
 *
 * Some firmwares emit the PTNLAVR variant with heading at field [2], some
 * use a slightly different ordering — we extract by absolute index but
 * treat all of heading/tilt/roll as nullable so a layout drift doesn't
 * crash the receiver pipeline.
 */
internal object AvrParser {
    fun parse(fields: List<String>): NmeaParsed {
        if (fields.size < 5) return NmeaParsed.Malformed("PTNLAVR needs ≥5 fields, got ${fields.size}")
        val heading = NmeaParseHelpers.nullableDouble(fields[2])
        val tilt = fields.getOrNull(4)?.let(NmeaParseHelpers::nullableDouble)
        val roll = fields.getOrNull(6)?.let(NmeaParseHelpers::nullableDouble)
        val range = fields.getOrNull(8)?.let(NmeaParseHelpers::nullableDouble)
        val fix = fields.getOrNull(9)?.let(NmeaParseHelpers::nullableInt)
        return NmeaParsed.Avr(
            headingDeg = heading,
            tiltDeg = tilt,
            rollDeg = roll,
            rangeM = range,
            fixQuality = fix,
        )
    }
}
