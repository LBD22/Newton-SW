package ru.newton.fieldapp.features.survey.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.osmdroid.tileprovider.modules.OfflineTileProvider
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import ru.newton.fieldapp.core.ui.components.NewtonCard
import ru.newton.fieldapp.core.ui.components.NewtonPrimaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSecondaryButton
import ru.newton.fieldapp.core.ui.components.NewtonSuccessButton
import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.crs.CrsPresets
import ru.newton.fieldapp.crs.CrsTransformer
import ru.newton.fieldapp.crs.ProjectedPoint
import ru.newton.fieldapp.data.overlay.MapOverlay
import ru.newton.fieldapp.data.overlay.MapOverlayHolder
import ru.newton.fieldapp.data.preferences.ActiveProjectStore
import ru.newton.fieldapp.data.preferences.OfflineTilesPreferences
import ru.newton.fieldapp.domain.repository.PointRepository
import ru.newton.fieldapp.domain.repository.ProjectRepository
import ru.newton.fieldapp.features.survey.point.PointSurveyState
import ru.newton.fieldapp.features.survey.point.PointSurveyViewModel
import ru.newton.fieldapp.features.survey.stakeout.rememberDeviceHeadingDeg
import ru.newton.fieldapp.gnss.data.FixQuality
import ru.newton.fieldapp.gnss.data.GnssStatus
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import java.io.File
import javax.inject.Inject
import android.graphics.Color as AndroidColor

/**
 * SUR-015 — visibility toggles for the on-map layer groups.
 * Defaults: everything visible. Settings live in-process; not persisted —
 * user-chosen layer state for one survey session shouldn't leak into the next.
 */
data class MapLayers(
    val cadOverlay: Boolean = true,
    val projectPoints: Boolean = true,
)

/** SUR-016 — point made searchable on the map (lat/lon already resolved). */
data class MapPoint(
    val id: Long,
    val name: String,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
)

