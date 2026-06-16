package ru.newton.fieldapp.features.survey.point

import ru.newton.fieldapp.gnss.data.FixQuality

sealed interface PointSurveyState {
    data object Idle : PointSurveyState

    /** Collecting epochs. [collected] / [target] satisfy `0 < collected ≤ target`. */
    data class Collecting(
        val collected: Int,
        val target: Int,
        val currentFix: FixQuality,
        /**
         * `0L` until the GNSS pipeline emits its first NMEA epoch since this
         * collection started. The UI uses this to distinguish "no fix" (data
         * arrives but no satellite solution) from "no data" (Bluetooth link
         * down, NMEA disabled on the receiver, etc.).
         */
        val lastEpochAtUtc: Long,
    ) : PointSurveyState

    /**
     * Averaging finished. The user can edit name/code and save (or restart).
     */
    data class Ready(
        val averageLat: Double,
        val averageLon: Double,
        val averageH: Double,
        /** Averaged geoid separation N (GGA field 11), for orthometric height. Null if absent. */
        val averageGeoidSep: Double?,
        val sigmaH: Double,
        val sampleCount: Int,
        val name: String = "",
        val code: String = "",
        /** Quick-tap chip labels mirrored from [SurveyDefaults.codeLibrary]. */
        val codeLibrary: List<String> = emptyList(),
    ) : PointSurveyState

    data object Saving : PointSurveyState

    data class Saved(val pointId: Long) : PointSurveyState

    data class Error(val message: String) : PointSurveyState
}
