package ru.newton.fieldapp.gnss.data

/**
 * Quality of the current GNSS fix, derived from GPGGA field 6 and related NMEA.
 *
 * Mapping from GPGGA quality field:
 *   0 → NoFix
 *   1 → Single
 *   2 → DGnss
 *   4 → FixedRtk
 *   5 → FloatRtk
 *   6–9 → various PPP / SBAS — see Ppp subtype.
 */
sealed interface FixQuality {
    data object NoFix : FixQuality

    data object Single : FixQuality

    data object DGnss : FixQuality

    data object FloatRtk : FixQuality

    data object FixedRtk : FixQuality

    data class Ppp(
        val type: String,
    ) : FixQuality

    /** True iff the fix is at-or-better than the given baseline. */
    fun isAtLeast(baseline: FixQuality): Boolean = ordinal() >= baseline.ordinal()

    private fun ordinal(): Int =
        when (this) {
            NoFix -> 0
            Single -> 1
            DGnss -> 2
            is Ppp -> 3
            FloatRtk -> 4
            FixedRtk -> 5
        }
}
