package ru.newton.fieldapp.crs.transforms

import ru.newton.fieldapp.crs.Ellipsoid
import ru.newton.fieldapp.crs.GeoPoint
import ru.newton.fieldapp.crs.GeocentricPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geographic ↔ ECEF (Earth-Centred Earth-Fixed) conversions on a given ellipsoid.
 *
 * Forward (geographic→ECEF) is closed-form. Inverse uses Heiskanen-Moritz
 * iteration on latitude with `h` updated each pass — converges quadratically
 * to ≤ 0.1 mm in ≤ 4 iterations for any reasonable surface point. We run 6
 * iterations defensively.
 */
internal object EllipsoidalConversions {
    private const val DEG_TO_RAD = Math.PI / 180.0
    private const val RAD_TO_DEG = 180.0 / Math.PI
    private const val ITERATIONS = 6
    private const val POLE_EPSILON = 1.0e-12

    fun geographicToGeocentric(
        point: GeoPoint,
        ellipsoid: Ellipsoid,
    ): GeocentricPoint {
        val latRad = point.latDeg * DEG_TO_RAD
        val lonRad = point.lonDeg * DEG_TO_RAD
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val sinLon = sin(lonRad)
        val cosLon = cos(lonRad)

        val n = ellipsoid.primeVerticalRadiusM(latRad)
        val nPlusH = n + point.ellipsoidalHeightM

        return GeocentricPoint(
            xM = nPlusH * cosLat * cosLon,
            yM = nPlusH * cosLat * sinLon,
            zM = (n * (1.0 - ellipsoid.eccentricitySquared) + point.ellipsoidalHeightM) * sinLat,
        )
    }

    fun geocentricToGeographic(
        point: GeocentricPoint,
        ellipsoid: Ellipsoid,
    ): GeoPoint {
        val a = ellipsoid.semiMajorM
        val e2 = ellipsoid.eccentricitySquared
        val p = sqrt(point.xM * point.xM + point.yM * point.yM)
        val lonRad = atan2(point.yM, point.xM)

        // Pole singularity guard: when p≈0 we are on (or very near) the spin axis.
        if (p < POLE_EPSILON) {
            val signZ = if (point.zM >= 0.0) 1.0 else -1.0
            val height = abs(point.zM) - ellipsoid.polarRadiusM
            return GeoPoint(
                latDeg = signZ * 90.0,
                lonDeg = 0.0,
                ellipsoidalHeightM = height,
            )
        }

        // Heiskanen-Moritz iteration: latitude refined each pass with current N(φ) + h.
        var latRad = atan2(point.zM, p * (1.0 - e2))
        var height = 0.0
        repeat(ITERATIONS) {
            val sinLat = sin(latRad)
            val cosLat = cos(latRad)
            val n = a / sqrt(1.0 - e2 * sinLat * sinLat)
            height = p / cosLat - n
            latRad = atan2(point.zM, p * (1.0 - e2 * n / (n + height)))
        }

        return GeoPoint(
            latDeg = latRad * RAD_TO_DEG,
            lonDeg = lonRad * RAD_TO_DEG,
            ellipsoidalHeightM = height,
        )
    }
}
