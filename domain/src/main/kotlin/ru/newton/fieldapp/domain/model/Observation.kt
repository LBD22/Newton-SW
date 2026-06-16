package ru.newton.fieldapp.domain.model

import kotlinx.serialization.Serializable

/**
 * Coordinate system configuration for a project.
 *
 * `presetId` identifies one of the built-in CRSs (WGS84_GEO, GSK2011_GK_8, ...);
 * `paramsJson` holds any overrides (e.g. custom zone width, user Helmert).
 */
@Serializable
data class CrsConfig(
    val presetId: String,
    val geoid: GeoidConfig,
    val heightMode: HeightMode,
    val paramsJson: String? = null,
    /**
     * Local site calibration (4-param 2D Helmert + vertical offset) solved from
     * control-point pairs. Null = no calibration. Applied AFTER projecting a fix
     * into the projected CRS, on every survey save and re-projection. Default
     * null keeps old CRS-config JSON blobs deserialising cleanly.
     */
    val calibration: CalibrationConfig? = null,
)

/**
 * Persisted result of a local site calibration. The five parameters are the
 * 2D-Helmert coefficients (a = s·cosθ, b = s·sinθ) plus translations and the
 * vertical offset, matching `crs/LocalCalibration.Params`.
 */
@Serializable
data class CalibrationConfig(
    val a: Double,
    val b: Double,
    val dx: Double,
    val dy: Double,
    val dz: Double,
) {
    /** Map a projected (n, e, h) onto the local grid. Mirrors LocalCalibration.Params.apply. */
    fun apply(n: Double, e: Double, h: Double): Triple<Double, Double, Double> =
        Triple(a * n - b * e + dx, b * n + a * e + dy, h + dz)
}

@Serializable
sealed interface GeoidConfig {
    @Serializable data object None : GeoidConfig // ellipsoidal heights only

    @Serializable data object Egm86 : GeoidConfig // built-in

    @Serializable data class UserGrid(
        val gridId: Long,
    ) : GeoidConfig
}

enum class HeightMode { ELLIPSOIDAL, ORTHOMETRIC }

/**
 * Metric snapshot taken at the moment a point was measured.
 *
 * Stored alongside each revision of each Point; exports can include these
 * to provide traceability of data quality.
 */
data class Observation(
    val id: Long,
    val pointId: Long,
    val revision: Int,
    val fixType: String, // "single" | "float" | "fixed" | "ppp_*"
    val sigmaN: Double?,
    val sigmaE: Double?,
    val sigmaH: Double?,
    val hdop: Double?,
    val pdop: Double?,
    val satsUsed: Int?,
    val correctionAgeSec: Double?,
    val epochs: Int,
    val antennaHeight: Double,
    val antennaMethod: AntennaMethod,
    val tiltApplied: Boolean,
    val timestampUtc: Long,
)

enum class AntennaMethod { VERTICAL, SLANT }

/**
 * Quality metadata to persist with a new point (id/pointId assigned by the
 * repository; revision follows the point). Passed to [PointRepository.save]
 * alongside the [NewPoint] so the pair is written in one transaction.
 */
data class NewObservation(
    val fixType: String,
    val sigmaN: Double?,
    val sigmaE: Double?,
    val sigmaH: Double?,
    val hdop: Double?,
    val pdop: Double?,
    val satsUsed: Int?,
    val correctionAgeSec: Double?,
    val epochs: Int,
    val antennaHeightM: Double,
    val antennaMethod: AntennaMethod,
    val tiltApplied: Boolean,
    val timestampUtc: Long,
)
