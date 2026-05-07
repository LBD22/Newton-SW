package ru.newton.fieldapp.crs.geoid

/**
 * Geoid undulation source: produces N(lat, lon) — the height of the geoid
 * above the ellipsoid in metres. Positive in most of Russia (≈ +10..+30 m).
 *
 *   H_orthometric = h_ellipsoidal − N
 *
 * Implementations are pluggable: built-in EGM-86, user-supplied .gtx grid, or
 * a constant zero (`NoGeoid`) when the project uses ellipsoidal heights only.
 */
interface Geoid {
    /** Geoid undulation N(lat, lon) in metres. */
    fun undulationM(
        latDeg: Double,
        lonDeg: Double,
    ): Double
}

/** Identity geoid: returns 0 m everywhere. Use for ellipsoidal-height projects. */
object NoGeoid : Geoid {
    override fun undulationM(
        latDeg: Double,
        lonDeg: Double,
    ): Double = 0.0
}
