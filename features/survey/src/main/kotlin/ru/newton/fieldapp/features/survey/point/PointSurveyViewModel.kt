package ru.newton.fieldapp.features.survey.point

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import ru.newton.fieldapp.features.survey.defaults.SurveyPreferences
import ru.newton.fieldapp.features.survey.defaults.TiltCorrector
import ru.newton.fieldapp.gnss.data.FixQuality
import ru.newton.fieldapp.gnss.data.GnssStatus
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class PointSurveyViewModel
    @Inject
    constructor(
        private val store: GnssStatusStore,
        private val preferences: SurveyPreferences,
        private val projectRepository: ProjectRepository,
        private val pointRepository: PointRepository,
        private val activeProject: ActiveProjectStore,
    ) : ViewModel() {
        private val _state = MutableStateFlow<PointSurveyState>(PointSurveyState.Idle)
        val state: StateFlow<PointSurveyState> = _state.asStateFlow()

        private var collectingJob: Job? = null

        fun start() {
            collectingJob?.cancel()
            collectingJob = viewModelScope.launch {
                val prefsSnapshot = preferences.defaults.first()
                val target = prefsSnapshot.minEpochs
                val tiltOn = prefsSnapshot.tiltCorrectionEnabled
                val poleH = prefsSnapshot.poleHeightM
                val samples = mutableListOf<GnssStatus>()
                _state.value = PointSurveyState.Collecting(
                    collected = 0,
                    target = target,
                    currentFix = FixQuality.NoFix,
                    lastEpochAtUtc = 0L,
                )
                store.status
                    .takeWhile { samples.size < target }
                    .collect { status ->
                        // Skip epochs without a fix. We still update the UI's
                        // currentFix indicator so the surveyor sees why nothing
                        // is happening when they're under canopy.
                        if (status.fix == FixQuality.NoFix || status.latitude == null) {
                            _state.value = PointSurveyState.Collecting(
                                collected = samples.size,
                                target = target,
                                currentFix = status.fix,
                                lastEpochAtUtc = status.timestampUtc,
                            )
                            return@collect
                        }
                        // When tilt-correction is on, reduce each epoch to the
                        // pole tip; the corrector is a no-op on epochs whose
                        // IMU isn't valid, so a brief fall-back to vertical-pole
                        // numbers only happens when the receiver actually
                        // says so.
                        val sample = if (tiltOn) TiltCorrector.apply(status, poleH) else status
                        samples += sample
                        _state.value = PointSurveyState.Collecting(
                            collected = samples.size,
                            target = target,
                            currentFix = status.fix,
                            lastEpochAtUtc = status.timestampUtc,
                        )
                    }

                if (samples.isEmpty()) {
                    _state.value = PointSurveyState.Error("Нет ни одной эпохи с фиксом")
                    return@launch
                }
                // Auto-name lookup needs the active project; if nothing is
                // selected yet we fall back to an empty name and let save()
                // surface the "no active project" error.
                val activeId = activeProject.activeId.firstOrNull()
                val autoName = if (activeId != null) {
                    pointRepository.nextAutoName(
                        projectId = activeId,
                        prefix = prefsSnapshot.namePrefix,
                        padding = prefsSnapshot.namePadding,
                    )
                } else {
                    ""
                }
                _state.value = samples.toReadyState(
                    autoName = autoName,
                    codeLibrary = prefsSnapshot.codeLibrary,
                )
            }
        }

        fun cancel() {
            collectingJob?.cancel()
            _state.value = PointSurveyState.Idle
        }

        fun onNameChanged(value: String) = mutateReady { copy(name = value) }
        fun onCodeChanged(value: String) = mutateReady { copy(code = value) }

        fun save() {
            val ready = _state.value as? PointSurveyState.Ready ?: return
            if (ready.name.trim().isEmpty()) {
                _state.value = PointSurveyState.Error("Имя точки обязательно")
                return
            }
            _state.value = PointSurveyState.Saving
            viewModelScope.launch {
                runCatching {
                    val activeId = activeProject.activeId.firstOrNull()
                        ?: error("Активный проект не выбран — отметьте на вкладке «Проект»")
                    val project = projectRepository.observeById(activeId).firstOrNull()
                        ?: error("Активный проект не найден (id=$activeId)")
                    val targetCrs = CrsPresets.parse(project.crsConfig.presetId) ?: Crs.Wgs84Geo
                    val geo = GeoPoint(
                        latDeg = ready.averageLat,
                        lonDeg = ready.averageLon,
                        ellipsoidalHeightM = ready.averageH,
                    )
                    // For projected CRSs we forward-project; for geographic the values
                    // are stored as N=lat, E=lon to match the ProjectedPoint shape.
                    val (n, e, h) = when (targetCrs) {
                        is Crs.Projected -> {
                            val proj = CrsTransformer.project(geo, Crs.Wgs84Geo, targetCrs)
                            Triple(proj.northingM, proj.eastingM, proj.heightM)
                        }
                        is Crs.Geographic -> Triple(geo.latDeg, geo.lonDeg, geo.ellipsoidalHeightM)
                    }
                    pointRepository.save(
                        NewPoint(
                            projectId = project.id,
                            name = ready.name.trim(),
                            code = ready.code.trim().ifEmpty { null },
                            layerId = null,
                            n = n,
                            e = e,
                            h = h,
                            source = PointSource.SURVEY,
                        ),
                    )
                }.onSuccess { id -> _state.value = PointSurveyState.Saved(id) }
                    .onFailure { _state.value = PointSurveyState.Error(it.message ?: "Не удалось сохранить точку") }
            }
        }

        fun reset() {
            _state.value = PointSurveyState.Idle
        }

        private fun mutateReady(transform: PointSurveyState.Ready.() -> PointSurveyState.Ready) {
            val current = _state.value as? PointSurveyState.Ready ?: return
            _state.value = current.transform()
        }

        /**
         * Average lat/lon/h. σH is the standard deviation of altitude,
         * a quick proxy for "how stable was this fix?". Real σ-N/σ-E from
         * GST is averaged at the receiver and used for the saved
         * Observation; here we just expose the spread of altitude samples.
         */
        private fun List<GnssStatus>.toReadyState(
            autoName: String,
            codeLibrary: List<String>,
        ): PointSurveyState.Ready {
            val lats = mapNotNull { it.latitude }
            val lons = mapNotNull { it.longitude }
            val hs = mapNotNull { it.ellipsoidalHeight }
            val meanLat = lats.average()
            val meanLon = lons.average()
            val meanH = if (hs.isNotEmpty()) hs.average() else 0.0
            val sigmaH = if (hs.size > 1) {
                val variance = hs.sumOf { (it - meanH) * (it - meanH) } / (hs.size - 1)
                sqrt(variance)
            } else {
                0.0
            }
            return PointSurveyState.Ready(
                averageLat = meanLat,
                averageLon = meanLon,
                averageH = meanH,
                sigmaH = sigmaH,
                sampleCount = size,
                name = autoName,
                codeLibrary = codeLibrary,
            )
        }
    }
