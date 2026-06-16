package ru.newton.fieldapp.features.survey.defaults

import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.domain.model.CrsConfig
import ru.newton.fieldapp.domain.model.HeightMode

/**
 * Convert an already-projected, ellipsoidal-height coordinate into the values to
 * STORE for a point, applying — in order — the project's height mode and then
 * its local site calibration.
 *
 * 1. Height mode: when the project is [HeightMode.ORTHOMETRIC] and the receiver
 *    reported a geoid separation N (GGA field 11), store `H_ortho = h − N`.
 *    Falls back to the ellipsoidal height when N is unavailable (returns it
 *    unchanged — the caller should flag "no N" so it isn't a silent error).
 * 2. Calibration: a 2D grid fit, so it only applies on a [Crs.Projected] grid;
 *    a geographic CRS keeps the height conversion but skips calibration.
 *
 * Call right AFTER `CrsTransformer.project`, passing the ellipsoidal height and
 * the (averaged) geoid separation for the point.
 */
internal fun CrsConfig.toStoredCoords(
    crs: Crs,
    n: Double,
    e: Double,
    ellipsoidalH: Double,
    geoidSepN: Double?,
): Triple<Double, Double, Double> {
    val h = if (heightMode == HeightMode.ORTHOMETRIC && geoidSepN != null) {
        ellipsoidalH - geoidSepN
    } else {
        ellipsoidalH
    }
    return if (crs is Crs.Projected) {
        calibration?.apply(n, e, h) ?: Triple(n, e, h)
    } else {
        Triple(n, e, h)
    }
}
