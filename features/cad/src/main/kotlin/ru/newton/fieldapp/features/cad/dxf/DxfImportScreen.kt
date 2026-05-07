package ru.newton.fieldapp.features.cad.dxf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.crs.CrsPresets
import ru.newton.fieldapp.crs.CrsTransformer
import ru.newton.fieldapp.crs.ProjectedPoint
import ru.newton.fieldapp.data.overlay.LatLon
import ru.newton.fieldapp.data.overlay.MapOverlay
import ru.newton.fieldapp.data.overlay.MapOverlayHolder
import ru.newton.fieldapp.data.preferences.ActiveProjectStore
import ru.newton.fieldapp.domain.repository.ProjectRepository
import javax.inject.Inject

@HiltViewModel
class DxfImportViewModel
    @Inject
    constructor(
        private val activeProject: ActiveProjectStore,
        private val projectRepository: ProjectRepository,
        private val importUseCase: DxfImportUseCase,
        private val overlayHolder: MapOverlayHolder,
    ) : ViewModel() {
        private val _state = MutableStateFlow<DxfImportState>(DxfImportState.Idle)
        val state: StateFlow<DxfImportState> = _state.asStateFlow()

        /**
         * Step 1 — parse the file just enough to enumerate layers; do NOT
         * touch the database yet. The user reviews the layer list and clicks
         * "Импортировать выбранные" to proceed.
         */
        fun analyse(dxfText: String) {
            viewModelScope.launch {
                _state.value = DxfImportState.Working
                runCatching {
                    val drawing = DxfReader.parse(dxfText)
                    drawing to drawing.layerSummaries()
                }.onSuccess { (drawing, layers) ->
                    _state.value = DxfImportState.Picking(
                        dxfText = dxfText,
                        drawing = drawing,
                        layers = layers,
                        selected = layers.map { it.layer }.toSet(),
                    )
                }.onFailure {
                    _state.value = DxfImportState.Failed(it.message ?: "Не удалось разобрать DXF")
                }
            }
        }

        fun toggleLayer(layer: String) {
            val s = _state.value as? DxfImportState.Picking ?: return
            val newSelected = if (layer in s.selected) s.selected - layer else s.selected + layer
            _state.value = s.copy(selected = newSelected)
        }

        fun selectAll() {
            val s = _state.value as? DxfImportState.Picking ?: return
            _state.value = s.copy(selected = s.layers.map { it.layer }.toSet())
        }

        fun selectNone() {
            val s = _state.value as? DxfImportState.Picking ?: return
            _state.value = s.copy(selected = emptySet())
        }

        /** Step 2 — commit the selected layers. */
        fun importSelected() {
            val s = _state.value as? DxfImportState.Picking ?: return
            if (s.selected.isEmpty()) return
            viewModelScope.launch {
                _state.value = DxfImportState.Working
                runCatching {
                    val activeId = activeProject.activeId.firstOrNull()
                        ?: error("Активный проект не выбран")
                    val res = importUseCase(
                        projectId = activeId,
                        dxfText = s.dxfText,
                        allowedLayers = s.selected,
                    )
                    val project = projectRepository.observeById(activeId).firstOrNull()
                    val crs = project?.crsConfig?.presetId?.let(CrsPresets::parse) ?: Crs.Wgs84Geo
                    overlayHolder.set(s.drawing.toOverlay(crs, s.selected))
                    res
                }.onSuccess { res ->
                    _state.value = DxfImportState.Done(
                        saved = res.savedPointCount,
                        entities = res.totalEntities,
                    )
                }.onFailure {
                    _state.value = DxfImportState.Failed(it.message ?: "Не удалось импортировать DXF")
                }
            }
        }

        fun reset() { _state.value = DxfImportState.Idle }

        /**
         * Convert a parsed DXF drawing into a WGS-84 overlay. DXF stores `x` as
         * easting, `y` as northing, `z` as ellipsoidal height — same convention
         * as our `ProjectedPoint`. For a geographic project CRS, the values are
         * passed through (degrees). For a projected CRS, [CrsTransformer.unproject]
         * maps each vertex to lat/lon.
         */
        private fun DxfDrawing.toOverlay(crs: Crs, allowedLayers: Set<String>): MapOverlay {
            fun toLatLon(x: Double, y: Double, z: Double): LatLon = when (crs) {
                is Crs.Projected -> {
                    val geo = CrsTransformer.unproject(
                        ProjectedPoint(northingM = y, eastingM = x, heightM = z),
                        crs,
                    )
                    LatLon(latitudeDeg = geo.latDeg, longitudeDeg = geo.lonDeg)
                }
                is Crs.Geographic -> LatLon(latitudeDeg = y, longitudeDeg = x)
            }

            val polylines = mutableListOf<List<LatLon>>()
            val points = mutableListOf<LatLon>()
            for (entity in entities) {
                if (entity.layer !in allowedLayers) continue
                when (entity) {
                    is DxfEntity.Point -> points += toLatLon(entity.x, entity.y, entity.z)
                    is DxfEntity.Line -> polylines += listOf(
                        toLatLon(entity.x1, entity.y1, entity.z1),
                        toLatLon(entity.x2, entity.y2, entity.z2),
                    )
                    is DxfEntity.Polyline -> polylines += entity.vertices.map { v -> toLatLon(v.x, v.y, v.z) }
                    is DxfEntity.Circle -> {
                        // Approximate as a closed 36-vertex polyline.
                        val verts = (0..36).map { step ->
                            val angle = Math.toRadians(step * 10.0)
                            val px = entity.cx + entity.radius * kotlin.math.cos(angle)
                            val py = entity.cy + entity.radius * kotlin.math.sin(angle)
                            toLatLon(px, py, entity.cz)
                        }
                        polylines += verts
                    }
                    is DxfEntity.Arc -> {
                        val s = entity.startAngleDeg
                        val e = if (entity.endAngleDeg < s) entity.endAngleDeg + 360 else entity.endAngleDeg
                        val steps = ((e - s) / 5.0).toInt().coerceAtLeast(2)
                        val verts = (0..steps).map { step ->
                            val angle = Math.toRadians(s + step * (e - s) / steps)
                            val px = entity.cx + entity.radius * kotlin.math.cos(angle)
                            val py = entity.cy + entity.radius * kotlin.math.sin(angle)
                            toLatLon(px, py, entity.cz)
                        }
                        polylines += verts
                    }
                    is DxfEntity.Text -> points += toLatLon(entity.x, entity.y, entity.z)
                }
            }
            return MapOverlay(polylines = polylines, points = points)
        }
    }

