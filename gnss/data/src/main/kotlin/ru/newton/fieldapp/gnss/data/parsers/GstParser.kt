package ru.newton.fieldapp.gnss.data.parsers

/**
 * Parser for `$xxGST` — Position Error Statistics.
 *
 * Field layout (after talker tag):
 *   1: UTC time
 *   2: RMS value (overall σ, m)
 *   3: Std dev of semi-major axis (m)
 *   4: Std dev of semi-minor axis (m)
 *   5: Orientation of semi-major axis (deg)
 *   6: σ latitude  (m)  — `sigmaLat`
 *   7: σ longitude (m)  — `sigmaLon`
 *   8: σ altitude  (m)  — `sigmaAlt`
 */
internal object GstParser {
    fun parse(fields: List<String>): NmeaParsed {
        if (fields.size < 9) return NmeaParsed.Malformed("GST expects ≥9 fields, got ${fields.size}")

        val sigmaLat = NmeaParseHelpers.nullableDouble(fields[6])
            ?: return NmeaParsed.Malformed("GST: bad σ-lat '${fields[6]}'")
        val sigmaLon = NmeaParseHelpers.nullableDouble(fields[7])
            ?: return NmeaParsed.Malformed("GST: bad σ-lon '${fields[7]}'")
        val sigmaAlt = NmeaParseHelpers.nullableDouble(fields[8])
            ?: return NmeaParsed.Malformed("GST: bad σ-alt '${fields[8]}'")

        return NmeaParsed.Gst(
            sigmaLat = sigmaLat,
            sigmaLon = sigmaLon,
            sigmaAlt = sigmaAlt,
        )
    }
}
