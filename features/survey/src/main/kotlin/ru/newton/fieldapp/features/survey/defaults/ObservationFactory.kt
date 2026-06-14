package ru.newton.fieldapp.features.survey.defaults

import ru.newton.fieldapp.domain.model.AntennaMethod
import ru.newton.fieldapp.domain.model.NewObservation
import ru.newton.fieldapp.gnss.data.FixQuality
import ru.newton.fieldapp.gnss.data.GnssStatus

/**
 * Builds the quality [NewObservation] persisted with a surveyed point from the
 * epochs that produced it — so a Fixed-RTK point is later distinguishable from a
 * Single point with metre-level error (audit C5/M6).
 *
 * Records the WORST fix among the epochs (conservative) and averages the σ/DOP
 * the receiver reported. `antennaHeightM` is the pole height that was already
 * subtracted from the stored coordinate; `tiltApplied` marks IMU lean removal.
 */
object ObservationFactory {
    fun fromSamples(
        samples: List<GnssStatus>,
        antennaHeightM: Double,
        tiltApplied: Boolean,
        timestampUtc: Long = System.currentTimeMillis(),
    ): NewObservation {
        val worstFix = samples.minByOrNull { it.fix.rank() }?.fix ?: FixQuality.NoFix
        return NewObservation(
            fixType = worstFix.label(),
            sigmaN = samples.mapNotNull { it.sigmaN }.averageOrNull(),
            sigmaE = samples.mapNotNull { it.sigmaE }.averageOrNull(),
            sigmaH = samples.mapNotNull { it.sigmaH }.averageOrNull(),
            hdop = samples.mapNotNull { it.hdop }.averageOrNull(),
            pdop = samples.mapNotNull { it.pdop }.averageOrNull(),
            satsUsed = samples.maxOfOrNull { it.satsUsed },
            correctionAgeSec = samples.mapNotNull { it.correctionAgeSec }.lastOrNull(),
            epochs = samples.size,
            antennaHeightM = antennaHeightM,
            antennaMethod = if (tiltApplied) AntennaMethod.SLANT else AntennaMethod.VERTICAL,
            tiltApplied = tiltApplied,
            timestampUtc = timestampUtc,
        )
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private fun FixQuality.label(): String = when (this) {
        FixQuality.NoFix -> "none"
        FixQuality.Single -> "single"
        FixQuality.DGnss -> "dgnss"
        FixQuality.FloatRtk -> "float"
        FixQuality.FixedRtk -> "fixed"
        is FixQuality.Ppp -> "ppp"
    }

    private fun FixQuality.rank(): Int = when (this) {
        FixQuality.NoFix -> 0
        FixQuality.Single -> 1
        FixQuality.DGnss -> 2
        is FixQuality.Ppp -> 3
        FixQuality.FloatRtk -> 4
        FixQuality.FixedRtk -> 5
    }
}
