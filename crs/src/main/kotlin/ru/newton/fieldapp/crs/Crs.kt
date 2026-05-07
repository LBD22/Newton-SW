package ru.newton.fieldapp.crs

/**
 * A coordinate reference system used in the field app.
 *
 * Each instance carries enough information to:
 *  - identify the underlying ellipsoid ([ellipsoid]),
 *  - identify the datum-shift parameters relative to WGS-84 ([helmertFromWgs84]),
 *  - drive the projection (subtype-specific).
 *
 * `presetId` round-trips through Room (it is what `CrsConfig.presetId` stores).
 * Use [CrsPresets.parse] to lift a stored string back to a typed [Crs].
 */
sealed interface Crs {
    val presetId: String
    val ellipsoid: Ellipsoid

    /** Helmert parameters that take **WGS-84 geocentric → this datum geocentric**. */
    val helmertFromWgs84: HelmertParams

    /** Geographic on a given datum (not projected). */
    sealed interface Geographic : Crs

    /** Projected (Gauss-Krüger or UTM) on a given datum. */
    sealed interface Projected : Crs {
        /** Central meridian of the zone, in degrees east. */
        val centralMeridianDeg: Double

        /** False easting in metres (includes zone prefix for Russian GK). */
        val falseEastingM: Double

        /** False northing in metres. */
        val falseNorthingM: Double

        /** Scale factor on the central meridian (1.0 for GK, 0.9996 for UTM). */
        val scaleOnCentralMeridian: Double
    }

    // --- WGS-84 family ---

    data object Wgs84Geo : Geographic {
        override val presetId: String = "WGS84_GEO"
        override val ellipsoid: Ellipsoid get() = Ellipsoid.WGS84
        override val helmertFromWgs84: HelmertParams get() = HelmertParams.IDENTITY
    }

    /** UTM zone N (northern or southern hemisphere). */
    data class Wgs84Utm(
        val zone: Int,
        val northern: Boolean = true,
    ) : Projected {
        init {
            require(zone in 1..60) { "UTM zone must be 1..60, got $zone" }
        }

        override val presetId: String = "WGS84_UTM_${zone}${if (northern) "N" else "S"}"
        override val ellipsoid: Ellipsoid get() = Ellipsoid.WGS84
        override val helmertFromWgs84: HelmertParams get() = HelmertParams.IDENTITY
        override val centralMeridianDeg: Double get() = (zone - 1) * 6.0 - 177.0
        override val falseEastingM: Double get() = 500_000.0
        override val falseNorthingM: Double get() = if (northern) 0.0 else 10_000_000.0
        override val scaleOnCentralMeridian: Double get() = 0.9996
    }

    // --- ГСК-2011 family ---

    data object Gsk2011Geo : Geographic {
        override val presetId: String = "GSK2011_GEO"
        override val ellipsoid: Ellipsoid get() = Ellipsoid.GRS80
        override val helmertFromWgs84: HelmertParams get() = HelmertParams.WGS84_TO_GSK2011
    }

    data class Gsk2011Gk(
        val zone: Int,
    ) : Projected {
        init {
            require(zone in 1..60) { "GK zone must be 1..60, got $zone" }
        }

        override val presetId: String = "GSK2011_GK_$zone"
        override val ellipsoid: Ellipsoid get() = Ellipsoid.GRS80
        override val helmertFromWgs84: HelmertParams get() = HelmertParams.WGS84_TO_GSK2011
        override val centralMeridianDeg: Double get() = (zone - 1) * 6.0 + 3.0
        override val falseEastingM: Double get() = 500_000.0 + zone * 1_000_000.0
        override val falseNorthingM: Double get() = 0.0
        override val scaleOnCentralMeridian: Double get() = 1.0
    }

    // --- СК-42 family ---

    data class Sk42Gk(
        val zone: Int,
    ) : Projected {
        init {
            require(zone in 1..60) { "GK zone must be 1..60, got $zone" }
        }

        override val presetId: String = "SK42_GK_$zone"
        override val ellipsoid: Ellipsoid get() = Ellipsoid.KRASOVSKY1940
        override val helmertFromWgs84: HelmertParams get() = HelmertParams.WGS84_TO_SK42
        override val centralMeridianDeg: Double get() = (zone - 1) * 6.0 + 3.0
        override val falseEastingM: Double get() = 500_000.0 + zone * 1_000_000.0
        override val falseNorthingM: Double get() = 0.0
        override val scaleOnCentralMeridian: Double get() = 1.0
    }

    // --- СК-95 family ---

    data class Sk95Gk(
        val zone: Int,
    ) : Projected {
        init {
            require(zone in 1..60) { "GK zone must be 1..60, got $zone" }
        }

        override val presetId: String = "SK95_GK_$zone"
        override val ellipsoid: Ellipsoid get() = Ellipsoid.KRASOVSKY1940
        override val helmertFromWgs84: HelmertParams get() = HelmertParams.WGS84_TO_SK95
        override val centralMeridianDeg: Double get() = (zone - 1) * 6.0 + 3.0
        override val falseEastingM: Double get() = 500_000.0 + zone * 1_000_000.0
        override val falseNorthingM: Double get() = 0.0
        override val scaleOnCentralMeridian: Double get() = 1.0
    }

    // --- Web Mercator (rendering only) ---

    data object WebMercator : Projected {
        override val presetId: String = "WEB_MERC"
        override val ellipsoid: Ellipsoid get() = Ellipsoid.WGS84
        override val helmertFromWgs84: HelmertParams get() = HelmertParams.IDENTITY
        override val centralMeridianDeg: Double get() = 0.0
        override val falseEastingM: Double get() = 0.0
        override val falseNorthingM: Double get() = 0.0
        override val scaleOnCentralMeridian: Double get() = 1.0
    }
}
