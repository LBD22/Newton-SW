package ru.newton.fieldapp.features.survey.defaults

import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.domain.model.CrsConfig

/**
 * Apply the project's local site calibration to an already-projected coordinate.
 *
 * Calibration is a 2D-grid fit (translation + rotation + scale + vertical
 * offset), so it only makes sense on a [Crs.Projected] grid — for a geographic
 * CRS (degrees) it is a no-op. Returns the input unchanged when no calibration
 * is set. Call this right AFTER `CrsTransformer.project`, on every survey save,
 * so stored points land on the customer's local grid (audit M5).
 */
internal fun CrsConfig.applyCalibration(
    crs: Crs,
    n: Double,
    e: Double,
    h: Double,
): Triple<Double, Double, Double> =
    if (crs is Crs.Projected) {
        calibration?.apply(n, e, h) ?: Triple(n, e, h)
    } else {
        Triple(n, e, h)
    }
