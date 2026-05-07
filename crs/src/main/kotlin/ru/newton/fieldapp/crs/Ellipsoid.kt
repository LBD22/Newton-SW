package ru.newton.fieldapp.crs

import kotlin.math.sqrt

/**
 * A reference ellipsoid for geographic / geocentric coordinates.
 *
 * Constants come from `docs/crs.md` (see authoritative sources cited there).
 * Derived quantities ([flattening], [eccentricitySquared], [secondEccentricitySquared],
 * [polarRadiusM]) are computed once per instance.
 */
data class Ellipsoid(
    /** Semi-major axis (equatorial radius) in metres. */
    val semiMajorM: Double,
    /** Inverse flattening 1/f, dimensionless. */
    val inverseFlattening: Double,
) {
    /** Flattening f. */
    val flattening: Double = 1.0 / inverseFlattening

    /** First eccentricity squared, e². */
    val eccentricitySquared: Double = flattening * (2.0 - flattening)

    /** Semi-minor axis (polar radius) b = a·(1−f). */
    val polarRadiusM: Double = semiMajorM * (1.0 - flattening)

    /** Second eccentricity squared, e'² = e²/(1−e²). */
    val secondEccentricitySquared: Double =
        eccentricitySquared / (1.0 - eccentricitySquared)

    /** Prime vertical radius of curvature N(φ) = a / sqrt(1 − e²·sin²φ). */
    fun primeVerticalRadiusM(latRad: Double): Double {
        val sinLat = kotlin.math.sin(latRad)
        return semiMajorM / sqrt(1.0 - eccentricitySquared * sinLat * sinLat)
    }

    companion object {
        /** Used by WGS-84 datum and Web Mercator tiles. */
        val WGS84 = Ellipsoid(semiMajorM = 6_378_137.0, inverseFlattening = 298.257223563)

        /** Used by ГСК-2011 datum. */
        val GRS80 = Ellipsoid(semiMajorM = 6_378_137.0, inverseFlattening = 298.257222101)

        /** Used by СК-42 and СК-95 datums. */
        val KRASOVSKY1940 = Ellipsoid(semiMajorM = 6_378_245.0, inverseFlattening = 298.3)
    }
}
