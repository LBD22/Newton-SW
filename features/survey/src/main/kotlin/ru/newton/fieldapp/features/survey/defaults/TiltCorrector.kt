package ru.newton.fieldapp.features.survey.defaults

import ru.newton.fieldapp.gnss.data.GnssStatus
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Applies pole-tilt correction to a GNSS solution.
 *
 * The Newton receiver's IMU emits TRA messages with `pitch`, `roll`, `heading`
 * (degrees). When the surveyor tilts the pole away from vertical, the antenna
 * phase centre is offset from the actual ground point under the pole tip:
 *
 * ```
 *      antenna
 *        /
 *       / ← pole tilted
 *      /
 *     X  ← ground point we want (sometimes "tip" here)
 * ```
 *
 * Correction in local pole frame:
 *  - forward offset = poleHeight · sin(pitch)
 *  - right offset   = poleHeight · sin(roll)
 *  - vertical drop  = poleHeight · (1 − cos(tilt))   where tilt² ≈ pitch² + roll²
 *
 * Heading converts (forward, right) to local ENU (north, east). Lat/lon
 * displacement uses the spherical small-distance approximation — accuracy
 * is better than 1 mm for displacements under 1 m, which is the regime of
 * any reasonable pole tilt.
 *
 * Returns the GnssStatus unchanged when [poleHeightM] ≤ 0 or the IMU isn't
 * valid — fail-safe for use during collection where we'd rather skip an
 * epoch than poison it with garbage.
 */
object TiltCorrector {
    private const val METRES_PER_DEGREE_LAT = 111_320.0

    fun apply(status: GnssStatus, poleHeightM: Double): GnssStatus {
        if (poleHeightM <= 0.0) return status
        if (!status.imuValid) return status
        val lat = status.latitude ?: return status
        val lon = status.longitude ?: return status
        val pitchDeg = status.pitchDeg ?: return status
        val rollDeg = status.rollDeg ?: return status
        val headingDeg = status.headingDeg ?: 0.0
        val height = status.ellipsoidalHeight ?: 0.0

        val pitch = Math.toRadians(pitchDeg)
        val roll = Math.toRadians(rollDeg)
        val heading = Math.toRadians(headingDeg)

        // Pole-frame offsets — antenna is "above" by poleHeight on a vertical pole;
        // the ground tip is poleHeight below along the pole's local vertical.
        val forwardOffset = poleHeightM * sin(pitch)
        val rightOffset = poleHeightM * sin(roll)
        val tiltMagnitude = sqrt(forwardOffset * forwardOffset + rightOffset * rightOffset)
            .coerceAtMost(poleHeightM)
        // Pythagorean reconstruction of the vertical leg keeps the total pole
        // length exact even when pitch²+roll² approaches a real tilt angle.
        val verticalLeg = sqrt(poleHeightM * poleHeightM - tiltMagnitude * tiltMagnitude)

        // Project (forward, right) onto local ENU using heading.
        // forward axis points along heading; right axis is heading+90°.
        val northOffset = forwardOffset * cos(heading) - rightOffset * sin(heading)
        val eastOffset = forwardOffset * sin(heading) + rightOffset * cos(heading)

        // Ground tip = antenna − offset (ground is "down and to the side").
        val groundLat = lat - northOffset / METRES_PER_DEGREE_LAT
        val lonDenominator = METRES_PER_DEGREE_LAT * cos(Math.toRadians(lat))
        val groundLon = if (lonDenominator != 0.0) {
            lon - eastOffset / lonDenominator
        } else {
            lon
        }
        val groundHeight = height - verticalLeg

        return status.copy(
            latitude = groundLat,
            longitude = groundLon,
            ellipsoidalHeight = groundHeight,
        )
    }
}
