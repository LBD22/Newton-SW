package ru.newton.fieldapp.gnss.data

import ru.newton.fieldapp.gnss.data.parsers.SatelliteInView

/**
 * A consistent snapshot of the current GNSS state for one receiver.
 *
 * Updates are atomic: UI collecting `GnssStatusStore.status` always sees a
 * coherent view (e.g. never GGA fields from T1 mixed with GST fields from T2).
 *
 * Nullable fields (`latitude`, `hdop`, `sigmaH`, ...) indicate "not yet
 * received or not provided by the current sentence mix" — not "zero".
 */
data class GnssStatus(
    val fix: FixQuality,
    // Position from GPGGA (geographic)
    val latitude: Double?,
    val longitude: Double?,
    /** Ellipsoidal height, meters. */
    val ellipsoidalHeight: Double?,
    // Position projected into the current project CRS (filled by upstream)
    val n: Double?,
    val e: Double?,
    /** Orthometric height, meters. Null if project uses ellipsoidal heights. */
    val h: Double?,
    // Accuracy from GPGST
    val sigmaN: Double?,
    val sigmaE: Double?,
    val sigmaH: Double?,
    // DOP from GPGSA
    val hdop: Double?,
    val pdop: Double?,
    val vdop: Double?,
    // Satellites
    val satsUsed: Int,
    val satsVisible: Int,
    /** Age of corrections in seconds, from GPGGA field 13. Null if not available. */
    val correctionAgeSec: Double?,
    // Heading from ORIENT / IMU
    val headingDeg: Double?,
    val pitchDeg: Double?,
    val rollDeg: Double?,
    val imuValid: Boolean,
    /** When this snapshot was produced, epoch millis. */
    val timestampUtc: Long,
    /**
     * GPGSV-derived skyplot. Aggregator merges multi-message GSV batches into
     * a single list keyed by `(constellation, prn)`; UI treats it as a flat
     * "currently visible" snapshot. Empty until at least one full GSV batch
     * has been received.
     */
    val satellitesInView: List<SatelliteInView> = emptyList(),
) {
    companion object {
        fun initial(): GnssStatus =
            GnssStatus(
                fix = FixQuality.NoFix,
                latitude = null,
                longitude = null,
                ellipsoidalHeight = null,
                n = null,
                e = null,
                h = null,
                sigmaN = null,
                sigmaE = null,
                sigmaH = null,
                hdop = null,
                pdop = null,
                vdop = null,
                satsUsed = 0,
                satsVisible = 0,
                correctionAgeSec = null,
                headingDeg = null,
                pitchDeg = null,
                rollDeg = null,
                imuValid = false,
                timestampUtc = 0L,
                satellitesInView = emptyList(),
            )
    }
}
