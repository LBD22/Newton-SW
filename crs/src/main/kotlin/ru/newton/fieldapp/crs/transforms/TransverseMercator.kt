package ru.newton.fieldapp.crs.transforms

import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.crs.Ellipsoid
import ru.newton.fieldapp.crs.GeoPoint
import ru.newton.fieldapp.crs.ProjectedPoint
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Transverse Mercator projection — shared engine for Gauss-Krüger (k₀ = 1.0) and
 * UTM (k₀ = 0.9996). Both differ only in scale factor on the central meridian
 * and the false easting / false northing applied to the projected coordinates,
 * which the [Crs.Projected] descriptor carries.
 *
 * Series truncation: easting series goes to l⁵, northing goes to l⁶, meridional
 * arc and inverse foot-point latitude go to e⁸. This delivers ≤ 1 cm error
 * within ±3° of the central meridian — the regime used for Russian 6° zones.
 *
 * For sub-mm work (tunnel surveys, monitoring networks) extend to l⁷ or switch
 * to Krüger's exponential-series formulation. Out of scope for MVP — see
 * `docs/crs.md` § Gauss-Krüger projection.
 */
internal object TransverseMercator {
    private const val DEG_TO_RAD = Math.PI / 180.0
    private const val RAD_TO_DEG = 180.0 / Math.PI

    fun forward(
        geo: GeoPoint,
        target: Crs.Projected,
    ): ProjectedPoint {
        val ellipsoid = target.ellipsoid
        val latRad = geo.latDeg * DEG_TO_RAD
        val lonRad = geo.lonDeg * DEG_TO_RAD
        val cmRad = target.centralMeridianDeg * DEG_TO_RAD

        val l = lonRad - cmRad
        val sinLat = sin(latRad)
        val cosLat = cos(latRad)
        val tanLat = tan(latRad)

        val n = ellipsoid.primeVerticalRadiusM(latRad)
        val tSq = tanLat * tanLat
        val etaSq = ellipsoid.secondEccentricitySquared * cosLat * cosLat

        val l2 = l * l
        val l3 = l2 * l
        val l4 = l3 * l
        val l5 = l4 * l
        val l6 = l5 * l

        val xPrime =
            n * cosLat * l +
                (n / 6.0) * cosLat * cosLat * cosLat *
                (1.0 - tSq + etaSq) * l3 +
                (n / 120.0) * cosLat * cosLat * cosLat * cosLat * cosLat *
                (5.0 - 18.0 * tSq + tSq * tSq + 14.0 * etaSq - 58.0 * tSq * etaSq) * l5

        val meridionalArc = meridionalArc(latRad, ellipsoid)
        val yPrime =
            meridionalArc +
                (n / 2.0) * sinLat * cosLat * l2 +
                (n / 24.0) * sinLat * cosLat * cosLat * cosLat *
                (5.0 - tSq + 9.0 * etaSq + 4.0 * etaSq * etaSq) * l4 +
                (n / 720.0) * sinLat * cosLat * cosLat * cosLat * cosLat * cosLat *
                (61.0 - 58.0 * tSq + tSq * tSq + 270.0 * etaSq - 330.0 * tSq * etaSq) * l6

        val k0 = target.scaleOnCentralMeridian
        return ProjectedPoint(
            northingM = target.falseNorthingM + k0 * yPrime,
            eastingM = target.falseEastingM + k0 * xPrime,
            heightM = geo.ellipsoidalHeightM,
        )
    }

