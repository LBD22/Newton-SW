package ru.newton.fieldapp.crs

/**
 * 7-parameter Helmert (Bursa-Wolf) transform between two geocentric systems.
 *
 * Convention: the parameters describe the transform `WGS84 → other`. The inverse
 * is obtained by negating all seven values (see [inverse]).
 *
 * Linear units are metres, rotations are arcseconds (positive = right-handed
 * about the named axis), and scale is parts-per-million.
 *
 * Reference values for Russian datums are pinned in [HelmertParams.Companion]
 * from `docs/crs.md` § Helmert 7-parameter transforms.
 */
data class HelmertParams(
    val dxM: Double,
    val dyM: Double,
    val dzM: Double,
    val rxArcSec: Double,
    val ryArcSec: Double,
    val rzArcSec: Double,
    val scalePpm: Double,
) {
    /** Inverse: simply negate all seven (small-angle approximation). */
    fun inverse(): HelmertParams =
        HelmertParams(
            dxM = -dxM,
            dyM = -dyM,
            dzM = -dzM,
            rxArcSec = -rxArcSec,
            ryArcSec = -ryArcSec,
            rzArcSec = -rzArcSec,
            scalePpm = -scalePpm,
        )

    companion object {
        /** Identity: produces no-op transform. */
        val IDENTITY = HelmertParams(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        /** WGS-84 → ГСК-2011 (Rosreestr 2020 order). */
        val WGS84_TO_GSK2011 = HelmertParams(
            dxM = +0.013,
            dyM = -0.092,
            dzM = -0.030,
            rxArcSec = -0.001,
            ryArcSec = +0.003,
            rzArcSec = +0.002,
            scalePpm = 0.000,
        )

        /** WGS-84 → СК-42 (widely-used parameters; ~1 m absolute accuracy). */
        val WGS84_TO_SK42 = HelmertParams(
            dxM = +23.57,
            dyM = -140.95,
            dzM = -79.80,
            rxArcSec = 0.000,
            ryArcSec = -0.35,
            rzArcSec = -0.79,
            scalePpm = -0.22,
        )

        /** WGS-84 → СК-95. */
        val WGS84_TO_SK95 = HelmertParams(
            dxM = +24.47,
            dyM = -130.89,
            dzM = -81.56,
            rxArcSec = 0.000,
            ryArcSec = 0.000,
            rzArcSec = -0.13,
            scalePpm = -0.22,
        )
    }
}
