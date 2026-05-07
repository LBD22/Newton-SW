package ru.newton.fieldapp.gnss.data.parsers

/**
 * One row of a GPGSV (or constellation-specific GNGSV/GLGSV/...) sentence:
 * a satellite currently visible to the receiver.
 *
 * Fields are nullable because GSV is allowed to elide unused trailing
 * columns when a sentence is partially populated (last group of a 4-message
 * batch with only 1–3 satellites).
 */
data class SatelliteInView(
    /** PRN — pseudo-random number; identifies the satellite within the constellation. */
    val prn: Int,
    /** Elevation degrees above horizon (0 = at the horizon, 90 = directly overhead). */
    val elevationDeg: Int?,
    /** Azimuth degrees clockwise from true north. */
    val azimuthDeg: Int?,
    /** Carrier-to-noise density in dB-Hz; null when the satellite is not tracked. */
    val snrDbHz: Int?,
    /**
     * Constellation tag derived from the talker id: GP (GPS), GL (GLONASS),
     * GA (Galileo), BD/GB (BeiDou), GN (combined). Used for filtering and
     * colouring on the skyplot.
     */
    val constellation: String,
)
