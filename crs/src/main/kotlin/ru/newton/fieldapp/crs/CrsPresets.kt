package ru.newton.fieldapp.crs

/**
 * Registry / parser for canonical preset IDs stored in `CrsConfig.presetId`.
 *
 * Format conventions (also documented in `docs/crs.md`):
 *  - `WGS84_GEO`
 *  - `WGS84_UTM_<zone><N|S>`     — e.g. `WGS84_UTM_36N`
 *  - `GSK2011_GEO`
 *  - `GSK2011_GK_<zone>`         — zone 1..60
 *  - `SK42_GK_<zone>`            — zone 1..60
 *  - `SK95_GK_<zone>`            — zone 1..60
 *  - `WEB_MERC`
 */
object CrsPresets {
    /**
     * Parse a stored preset ID back into a typed [Crs].
     *
     * @return null when the string is malformed or references an unknown CRS family.
     */
    fun parse(presetId: String): Crs? =
        when {
            presetId == Crs.Wgs84Geo.presetId -> Crs.Wgs84Geo
            presetId == Crs.Gsk2011Geo.presetId -> Crs.Gsk2011Geo
            presetId == Crs.WebMercator.presetId -> Crs.WebMercator
            presetId.startsWith("WGS84_UTM_") -> parseUtm(presetId.removePrefix("WGS84_UTM_"))
            presetId.startsWith("GSK2011_GK_") -> parseZoneInt(presetId.removePrefix("GSK2011_GK_"))?.let(Crs::Gsk2011Gk)
            presetId.startsWith("SK42_GK_") -> parseZoneInt(presetId.removePrefix("SK42_GK_"))?.let(Crs::Sk42Gk)
            presetId.startsWith("SK95_GK_") -> parseZoneInt(presetId.removePrefix("SK95_GK_"))?.let(Crs::Sk95Gk)
            else -> null
        }

    /** Minimal MVP catalogue of CRSs to surface in the picker (PRJ-003). */
    val mvpCatalogue: List<Crs> = buildList {
        add(Crs.Wgs84Geo)
        addAll((35..37).map { Crs.Wgs84Utm(zone = it, northern = true) })
        add(Crs.Gsk2011Geo)
        addAll((4..32).map(Crs::Gsk2011Gk))
        addAll((4..32).map(Crs::Sk42Gk))
        addAll((4..32).map(Crs::Sk95Gk))
    }

    private fun parseUtm(remainder: String): Crs.Wgs84Utm? {
        if (remainder.isEmpty()) return null
        val hemiChar = remainder.last()
        if (hemiChar != 'N' && hemiChar != 'S') return null
        val zone = remainder.dropLast(1).toIntOrNull() ?: return null
        if (zone !in 1..60) return null
        return Crs.Wgs84Utm(zone = zone, northern = hemiChar == 'N')
    }

    private fun parseZoneInt(s: String): Int? {
        val zone = s.toIntOrNull() ?: return null
        return if (zone in 1..60) zone else null
    }
}
