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
)

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