    fun inverse(
        projected: ProjectedPoint,
        source: Crs.Projected,
    ): GeoPoint {
        val ellipsoid = source.ellipsoid
        val k0 = source.scaleOnCentralMeridian
        val xPrime = (projected.eastingM - source.falseEastingM) / k0
        val yPrime = (projected.northingM - source.falseNorthingM) / k0

        val phiFoot = footPointLatitude(yPrime, ellipsoid)
        val sinPhiF = sin(phiFoot)
        val cosPhiF = cos(phiFoot)
        val tanPhiF = tan(phiFoot)
        val tF2 = tanPhiF * tanPhiF
        val etaF2 = ellipsoid.secondEccentricitySquared * cosPhiF * cosPhiF

        val nF = ellipsoid.primeVerticalRadiusM(phiFoot)
        val mF = ellipsoid.semiMajorM * (1.0 - ellipsoid.eccentricitySquared) /
            (1.0 - ellipsoid.eccentricitySquared * sinPhiF * sinPhiF).let { it * sqrt(it) }

        val x2 = xPrime * xPrime
        val x3 = x2 * xPrime
        val x4 = x3 * xPrime
        val x5 = x4 * xPrime

        val latRad = phiFoot -
            (tanPhiF / (2.0 * mF * nF)) * x2 +
            (tanPhiF / (24.0 * mF * nF * nF * nF)) *
            (5.0 + 3.0 * tF2 + etaF2 - 9.0 * tF2 * etaF2 - 4.0 * etaF2 * etaF2) * x4

        val lonRad = (source.centralMeridianDeg * DEG_TO_RAD) +
            (1.0 / (nF * cosPhiF)) *
            (
                xPrime -
                    (x3 / (6.0 * nF * nF)) * (1.0 + 2.0 * tF2 + etaF2) +
                    (x5 / (120.0 * nF * nF * nF * nF)) *
                    (5.0 + 28.0 * tF2 + 24.0 * tF2 * tF2 + 6.0 * etaF2 + 8.0 * tF2 * etaF2)
            )

        return GeoPoint(
            latDeg = latRad * RAD_TO_DEG,
            lonDeg = lonRad * RAD_TO_DEG,
            ellipsoidalHeightM = projected.heightM,
        )
    }

    /**
     * Meridional arc length from equator to [latRad] — series in e² up to e⁸.
     */
    private fun meridionalArc(
        latRad: Double,
        ellipsoid: Ellipsoid,
    ): Double {
        val a = ellipsoid.semiMajorM
        val e2 = ellipsoid.eccentricitySquared
        val e4 = e2 * e2
        val e6 = e4 * e2
        val e8 = e6 * e2

        val a0 = 1.0 - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0 - 175.0 * e8 / 16384.0
        val a2 = (3.0 / 8.0) * (e2 + e4 / 4.0 + 15.0 * e6 / 128.0 - 455.0 * e8 / 4096.0)
        val a4 = (15.0 / 256.0) * (e4 + 3.0 * e6 / 4.0 - 77.0 * e8 / 128.0)
        val a6 = (35.0 / 3072.0) * (e6 - 41.0 * e8 / 32.0)
        val a8 = (315.0 / 131_072.0) * e8

        return a * (
            a0 * latRad -
                a2 * sin(2.0 * latRad) +
                a4 * sin(4.0 * latRad) -
                a6 * sin(6.0 * latRad) +
                a8 * sin(8.0 * latRad)
        )
    }

    /**
     * Inverse meridional arc → foot-point latitude. Series in `e1`, the
     * "third flattening" derived from `e²`, gives sub-mm latitude error.
     */
    private fun footPointLatitude(
        yPrime: Double,
        ellipsoid: Ellipsoid,
    ): Double {
        val a = ellipsoid.semiMajorM
        val e2 = ellipsoid.eccentricitySquared
        val e4 = e2 * e2
        val e6 = e4 * e2
        val e8 = e6 * e2

        val a0 = 1.0 - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0 - 175.0 * e8 / 16384.0
        val mu = yPrime / (a * a0)

        val e1 = (1.0 - sqrt(1.0 - e2)) / (1.0 + sqrt(1.0 - e2))
        val e1Sq = e1 * e1
        val e1Cb = e1Sq * e1
        val e1Q4 = e1Cb * e1

        return mu +
            (3.0 * e1 / 2.0 - 27.0 * e1Cb / 32.0) * sin(2.0 * mu) +
            (21.0 * e1Sq / 16.0 - 55.0 * e1Q4 / 32.0) * sin(4.0 * mu) +
            (151.0 * e1Cb / 96.0) * sin(6.0 * mu) +
            (1097.0 * e1Q4 / 512.0) * sin(8.0 * mu)
    }
}
