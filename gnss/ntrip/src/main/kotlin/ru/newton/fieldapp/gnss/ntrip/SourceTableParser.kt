package ru.newton.fieldapp.gnss.ntrip

/**
 * Parser for NTRIP caster source-table responses (RFC-style format used by all
 * casters: each row is a `;`-separated record starting with `STR;`, `CAS;`, or
 * `NET;`; we only consume `STR;` rows for MVP — those describe mountpoints
 * the surveyor can subscribe to).
 *
 * Reference field order on `STR;` rows:
 *  1. mountpoint identifier
 *  2. identifier (display name)
 *  3. format         (e.g. "RTCM 3.2")
 *  4. format-details
 *  5. carrier        (1 = L1 only, 2 = L1+L2, 0 = unknown)
 *  6. nav-system     ("GPS+GLO+GAL+BDS")
 *  7. network
 *  8. country (ISO-3)
 *  9. latitude
 * 10. longitude
 * 11. nmea           (0 = no, 1 = client must send GPGGA)
 * 12. solution       (0 = single station, 1 = network)
 * 13+ — additional fields (generator, encrypt, auth, fee, bitrate, misc).
 *      We do not consume those here.
 */
object SourceTableParser {
    fun parse(content: String): List<MountpointInfo> = content.lineSequence()
        .filter { it.startsWith("STR;") }
        .mapNotNull { parseRow(it) }
        .toList()

    private fun parseRow(line: String): MountpointInfo? {
        val cells = line.split(';')
        if (cells.size < 13) return null
        // cells[0] is the literal "STR" — skip it; mountpoint is at index 1.
        return MountpointInfo(
            id = cells.getOrNull(1).orEmpty().ifBlank { return null },
            format = cells.getOrNull(3).orEmpty(),
            carrier = cells.getOrNull(5).orEmpty(),
            navSystem = cells.getOrNull(6).orEmpty(),
            network = cells.getOrNull(7).orEmpty(),
            country = cells.getOrNull(8).orEmpty(),
            latitude = cells.getOrNull(9)?.toDoubleOrNull(),
            longitude = cells.getOrNull(10)?.toDoubleOrNull(),
            nmea = cells.getOrNull(11) == "1",
            solution = cells.getOrNull(12)?.toIntOrNull() ?: 0,
        )
    }
}
