package ru.newton.fieldapp.features.survey.stakeout

import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Stakeout vector — distance + azimuth from the current point to a target.
 *
 * Azimuth is degrees clockwise from north (geodetic convention): 0° = N,
 * 90° = E, 180° = S, 270° = W. Distance is metres in the same CRS as the
 * input ΔN/ΔE — the caller projects raw GNSS into project CRS first.
 */
data class StakeoutVector(
    val distanceM: Double,
    val azimuthDeg: Double,
    val deltaH: Double,
) {
    companion object {
        fun between(
            currentN: Double,
            currentE: Double,
            currentH: Double,
            targetN: Double,
            targetE: Double,
            targetH: Double,
        ): StakeoutVector {
            val dn = targetN - currentN
            val de = targetE - currentE
            val distance = hypot(dn, de)
            val azimuth = (Math.toDegrees(atan2(de, dn)) + 360.0) % 360.0
            return StakeoutVector(
                distanceM = distance,
                azimuthDeg = azimuth,
                deltaH = targetH - currentH,
            )
        }
    }
}
