package ru.newton.fieldapp.features.survey.stakeout

import kotlin.math.hypot
import kotlin.math.sign

/**
 * Stakeout vector to a line segment AB.
 *
 *   off       — perpendicular distance from current position to the line.
 *               Positive when the position is on the **right** of AB walking
 *               from A to B; negative on the left.
 *   along     — projection of current position onto AB measured from A.
 *               Negative if before A, > |AB| if past B (clamping is left to UI).
 *   distanceToFootM — magnitude of `off` (always non-negative — convenient
 *               for tolerance comparisons).
 *   nearestEnd — A or B if the foot is outside the segment, null otherwise.
 */
data class LineStakeoutVector(
    val offM: Double,
    val alongM: Double,
    val distanceToFootM: Double,
    val nearestEnd: Endpoint?,
    val deltaH: Double,
) {
    enum class Endpoint { A, B }

    companion object {
        fun between(
            currentN: Double,
            currentE: Double,
            currentH: Double,
            aN: Double,
            aE: Double,
            aH: Double,
            bN: Double,
            bE: Double,
            bH: Double,
        ): LineStakeoutVector {
            val abN = bN - aN
            val abE = bE - aE
            val abLen = hypot(abN, abE)
            if (abLen < 1.0e-9) {
                // Degenerate: A == B. Fall back to point stakeout against A.
                val pn = currentN - aN
                val pe = currentE - aE
                val d = hypot(pn, pe)
                return LineStakeoutVector(
                    offM = d,
                    alongM = 0.0,
                    distanceToFootM = d,
                    nearestEnd = Endpoint.A,
                    deltaH = aH - currentH,
                )
            }

            val apN = currentN - aN
            val apE = currentE - aE
            // Along-track distance: dot(AP, AB) / |AB|
            val along = (apN * abN + apE * abE) / abLen
            // Cross-track (signed): cross(AB, AP) / |AB|. Positive = right-side
            // of AB walking from A → B in (N, E) plane, where N is "up" and E is
            // "right". cross(AB, AP) = abN*apE - abE*apN.
            val crossSigned = (abN * apE - abE * apN) / abLen
            val nearest = when {
                along < 0.0 -> Endpoint.A
                along > abLen -> Endpoint.B
                else -> null
            }
            // Interpolated target H: blend between aH and bH using `t` clamped to [0,1].
            val t = (along / abLen).coerceIn(0.0, 1.0)
            val footH = aH + t * (bH - aH)
            return LineStakeoutVector(
                offM = crossSigned,
                alongM = along,
                distanceToFootM = kotlin.math.abs(crossSigned).let { absOff ->
                    when (nearest) {
                        // Past an endpoint → distance is to that endpoint, not
                        // to the infinite line.
                        Endpoint.A -> hypot(apN, apE)
                        Endpoint.B -> hypot(currentN - bN, currentE - bE)
                        null -> absOff
                    }
                },
                nearestEnd = nearest,
                deltaH = footH - currentH,
            )
        }
    }
}

@Suppress("unused") // keeps the file's helper visible to tests if they ever need it
internal fun signOf(value: Double): Double = sign(value)
