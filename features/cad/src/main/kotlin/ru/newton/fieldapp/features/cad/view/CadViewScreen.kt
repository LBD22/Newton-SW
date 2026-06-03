package ru.newton.fieldapp.features.cad.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.crs.CrsPresets
import ru.newton.fieldapp.crs.CrsTransformer
import ru.newton.fieldapp.crs.ProjectedPoint
import ru.newton.fieldapp.data.overlay.LatLon
import ru.newton.fieldapp.data.overlay.MapOverlay
import ru.newton.fieldapp.data.overlay.MapOverlayHolder
import ru.newton.fieldapp.data.preferences.ActiveProjectStore
import ru.newton.fieldapp.domain.repository.PointRepository
import ru.newton.fieldapp.domain.repository.ProjectRepository
import javax.inject.Inject

data class CadPoint(val name: String, val lat: Double, val lon: Double)

data class CadScene(
    val polylines: List<List<LatLon>> = emptyList(),
    val cadPoints: List<LatLon> = emptyList(),
    val projectPoints: List<CadPoint> = emptyList(),
)

@HiltViewModel
class CadViewViewModel
    @OptIn(ExperimentalCoroutinesApi::class)
    @Inject
    constructor(
        overlayHolder: MapOverlayHolder,
        activeProject: ActiveProjectStore,
        projectRepository: ProjectRepository,
        pointRepository: PointRepository,
    ) : ViewModel() {
        private val projectPointsFlow = activeProject.activeId.flatMapLatest { id ->
            if (id == null) {
                flowOf(emptyList())
            } else {
                combine(
                    projectRepository.observeById(id),
                    pointRepository.observePoints(id),
                ) { project, points ->
                    val crs = project?.crsConfig?.presetId?.let(CrsPresets::parse) ?: Crs.Wgs84Geo
                    points.map { p ->
                        when (crs) {
                            is Crs.Geographic -> CadPoint(p.name, p.n, p.e)
                            is Crs.Projected -> {
                                val geo = CrsTransformer.unproject(
                                    ProjectedPoint(p.n, p.e, p.h),
                                    crs,
                                )
                                CadPoint(p.name, geo.latDeg, geo.lonDeg)
                            }
                        }
                    }
                }
            }
        }

        val scene: StateFlow<CadScene> = combine(
            overlayHolder.overlay,
            projectPointsFlow,
        ) { overlay: MapOverlay?, projectPoints ->
            CadScene(
                polylines = overlay?.polylines.orEmpty(),
                cadPoints = overlay?.points.orEmpty(),
                projectPoints = projectPoints,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CadScene())
    }

@Composable
fun CadViewScreen(
    onBack: () -> Unit,
    onImportDxf: () -> Unit,
    viewModel: CadViewViewModel = hiltViewModel(),
) {
    val scene by viewModel.scene.collectAsStateWithLifecycle()
    CadViewContent(scene = scene, onBack = onBack, onImportDxf = onImportDxf)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CadViewContent(
    scene: CadScene,
    onBack: () -> Unit,
    onImportDxf: () -> Unit,
) {
    var pan by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val resetSentinel = remember(scene) { Any() } // resets pan/zoom on scene change

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CAD-вид") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { pan = Offset.Zero; zoom = 1f }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Сбросить вид")
                    }
                    IconButton(onClick = onImportDxf) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Импорт DXF")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val bbox = remember(scene) { computeBoundingBox(scene) }
            if (bbox == null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text(
                        "Пока нечего показать.\n\nИмпортируйте DXF (иконка справа сверху) " +
                            "или снимите хотя бы одну точку — они появятся здесь как " +
                            "техническое отображение без подложки карты.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(resetSentinel) {
                        detectTransformGestures { _, dragOffset, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(0.2f, 50f)
                            pan += dragOffset
                        }
                    },
            ) {
                val w = size.width
                val h = size.height
                // Equirectangular projection: lat/lon → canvas pixels with
                // padding on the bounding box so geometry doesn't touch the
                // edges. Pan/zoom is layered on top.
                val padPx = 24f
                val fitW = w - padPx * 2
                val fitH = h - padPx * 2
                val rangeLon = (bbox.lonMax - bbox.lonMin).takeIf { it > 0 } ?: 1e-9
                val rangeLat = (bbox.latMax - bbox.latMin).takeIf { it > 0 } ?: 1e-9
                val baseScale = minOf(fitW / rangeLon, fitH / rangeLat)
                val scale = baseScale * zoom
                val cx = (bbox.lonMin + bbox.lonMax) / 2
                val cy = (bbox.latMin + bbox.latMax) / 2

                fun project(p: LatLon): Offset {
                    val x = (p.longitudeDeg - cx) * scale + w / 2 + pan.x
                    val y = -(p.latitudeDeg - cy) * scale + h / 2 + pan.y
                    return Offset(x.toFloat(), y.toFloat())
                }
                fun project(lat: Double, lon: Double): Offset =
                    project(LatLon(lat, lon))

                // Polylines (CAD overlay) — blue.
                scene.polylines.forEach { vertices ->
                    if (vertices.size >= 2) {
                        val path = Path().apply {
                            val first = project(vertices.first())
                            moveTo(first.x, first.y)
                            vertices.drop(1).forEach { v ->
                                val pt = project(v)
                                lineTo(pt.x, pt.y)
                            }
                        }
                        drawPath(path, color = CAD_LINE, style = Stroke(width = 3f))
                    }
                }
                // CAD point nodes (DXF POINT) — small empty circles.
                scene.cadPoints.forEach { p ->
                    val pt = project(p)
                    drawCircle(color = CAD_LINE, radius = 4f, center = pt, style = Stroke(width = 2f))
                }
                // Project points — solid green dots, drawn on top.
                scene.projectPoints.forEach { p ->
                    val pt = project(p.lat, p.lon)
                    drawCircle(color = POINT_FILL, radius = 5f, center = pt)
                }
            }
        }
    }
}

private val CAD_LINE = Color(0xFF2C5BB5)
private val POINT_FILL = Color(0xFF4CB85C)

private data class BoundingBox(
    val latMin: Double,
    val latMax: Double,
    val lonMin: Double,
    val lonMax: Double,
)

private fun computeBoundingBox(scene: CadScene): BoundingBox? {
    val all = buildList<Pair<Double, Double>> {
        scene.polylines.forEach { line -> line.forEach { add(it.latitudeDeg to it.longitudeDeg) } }
        scene.cadPoints.forEach { add(it.latitudeDeg to it.longitudeDeg) }
        scene.projectPoints.forEach { add(it.lat to it.lon) }
    }
    if (all.isEmpty()) return null
    return BoundingBox(
        latMin = all.minOf { it.first },
        latMax = all.maxOf { it.first },
        lonMin = all.minOf { it.second },
        lonMax = all.maxOf { it.second },
    )
}
