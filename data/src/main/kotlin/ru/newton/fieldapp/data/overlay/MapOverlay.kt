package ru.newton.fieldapp.data.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Geographic overlay shared between feature modules — produced by the DXF
 * import (CAD-003) and consumed by the map survey screen.
 *
 * Coordinates are WGS-84 lat/lon (degrees) — the importer projects from the
 * active project's CRS so the map screen doesn't need to know about CRS at
 * render time. Empty `polylines` and `points` mean "no overlay".
 *
 * Lives in `:data` (not `:features:*`) because two feature modules share it
 * and `:features:* ↛ :features:*` is a hard architecture invariant.
 */
data class MapOverlay(
    val polylines: List<List<LatLon>> = emptyList(),
    val points: List<LatLon> = emptyList(),
) {
    val isEmpty: Boolean get() = polylines.isEmpty() && points.isEmpty()

    companion object {
        val EMPTY = MapOverlay()
    }
}

data class LatLon(val latitudeDeg: Double, val longitudeDeg: Double)

/**
 * Singleton holder so the importer can publish without taking a direct
 * dependency on the survey feature.
 */
@Singleton
class MapOverlayHolder
    @Inject
    constructor() {
        private val _overlay = MutableStateFlow<MapOverlay?>(null)
        val overlay: StateFlow<MapOverlay?> = _overlay.asStateFlow()

        fun set(overlay: MapOverlay) {
            _overlay.value = overlay
        }

        fun clear() {
            _overlay.value = null
        }
    }
