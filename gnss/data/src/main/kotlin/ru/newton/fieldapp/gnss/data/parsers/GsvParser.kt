package ru.newton.fieldapp.gnss.data.parsers

/**
 * GPGSV / GNGSV / GLGSV / GAGSV / BDGSV — satellites in view.
 *
 * Layout (after talker id stripped from `fields[0]`):
 * ```
 *   [0] talker+sentence  (e.g. "GPGSV")
 *   [1] total messages in this batch (1..N)
 *   [2] this message number          (1..N)
 *   [3] total satellites in view
 *   for each satellite (up to 4 per sentence):
 *     [+0] PRN
 *     [+1] elevation, deg
 *     [+2] azimuth, deg
 *     [+3] SNR, dB-Hz
 * ```
 *
 * The store-side aggregator merges multiple GSV batches into one snapshot —
 * we just emit what this single line carries. Trailing-elision (a 4th group
 * with fewer fields) is allowed: missing values become `null`.
 */
internal object GsvParser {
    fun parse(fields: List<String>): NmeaParsed {
        if (fields.size < 4) return NmeaParsed.Malformed("GSV needs ≥4 header fields, got ${fields.size}")
        val totalMessages = NmeaParseHelpers.nullableInt(fields[1])
            ?: return NmeaParsed.Malformed("GSV totalMessages not int")
        val messageNumber = NmeaParseHelpers.nullableInt(fields[2])
            ?: return NmeaParsed.Malformed("GSV messageNumber not int")
        val totalSatsInView = NmeaParseHelpers.nullableInt(fields[3])
            ?: return NmeaParsed.Malformed("GSV totalSats not int")

        // Constellation = first two chars of the talker id (e.g. "GP" from "GPGSV").
        val constellation = fields[0].take(2)

        val satellites = buildList {
            var i = 4
            while (i + 3 < fields.size) {
                val prn = NmeaParseHelpers.nullableInt(fields[i])
                if (prn != null) {
                    add(
                        SatelliteInView(
                            prn = prn,
                            elevationDeg = NmeaParseHelpers.nullableInt(fields[i + 1]),
                            azimuthDeg = NmeaParseHelpers.nullableInt(fields[i + 2]),
                            snrDbHz = NmeaParseHelpers.nullableInt(fields[i + 3]),
                            constellation = constellation,
                        ),
                    )
                }
                i += 4
            }
            // Trailing partial group (e.g. last sentence with 2 sats only).
            if (i < fields.size && fields[i].isNotBlank()) {
                val prn = NmeaParseHelpers.nullableInt(fields[i])
                if (prn != null) {
                    add(
                        SatelliteInView(
                            prn = prn,
                            elevationDeg = fields.getOrNull(i + 1)?.let(NmeaParseHelpers::nullableInt),
                            azimuthDeg = fields.getOrNull(i + 2)?.let(NmeaParseHelpers::nullableInt),
                            snrDbHz = fields.getOrNull(i + 3)?.let(NmeaParseHelpers::nullableInt),
                            constellation = constellation,
                        ),
                    )
                }
            }
        }
        return NmeaParsed.Gsv(
            totalMessages = totalMessages,
            messageNumber = messageNumber,
            totalSatsInView = totalSatsInView,
            satellites = satellites,
        )
    }
}
