package ru.newton.fieldapp.gnss.data.parsers

/**
 * Parser for `$GPTRA` — vendor-specific dual-antenna heading sentence used by
 * Newton receivers (also seen on ComNav).
 *
 * Field layout (after talker tag):
 *   1: UTC time
 *   2: Heading (deg)        — null if unavailable
 *   3: Pitch (deg)          — null if unavailable
 *   4: Roll (deg)           — null if unavailable
 *   5: Solution quality     — 0=none, 1=single, 2=float, 3=fix (vendor-specific)
 *   6: Satellites in fix
 *   7: Age of corrections, seconds
 *
 * Per `docs/protocol-newton.md` § Messages, GPTRA is one of the MVP-required
 * sentences. We only consume heading/pitch/roll for now; the rest of the
 * fields are ignored until a feature needs them.
 */
internal object TraParser {
    fun parse(fields: List<String>): NmeaParsed {
        if (fields.size < 5) return NmeaParsed.Malformed("TRA expects ≥5 fields, got ${fields.size}")

        return NmeaParsed.Tra(
            headingDeg = NmeaParseHelpers.nullableDouble(fields[2]),
            pitchDeg = NmeaParseHelpers.nullableDouble(fields[3]),
            rollDeg = NmeaParseHelpers.nullableDouble(fields[4]),
        )
    }
}
