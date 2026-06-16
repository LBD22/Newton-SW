package ru.newton.fieldapp.features.survey.line

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.crs.CrsPresets
import ru.newton.fieldapp.crs.CrsTransformer
import ru.newton.fieldapp.crs.GeoPoint
import ru.newton.fieldapp.data.preferences.ActiveProjectStore
import ru.newton.fieldapp.domain.model.NewPoint
import ru.newton.fieldapp.domain.model.PointSource
import ru.newton.fieldapp.domain.repository.PointRepository
import ru.newton.fieldapp.domain.repository.ProjectRepository
import ru.newton.fieldapp.features.survey.defaults.ObservationFactory
import ru.newton.fieldapp.features.survey.defaults.SurveyPreferences
import ru.newton.fieldapp.features.survey.defaults.TiltCorrector
import ru.newton.fieldapp.features.survey.defaults.applyCalibration
import ru.newton.fieldapp.gnss.data.FixQuality
import ru.newton.fieldapp.gnss.data.GnssStatus
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class LineSurveyViewModel
    @Inject
    constructor(
        private val store: GnssStatusStore,
        private val preferences: SurveyPreferences,
        private val projectRepository: ProjectRepository,
        private val pointRepository: PointRepository,
        private val activeProject: ActiveProjectStore,
    ) : ViewModel() {
        private val _state = MutableStateFlow<LineSurveyState>(LineSurveyState.Idle("Линия 1"))
        val state: StateFlow<LineSurveyState> = _state.asStateFlow()

        private var collectingJob: Job? = null
        private val accumulated = mutableListOf<Vertex>()

        fun onNameChanged(value: String) {
            when (val s = _state.value) {
                is LineSurveyState.Idle -> _state.value = LineSurveyState.Idle(value)
                is LineSurveyState.BetweenVertices -> _state.value = s.copy(lineName = value)
                else -> Unit
            }
        }

        fun startNextVertex() {
            collectingJob?.cancel()
            val name = currentLineName()
            collectingJob = viewModelScope.launch {
                val prefs = preferences.defaults.first()
                val target = prefs.minEpochs
                val minFix = prefs.minFix
                val poleH = prefs.poleHeightM
                val tiltOn = prefs.tiltCorrectionEnabled
                val samples = mutableListOf<GnssStatus>()
                _state.value = LineSurveyState.Collecting(
                    lineName = name,
                    collectedVertexCount = accumulated.size,
                    collectedAtCurrentVertex = 0,
                    target = target,
                    currentFix = FixQuality.NoFix,
                )
                store.status
                    // One sample per GGA epoch (see PointSurveyViewModel); the gate
                    // rejects stale / sub-threshold-fix epochs from the vertex average.
                    .distinctUntilChangedBy { it.timestampUtc to it.isStale }
                    .takeWhile { samples.size < target }
                    .collect { status ->
                        if (status.isStale || status.latitude == null || !status.fix.isAtLeast(minFix)) {
                            updateCollecting(name, samples.size, target, status.fix)
                            return@collect
                        }
                        // Reduce the antenna fix to the ground mark (height always;
                        // lean too when tilt is on). Skip if tilt requested but IMU unusable.
                        val sample = TiltCorrector.reduceToGround(status, poleH, tiltOn)
                        if (sample == null) {
                            updateCollecting(name, samples.size, target, status.fix)
                            return@collect
                        }
                        samples += sample
                        updateCollecting(name, samples.size, target, status.fix)
                    }
                if (samples.isEmpty()) {
                    _state.value = LineSurveyState.Error("Нет ни одной эпохи с фиксом")
                    return@launch
                }
                accumulated += samplesToVertex(samples, poleH, tiltOn)
                _state.value = LineSurveyState.BetweenVertices(name, accumulated.toList())
            }
        }

        fun finish() {
            val name = currentLineName()
            if (accumulated.isEmpty()) {
                _state.value = LineSurveyState.Error("Нет ни одной вершины")
                return
            }
            _state.value = LineSurveyState.Saving(name, accumulated.size)
            viewModelScope.launch {
                runCatching {
                    val activeId = activeProject.activeId.firstOrNull()
                        ?: error("Активный проект не выбран — отметьте на вкладке «Проект»")
                    val project = projectRepository.observeById(activeId).firstOrNull()
                        ?: error("Активный проект не найден")
                    val targetCrs = CrsPresets.parse(project.crsConfig.presetId) ?: Crs.Wgs84Geo
                    accumulated.forEachIndexed { idx, vertex ->
                        val (rawN, rawE, rawH) = projectVertex(vertex, targetCrs)
                        val (n, e, h) = project.crsConfig.applyCalibration(targetCrs, rawN, rawE, rawH)
                        pointRepository.save(
                            NewPoint(
                                projectId = project.id,
                                name = "${name}_v$idx",
                                code = name,
                                layerId = null,
                                n = n,
                                e = e,
                                h = h,
                                source = PointSource.SURVEY,
                                externalRef = "line:$name",
                            ),
                            vertex.observation,
                        )
                    }
                }.onSuccess {
                    val saved = accumulated.size
                    accumulated.clear()
                    _state.value = LineSurveyState.Saved(name, saved)
                }.onFailure {
                    _state.value = LineSurveyState.Error(it.message ?: "Не удалось сохранить линию")
                }
            }
        }

        fun reset() {
            collectingJob?.cancel()
            accumulated.clear()
            _state.value = LineSurveyState.Idle("Линия 1")
        }

        fun cancelCurrent() {
            collectingJob?.cancel()
            _state.value = if (accumulated.isEmpty()) {
                LineSurveyState.Idle(currentLineName())
            } else {
                LineSurveyState.BetweenVertices(currentLineName(), accumulated.toList())
            }
        }

        fun removeLastVertex() {
            if (accumulated.isNotEmpty()) {
                accumulated.removeAt(accumulated.lastIndex)
                _state.value = LineSurveyState.BetweenVertices(currentLineName(), accumulated.toList())
            }
        }

        private fun currentLineName(): String = when (val s = _state.value) {
            is LineSurveyState.Idle -> s.lineName
            is LineSurveyState.Collecting -> s.lineName
            is LineSurveyState.BetweenVertices -> s.lineName
            is LineSurveyState.Saving -> s.lineName
            is LineSurveyState.Saved -> s.lineName
            is LineSurveyState.Error -> "Линия 1"
        }

        private fun updateCollecting(name: String, collected: Int, target: Int, fix: FixQuality) {
            _state.value = LineSurveyState.Collecting(
                lineName = name,
                collectedVertexCount = accumulated.size,
                collectedAtCurrentVertex = collected,
                target = target,
                currentFix = fix,
            )
        }

        private fun projectVertex(vertex: Vertex, crs: Crs): Triple<Double, Double, Double> = when (crs) {
            is Crs.Projected -> {
                // Vertex N/E/H come from averaged GeoPoint; reproject via CrsTransformer.
                val proj = CrsTransformer.project(
                    GeoPoint(latDeg = vertex.n, lonDeg = vertex.e, ellipsoidalHeightM = vertex.h),
                    Crs.Wgs84Geo,
                    crs,
                )
                Triple(proj.northingM, proj.eastingM, proj.heightM)
            }
            is Crs.Geographic -> Triple(vertex.n, vertex.e, vertex.h)
        }

        private fun samplesToVertex(samples: List<GnssStatus>, poleH: Double, tiltOn: Boolean): Vertex {
            val lats = samples.mapNotNull { it.latitude }
            val lons = samples.mapNotNull { it.longitude }
            val hs = samples.mapNotNull { it.ellipsoidalHeight }
            val meanLat = lats.average()
            val meanLon = lons.average()
            val meanH = if (hs.isNotEmpty()) hs.average() else 0.0
            val sigmaH = if (hs.size > 1) {
                val variance = hs.sumOf { (it - meanH) * (it - meanH) } / (hs.size - 1)
                sqrt(variance)
            } else {
                0.0
            }
            return Vertex(
                n = meanLat,
                e = meanLon,
                h = meanH,
                sigmaH = sigmaH,
                observation = ObservationFactory.fromSamples(samples, poleH, tiltOn),
            )
        }
    }
