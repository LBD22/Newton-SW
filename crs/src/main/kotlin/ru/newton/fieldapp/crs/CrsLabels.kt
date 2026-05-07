package ru.newton.fieldapp.crs

/**
 * Human-readable labels for CRS picker UI (PRJ-003) and project headers.
 *
 * Russian wording — geodesists are the primary users of this app. Keep this
 * file's strings in sync with `docs/crs.md`. The function lives in `:crs`
 * (not in `:core:ui`) because the labels are pure data: they do not depend on
 * Android resources or on any Compose machinery.
 */
fun Crs.displayLabel(): String = when (this) {
    is Crs.Wgs84Geo -> "WGS-84 (геогр.)"
    is Crs.Wgs84Utm -> "WGS-84 / UTM зона $zone${if (northern) "N" else "S"}"
    is Crs.Gsk2011Geo -> "ГСК-2011 (геогр.)"
    is Crs.Gsk2011Gk -> "ГСК-2011 / Гаусс-Крюгер, зона $zone (CM ${centralMeridianDeg.toInt()}°)"
    is Crs.Sk42Gk -> "СК-42 / Гаусс-Крюгер, зона $zone (CM ${centralMeridianDeg.toInt()}°)"
    is Crs.Sk95Gk -> "СК-95 / Гаусс-Крюгер, зона $zone (CM ${centralMeridianDeg.toInt()}°)"
    is Crs.WebMercator -> "Web Mercator (только для тайлов)"
}
