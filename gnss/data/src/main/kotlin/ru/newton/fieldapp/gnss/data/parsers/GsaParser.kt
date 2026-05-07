package ru.newton.fieldapp.gnss.data.parsers

/**
 * Parser for `$xxGSA` — DOP and Active Satellites.
 *
 * Field layout (after talker tag):
 *   1: Mode (A=auto, M=manual)
 *   2: Fix mode (1=no fix, 2=2D, 3=3D)
 *   3..14: 12 PRN slots — empty cells mean "slot unused"
 *   15: PDOP
 *   16: HDOP
 *   17: VDOP
 */
internal object GsaParser {
    fun parse(fields: List<String>): NmeaParsed {
        if (fields.size < 18) return NmeaParsed.Malformed("GSA expects ≥18 fields, got ${fields.size}")

        val prns = (3..14).mapNotNull { i -> NmeaParseHelpers.nullableInt(fields[i]) }
        val pdop = NmeaParseHelpers.nullableDouble(fields[15])
        val hdop = NmeaParseHelpers.nullableDouble(fields[16])
        val vdop = NmeaParseHelpers.nullableDouble(fields[17])

        return NmeaParsed.Gsa(pdop = pdop, hdop = hdop, vdop = vdop, satPrns = prns)
    }
}
