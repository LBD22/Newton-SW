package ru.newton.fieldapp.data.overlay

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
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
@Serializable
data class MapOverlay(
    val polylines: List<List<LatLon>> = emptyList(),
    val points: List<LatLon> = emptyList(),
) {
    val isEmpty: Boolean get() = polylines.isEmpty() && points.isEmpty()

    companion object {
        val EMPTY = MapOverlay()
    }
}

@Serializable
data class LatLon(val latitudeDeg: Double, val longitudeDeg: Double)

/**
 * Singleton holder so the importer can publish without taking a direct
 * dependency on the survey feature.
 *
 * Persists the current overlay to a small JSON file so an imported DXF survives
 * process death — otherwise the surveyor had to re-import after every app kill.
 */
@Singleton
class MapOverlayHolder
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val json = Json { ignoreUnknownKeys = true }
        private val file: File get() = File(context.filesDir, OVERLAY_FILE)

        private val _overlay = MutableStateFlow<MapOverlay?>(null)
        val overlay: StateFlow<MapOverlay?> = _overlay.asStateFlow()

        init {
            scope.launch {
                runCatching {
                    if (file.exists()) {
                        _overlay.value = json.decodeFromString(MapOverlay.serializer(), file.readText())
                    }
                }
            }
        }

        fun set(overlay: MapOverlay) {
            _overlay.value = overlay
            scope.launch {
                runCatching { file.writeText(json.encodeToString(MapOverlay.serializer(), overlay)) }
            }
        }

        fun clear() {
            _overlay.value = null
            scope.launch { runCatching { file.delete() } }
        }

        private companion object {
            const val OVERLAY_FILE = "map_overlay.json"
        }
    }