@HiltViewModel
class MapSurveyViewModel
    @OptIn(ExperimentalCoroutinesApi::class)
    @Inject
    constructor(
        store: GnssStatusStore,
        overlayHolder: MapOverlayHolder,
        offlineTiles: OfflineTilesPreferences,
        activeProject: ActiveProjectStore,
        projectRepository: ProjectRepository,
        pointRepository: PointRepository,
    ) : ViewModel() {
        val status: StateFlow<GnssStatus> = store.status
        val overlay: StateFlow<MapOverlay?> = overlayHolder.overlay
        val offlineFile: StateFlow<File?> = offlineTiles.activeFile
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private val _layers = MutableStateFlow(MapLayers())
        val layers: StateFlow<MapLayers> = _layers

        private val _focused = MutableStateFlow<MapPoint?>(null)

        /** Map widget pans/zooms here when non-null, then we clear it. */
        val focused: StateFlow<MapPoint?> = _focused

        @OptIn(ExperimentalCoroutinesApi::class)
        val projectPoints: StateFlow<List<MapPoint>> = activeProject.activeId
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(emptyList())
                } else {
                    combine(
                        projectRepository.observeById(id),
                        pointRepository.observePoints(id),
                    ) { project, points ->
                        val crs = project?.crsConfig?.presetId?.let(CrsPresets::parse) ?: Crs.Wgs84Geo
                        points.map { p -> p.toMapPoint(crs) }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        fun setLayers(transform: (MapLayers) -> MapLayers) {
            _layers.value = transform(_layers.value)
        }

        fun focusPoint(point: MapPoint) { _focused.value = point }
        fun clearFocus() { _focused.value = null }

        private fun ru.newton.fieldapp.domain.model.Point.toMapPoint(crs: Crs): MapPoint {
            // Geographic on the project datum, or unprojected from the grid.
            val geoOnDatum = when (crs) {
                is Crs.Geographic ->
                    ru.newton.fieldapp.crs.GeoPoint(latDeg = n, lonDeg = e, ellipsoidalHeightM = h)
                is Crs.Projected ->
                    CrsTransformer.unproject(ProjectedPoint(northingM = n, eastingM = e, heightM = h), crs)
            }
            // Stored points live on the project datum (e.g. Krasovsky/СК-42), but
            // OSMDroid tiles and the live GGA marker are WGS-84. Without this datum
            // shift, СК-42 points render ~120-150 m from where the surveyor stands.
            val wgs84 = CrsTransformer.transformGeo(geoOnDatum, crs, Crs.Wgs84Geo)
            return MapPoint(id = id, name = name, latitudeDeg = wgs84.latDeg, longitudeDeg = wgs84.lonDeg)
        }
    }

@Composable
fun MapSurveyScreen(
    onBack: () -> Unit,
    onOpenOptions: () -> Unit = {},
    viewModel: MapSurveyViewModel = hiltViewModel(),
    captureViewModel: PointSurveyViewModel = hiltViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val overlay by viewModel.overlay.collectAsStateWithLifecycle()
    val offlineFile by viewModel.offlineFile.collectAsStateWithLifecycle()
    val layers by viewModel.layers.collectAsStateWithLifecycle()
    val projectPoints by viewModel.projectPoints.collectAsStateWithLifecycle()
    val focused by viewModel.focused.collectAsStateWithLifecycle()
    val capture by captureViewModel.state.collectAsStateWithLifecycle()

    MapSurveyContent(
        status = status,
        overlay = overlay,
        offlineFile = offlineFile,
        layers = layers,
        projectPoints = projectPoints,
        focused = focused,
        capture = capture,
        onBack = onBack,
        onOpenOptions = onOpenOptions,
        onSetLayers = viewModel::setLayers,
        onFocusPoint = viewModel::focusPoint,
        onClearFocus = viewModel::clearFocus,
        onStartCapture = captureViewModel::start,
        onCancelCapture = captureViewModel::cancel,
        onCaptureNameChanged = captureViewModel::onNameChanged,
        onCaptureCodeChanged = captureViewModel::onCodeChanged,
        onSaveCapture = captureViewModel::save,
        onResetCapture = captureViewModel::reset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapSurveyContent(
    status: GnssStatus,
    overlay: MapOverlay?,
    offlineFile: File?,
    layers: MapLayers,
    projectPoints: List<MapPoint>,
    focused: MapPoint?,
    capture: PointSurveyState,
    onBack: () -> Unit,
    onOpenOptions: () -> Unit,
    onSetLayers: ((MapLayers) -> MapLayers) -> Unit,
    onFocusPoint: (MapPoint) -> Unit,
    onClearFocus: () -> Unit,
    onStartCapture: () -> Unit,
    onCancelCapture: () -> Unit,
    onCaptureNameChanged: (String) -> Unit,
    onCaptureCodeChanged: (String) -> Unit,
    onSaveCapture: () -> Unit,
    onResetCapture: () -> Unit,
) {
    var layersDialog by remember { mutableStateOf(false) }
    var searchDialog by remember { mutableStateOf(false) }
    var headingLocked by remember { mutableStateOf(false) }
    val deviceHeadingDeg = rememberDeviceHeadingDeg()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Съёмка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenOptions) {
                        Icon(Icons.Default.Tune, contentDescription = "Параметры съёмки")
                    }
                    IconButton(onClick = { headingLocked = !headingLocked }) {
                        Icon(
                            imageVector = if (headingLocked) Icons.Default.Navigation else Icons.Default.ExploreOff,
                            contentDescription = if (headingLocked) {
                                "В направлении движения"
                            } else {
                                "На север"
                            },
                            tint = if (headingLocked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                    IconButton(onClick = { searchDialog = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Поиск точки")
                    }
                    IconButton(onClick = { layersDialog = true }) {
                        Icon(Icons.Default.Layers, contentDescription = "Слои")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            MapWidget(
                status = status,
                overlay = overlay.takeIf { layers.cadOverlay },
                projectPoints = if (layers.projectPoints) projectPoints else emptyList(),
                offlineFile = offlineFile,
                focused = focused,
                onConsumeFocus = onClearFocus,
                mapOrientationDeg = if (headingLocked) -deviceHeadingDeg else 0f,
                modifier = Modifier.fillMaxSize(),
            )
            StatusReadout(status = status, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
            // North-arrow widget — rotates with the map so the surveyor
            // always sees where geographic north is, regardless of mode.
            CompassRose(
                mapOrientationDeg = if (headingLocked) -deviceHeadingDeg else 0f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            )
            // Map-centric capture flow (replaces the old separate «Снять точку»
            // screen): button → epoch averaging → confirm card → save. The saved
            // point then appears on the map via the reactive projectPoints layer.
            CapturePanel(
                capture = capture,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                onStart = onStartCapture,
                onCancel = onCancelCapture,
                onNameChanged = onCaptureNameChanged,
                onCodeChanged = onCaptureCodeChanged,
                onSave = onSaveCapture,
                onReset = onResetCapture,
            )
        }
    }

    if (layersDialog) {
        LayersDialog(
            layers = layers,
            onChange = { newLayers -> onSetLayers { newLayers } },
            onDismiss = { layersDialog = false },
        )
    }
    if (searchDialog) {
        SearchDialog(
            points = projectPoints,
            onPick = { mp ->
                onFocusPoint(mp)
                searchDialog = false
            },
            onDismiss = { searchDialog = false },
        )
    }
}

@Composable
private fun LayersDialog(
    layers: MapLayers,
    onChange: (MapLayers) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Слои") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LayerRow(
                    label = "Точки проекта",
                    checked = layers.projectPoints,
                    onCheckedChange = { onChange(layers.copy(projectPoints = it)) },
                )
                LayerRow(
                    label = "DXF-подложка",
                    checked = layers.cadOverlay,
                    onCheckedChange = { onChange(layers.copy(cadOverlay = it)) },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )
}

@Composable
private fun LayerRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SearchDialog(
    points: List<MapPoint>,
    onPick: (MapPoint) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, points) {
        if (query.isBlank()) {
            points.take(50)
        } else {
            points.filter { it.name.contains(query, ignoreCase = true) }.take(50)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Поиск точки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (filtered.isEmpty()) {
                    Text(
                        if (points.isEmpty()) "В проекте пока нет точек." else "Ничего не найдено.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(filtered, key = MapPoint::id) { p ->
                            TextButton(
                                onClick = { onPick(p) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    "${p.name}   φ=${"%.6f".format(p.latitudeDeg)}, λ=${"%.6f".format(p.longitudeDeg)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

/**
 * OSMDroid wrapped in [AndroidView]. The marker position is updated on each
 * recomposition driven by [status], without recreating the [MapView].
 *
 * The CAD overlay (CAD-003) is rendered as a separate group of [Polyline] and
 * [Marker] overlays, kept in a dedicated list so we can clear them when the
 * overlay reference changes without touching the live-position marker.
 *
 * SUR-015 / SUR-016 additions: project-points layer is its own list of small
 * green markers; [focused] briefly drives `controller.animateTo` so the search
 * dialog can centre the map on a selected point.
 */
@Composable
private fun MapWidget(
    status: GnssStatus,
    overlay: MapOverlay?,
    projectPoints: List<MapPoint>,
    offlineFile: File?,
    focused: MapPoint?,
    onConsumeFocus: () -> Unit,
    mapOrientationDeg: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            // OSM defaults to (0,0); centre on Moscow until we get a real fix
            // so the user sees Russian tiles immediately rather than the ocean.
            controller.setCenter(GeoPoint(55.7522, 37.6156))
        }
    }
    DisposableEffect(offlineFile) {
        if (offlineFile != null) {
            val tileSource = XYTileSource(
                "Offline",
                MIN_OFFLINE_ZOOM,
                MAX_OFFLINE_ZOOM,
                256,
                ".png",
                arrayOf(),
            )
            mapView.setTileSource(tileSource)
            mapView.tileProvider = OfflineTileProvider(
                SimpleRegisterReceiver(context),
                arrayOf(offlineFile),
            )
        } else {
            mapView.setTileSource(TileSourceFactory.MAPNIK)
        }
        mapView.invalidate()
        onDispose {}
    }
    val marker = remember { Marker(mapView).apply { title = "Текущая позиция" } }
    val cadOverlays = remember { mutableListOf<org.osmdroid.views.overlay.Overlay>() }
    val projectOverlays = remember { mutableListOf<org.osmdroid.views.overlay.Overlay>() }

    DisposableEffect(mapView) {
        mapView.overlays.add(marker)
        onDispose { mapView.onDetach() }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { mv ->
            // Refresh CAD overlay set when the holder publishes a new drawing.
            mv.overlays.removeAll(cadOverlays)
            cadOverlays.clear()
            overlay?.let { ov ->
                ov.polylines.forEach { vertices ->
                    if (vertices.size >= 2) {
                        val poly = Polyline(mv).apply {
                            outlinePaint.color = CAD_LINE_COLOR
                            outlinePaint.strokeWidth = 4f
                            setPoints(vertices.map { GeoPoint(it.latitudeDeg, it.longitudeDeg) })
                        }
                        cadOverlays += poly
                    }
                }
                ov.points.forEach { p ->
                    val m = Marker(mv).apply {
                        position = GeoPoint(p.latitudeDeg, p.longitudeDeg)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    cadOverlays += m
                }
            }

            // Refresh project-points overlays.
            mv.overlays.removeAll(projectOverlays)
            projectOverlays.clear()
            projectPoints.forEach { p ->
                val m = Marker(mv).apply {
                    position = GeoPoint(p.latitudeDeg, p.longitudeDeg)
                    title = p.name
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                projectOverlays += m
            }

            // Re-stack: CAD below, project points above CAD, live marker on top.
            val livePosIdx = mv.overlays.indexOf(marker).coerceAtLeast(0)
            mv.overlays.addAll(livePosIdx, cadOverlays + projectOverlays)

            val lat = status.latitude
            val lon = status.longitude
            if (lat != null && lon != null) {
                val pt = GeoPoint(lat, lon)
                marker.position = pt
                if (status.fix != FixQuality.NoFix && focused == null) {
                    mv.controller.animateTo(pt)
                }
            }
            // Search-dialog focus override — pan to the picked point and clear.
            focused?.let { f ->
                mv.controller.animateTo(GeoPoint(f.latitudeDeg, f.longitudeDeg))
                mv.controller.setZoom(18.0)
                onConsumeFocus()
            }
            // Heading-lock — rotates tiles + overlays around the cursor.
            mv.mapOrientation = mapOrientationDeg
            mv.invalidate()
        },
    )
}

/**
 * Small north-arrow widget pinned to the map corner. The "N" tip rotates by
 * the map's orientation so it always points at geographic north regardless
 * of whether the user enabled heading-lock.
 */
@Composable
private fun CompassRose(mapOrientationDeg: Float, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(40.dp)
                // Map rotates by `mapOrientationDeg`; to point at true north
                // from the user's viewpoint, the rose rotates inversely.
                .rotate(-mapOrientationDeg),
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            // Outline circle
            drawCircle(
                color = outline.copy(alpha = 0.4f),
                radius = w * 0.45f,
                center = Offset(cx, cy),
                style = Stroke(width = 2f),
            )
            // North half (red/accent), south half (grey) — classic compass.
            val northTip = Path().apply {
                moveTo(cx, h * 0.10f)
                lineTo(cx + w * 0.13f, cy)
                lineTo(cx - w * 0.13f, cy)
                close()
            }
            val southTip = Path().apply {
                moveTo(cx, h * 0.90f)
                lineTo(cx + w * 0.13f, cy)
                lineTo(cx - w * 0.13f, cy)
                close()
            }
            drawPath(northTip, accent)
            drawPath(southTip, onSurface.copy(alpha = 0.5f))
            // Centre dot
            drawCircle(color = onSurface, radius = w * 0.035f, center = Offset(cx, cy))
        }
    }
}

private val CAD_LINE_COLOR = AndroidColor.rgb(0x2C, 0x5B, 0xB5)

// MBTiles archives don't expose min/max zoom uniformly; pick a wide enough
// envelope and let the archive supply or skip individual tiles. 0..20 covers
// every common pack we've seen in field tests.
private const val MIN_OFFLINE_ZOOM = 0
private const val MAX_OFFLINE_ZOOM = 20

/**
 * Compact live-position readout pinned to the map's top-left corner. Shows the
 * fix quality and, when the project CRS is projected, the projected N/E/H the
 * surveyor actually works in (falls back to lat/lon for a geographic CRS).
 */
@Composable
private fun StatusReadout(status: GnssStatus, modifier: Modifier = Modifier) {
    NewtonCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Решение: ${fixLabel(status.fix)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            val n = status.n
            val e = status.e
            if (n != null && e != null) {
                Text("N ${"%.3f".format(n)}", style = MaterialTheme.typography.bodyMedium)
                Text("E ${"%.3f".format(e)}", style = MaterialTheme.typography.bodyMedium)
                status.h?.let { Text("H ${"%.3f".format(it)}", style = MaterialTheme.typography.bodyMedium) }
            } else if (status.latitude != null) {
                Text("φ ${"%.7f".format(status.latitude)}°", style = MaterialTheme.typography.bodyMedium)
                Text("λ ${"%.7f".format(status.longitude ?: 0.0)}°", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Нет координат", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Bottom capture controls: button → averaging → confirm card → save. */
@Composable
private fun CapturePanel(
    capture: PointSurveyState,
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onNameChanged: (String) -> Unit,
    onCodeChanged: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
) {
    Box(modifier = modifier) {
        when (capture) {
            is PointSurveyState.Idle -> NewtonSuccessButton(
                onClick = onStart,
                text = "Снять точку",
                icon = Icons.Default.MyLocation,
                modifier = Modifier.fillMaxWidth(),
            )

            is PointSurveyState.Collecting -> NewtonCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Сбор эпох ${capture.collected}/${capture.target} · ${fixLabel(capture.currentFix)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (capture.target > 0) capture.collected.toFloat() / capture.target else 0f
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    NewtonSecondaryButton(
                        onClick = onCancel,
                        text = "Отмена",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            is PointSurveyState.Ready -> NewtonCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "φ ${"%.7f".format(capture.averageLat)}°  λ ${"%.7f".format(capture.averageLon)}°",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "H ${"%.3f".format(capture.averageH)} м · σH ${"%.3f".format(capture.sigmaH)} м · ${capture.sampleCount} эп.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = capture.name,
                        onValueChange = onNameChanged,
                        label = { Text("Имя точки") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = capture.code,
                        onValueChange = onCodeChanged,
                        label = { Text("Код (необязательно)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NewtonSecondaryButton(
                            onClick = onCancel,
                            text = "Отмена",
                            modifier = Modifier.weight(1f),
                        )
                        NewtonSuccessButton(
                            onClick = onSave,
                            text = "Сохранить",
                            icon = Icons.Default.Check,
                            enabled = capture.name.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            is PointSurveyState.Saving -> NewtonCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Сохранение…", style = MaterialTheme.typography.bodyMedium)
                }
            }

            is PointSurveyState.Saved -> {
                // Brief confirmation, then return to Idle so the button is ready
                // for the next point. The saved point is already on the map.
                LaunchedEffect(capture) {
                    delay(1_500)
                    onReset()
                }
                NewtonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Точка сохранена ✓",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            is PointSurveyState.Error -> NewtonCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        capture.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    NewtonPrimaryButton(
                        onClick = onReset,
                        text = "ОК",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun fixLabel(fix: FixQuality): String = when (fix) {
    FixQuality.NoFix -> "нет"
    FixQuality.Single -> "Single"
    FixQuality.DGnss -> "DGPS"
    FixQuality.FloatRtk -> "Float"
    FixQuality.FixedRtk -> "Fix"
    is FixQuality.Ppp -> "PPP"
}
