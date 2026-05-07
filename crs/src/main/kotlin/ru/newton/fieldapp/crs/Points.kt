package ru.newton.fieldapp.crs

/**
 * Geographic point on a specific ellipsoid: latitude, longitude, ellipsoidal height.
 *
 * Latitude / longitude are stored in **degrees**; conversion to radians happens
 * inside the math layer. Height is **always ellipsoidal** at this layer —
 * orthometric/geoid corrections are applied at a higher layer.
 */
data class GeoPoint(
    val latDeg: Double,
    val lonDeg: Double,
    val ellipsoidalHeightM: Double,
)

/**
 * Projected point: northing / easting in metres, plus height (carried through
 * unchanged from the source [GeoPoint] — projections do not affect height).
 *
 * For Russian Gauss-Krüger CRSs the [eastingM] includes the zone prefix
 * (e.g. `8_500_000` on the central meridian of zone 8).
 */
data class ProjectedPoint(
    val northingM: Double,
    val eastingM: Double,
    val heightM: Double,
)

/**
 * Earth-centred, Earth-fixed (ECEF) Cartesian coordinates in metres.
 * Used as the pivot frame for Helmert datum shifts.
 */
data class GeocentricPoint(
    val xM: Double,
    val yM: Double,
    val zM: Double,
)
