package ru.newton.fieldapp.crs.transforms

import ru.newton.fieldapp.crs.GeocentricPoint
import ru.newton.fieldapp.crs.HelmertParams

/**
 * 7-parameter Helmert (Bursa-Wolf) transform applied to ECEF coordinates.
 *
 * Implementation uses the small-rotation approximation valid for typical
 * geodetic Helmert parameters (rotations < a few arcseconds): the rotation
 * matrix is treated as `I + Ω`, where Ω is the skew-symmetric matrix of
 * rotation rates. For values orders of magnitude larger this would need the
 * exact rotation matrix, but Russian datums are well within the linear regime.
 */
internal object HelmertTransform {
    private const val ARCSEC_TO_RAD = Math.PI / (180.0 * 3600.0)
    private const val PPM_TO_FACTOR = 1.0e-6

    fun apply(
        point: GeocentricPoint,
        params: HelmertParams,
    ): GeocentricPoint {
        val rx = params.rxArcSec * ARCSEC_TO_RAD
        val ry = params.ryArcSec * ARCSEC_TO_RAD
        val rz = params.rzArcSec * ARCSEC_TO_RAD
        val s = 1.0 + params.scalePpm * PPM_TO_FACTOR

        val xRot = point.xM - rz * point.yM + ry * point.zM
        val yRot = rz * point.xM + point.yM - rx * point.zM
        val zRot = -ry * point.xM + rx * point.yM + point.zM

        return GeocentricPoint(
            xM = params.dxM + s * xRot,
            yM = params.dyM + s * yRot,
            zM = params.dzM + s * zRot,
        )
    }
}
