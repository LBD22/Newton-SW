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
    ) : StakeoutState

    data class Saved(val asBuiltPointId: Long) : StakeoutState

    data class Error(val message: String) : StakeoutState
}