sealed interface DxfImportState {
    data object Idle : DxfImportState
    data object Working : DxfImportState
    data class Picking(
        val dxfText: String,
        val drawing: DxfDrawing,
        val layers: List<DxfLayerSummary>,
        val selected: Set<String>,
    ) : DxfImportState
    data class Done(val saved: Int, val entities: Int) : DxfImportState
    data class Failed(val message: String) : DxfImportState
}

@Composable
fun DxfImportScreen(
    onBack: () -> Unit,
    viewModel: DxfImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lastError by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.onSuccess { text ->
                if (text == null) lastError = "Не удалось прочитать файл" else viewModel.analyse(text)
            }.onFailure { lastError = it.message }
        }
    }

    DxfImportContent(
        state = state,
        lastError = lastError,
        onBack = onBack,
        onPickFile = { launcher.launch(arrayOf("*/*")) },
        onToggleLayer = viewModel::toggleLayer,
        onSelectAll = viewModel::selectAll,
        onSelectNone = viewModel::selectNone,
        onImportSelected = viewModel::importSelected,
        onReset = { viewModel.reset(); lastError = null },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DxfImportContent(
    state: DxfImportState,
    lastError: String?,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    onToggleLayer: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onImportSelected: () -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Импорт DXF") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Импорт сохранит все POINT-сущности как точки, LINE — как пары " +
                    "точек *_A/*_B, LWPOLYLINE — как вершины *_v0..vN-1. " +
                    "Слой записывается в поле «код».",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Button(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
                Text(if (state is DxfImportState.Idle) "Выбрать DXF…" else "Выбрать другой DXF…")
            }
            when (state) {
                DxfImportState.Idle -> Unit
                DxfImportState.Working -> Text("Обработка…", style = MaterialTheme.typography.titleMedium)
                is DxfImportState.Picking -> LayerPicker(
                    state = state,
                    onToggle = onToggleLayer,
                    onSelectAll = onSelectAll,
                    onSelectNone = onSelectNone,
                    onImport = onImportSelected,
                )
                is DxfImportState.Done -> ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Сохранено точек: ${state.saved} из ${state.entities} сущностей.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Button(onClick = onReset, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Импортировать ещё")
                        }
                    }
                }
                is DxfImportState.Failed -> Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            lastError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun LayerPicker(
    state: DxfImportState.Picking,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onImport: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Слои в файле (${state.layers.size}). Снимите галочку, чтобы " +
                    "пропустить слой.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSelectAll) { Text("Все") }
                TextButton(onClick = onSelectNone) { Text("Никакие") }
            }
            state.layers.forEach { summary ->
                val checked = summary.layer in state.selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = checked, onValueChange = { onToggle(summary.layer) })
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = { onToggle(summary.layer) })
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(summary.layer, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${summary.entityCount} сущностей",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Button(
                onClick = onImport,
                enabled = state.selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Импортировать выбранные (${state.selected.size})")
            }
        }
    }
}
