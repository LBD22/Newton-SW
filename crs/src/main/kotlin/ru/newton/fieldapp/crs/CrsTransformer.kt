package ru.newton.fieldapp.crs

import ru.newton.fieldapp.crs.transforms.EllipsoidalConversions
import ru.newton.fieldapp.crs.transforms.HelmertTransform
import ru.newton.fieldapp.crs.transforms.TransverseMercator

/**
 * Top-level orchestrator for moving a point between any two [Crs] instances.
 *
 * Pipeline (worst case, two different datums + projections):
 *
 *   ProjectedA --inverse-TM--> GeoA --geo→ECEF on A's ellipsoid--> ECEF_A
 *   ECEF_A --inverse Helmert(A→WGS-84)--> ECEF_WGS84
 *   ECEF_WGS84 --Helmert(WGS-84→B)--> ECEF_B
 *   ECEF_B --ECEF→geo on B's ellipsoid--> GeoB --forward-TM--> ProjectedB
 *
 * Same-datum and geographic shortcuts skip the ECEF detour to keep round-trip
 * error symmetric. Heights are passed through unchanged at the projection
 * step but participate in the geographic↔ECEF conversion (so an ellipsoidal
 * height on ellipsoid A becomes the ellipsoidal height on B in the output).
 */
object CrsTransformer {
    /** Move a geographic point between two CRSs. */
    fun transformGeo(
        point: GeoPoint,
        source: Crs,
        target: Crs,
    ): GeoPoint {
        if (source.presetId == target.presetId) return point
        if (source.helmertFromWgs84 == target.helmertFromWgs84 &&
            source.ellipsoid == target.ellipsoid
        ) {
            return point
        }

        val sourceEcef = EllipsoidalConversions.geographicToGeocentric(point, source.ellipsoid)
        val ecefWgs84 = HelmertTransform.apply(sourceEcef, source.helmertFromWgs84.inverse())
        val targetEcef = HelmertTransform.apply(ecefWgs84, target.helmertFromWgs84)
        return EllipsoidalConversions.geocentricToGeographic(targetEcef, target.ellipsoid)
    }

    /** Project a geographic point onto the [target] projected CRS. */
    fun project(
        point: GeoPoint,
        source: Crs.Geographic,
        target: Crs.Projected,
    ): ProjectedPoint {
        val onTargetDatum = transformGeo(point, source, target)
        return TransverseMercator.forward(onTargetDatum, target)
    }

    /** Unproject a projected point to geographic on the [source]'s datum. */
    fun unproject(
        point: ProjectedPoint,
        source: Crs.Projected,
    ): GeoPoint = TransverseMercator.inverse(point, source)

    /** Move a projected point between two projected CRSs. */
    fun reproject(
        point: ProjectedPoint,
        source: Crs.Projected,
        target: Crs.Projected,
    ): ProjectedPoint {
        val sourceGeo = TransverseMercator.inverse(point, source)
        val onTargetDatum = transformGeo(sourceGeo, source, target)
        return TransverseMercator.forward(onTargetDatum, target)
    }
}
