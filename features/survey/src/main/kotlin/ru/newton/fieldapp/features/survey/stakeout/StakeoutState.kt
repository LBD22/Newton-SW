package ru.newton.fieldapp.features.survey.stakeout

import ru.newton.fieldapp.gnss.data.FixQuality

sealed interface StakeoutState {
    data object Loading : StakeoutState

    /** No fix yet — show "ждём фикс" hint. */
    data object WaitingForFix : StakeoutState

    data class Active(
        val targetName: String,
        val vector: StakeoutVector,
        val fix: FixQuality,
        val toleranceM: Double = 0.10,
        /**
         * Degrees to ADD to the device's magnetic-north heading to align it with
         * the frame [vector].bearing lives in (grid north for projected CRSs,
         * true north for geographic). Equals magnetic declination − grid
         * convergence; without it the direction arrow is off by 10-25° in Russia.
         */
        val headingCorrectionDeg: Double = 0.0,
    ) : StakeoutState

    data class Saved(val asBuiltPointId: Long) : StakeoutState

    data class Error(val message: String) : StakeoutState
}
