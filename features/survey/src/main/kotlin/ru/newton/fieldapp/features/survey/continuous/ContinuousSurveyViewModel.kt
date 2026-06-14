package ru.newton.fieldapp.features.survey.continuous

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
import ru.newton.fieldapp.gnss.data.FixQuality
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class ContinuousMode { DISTANCE, TIME }

data class ContinuousSurveyState(
    val running: Boolean = false,
    val mode: ContinuousMode = ContinuousMode.DISTANCE,
    val distanceThresholdM: Double = 1.0,
    val timeThresholdSec: Int = 5,
    val code: String = "",
    val savedCount: Int = 0,
    val lastSavedName: String? = null,
    val distanceSinceLastSaveM: Double = 0.0,
    val secondsSinceLastSave: Int = 0,
    val currentFix: FixQuality = FixQuality.NoFix,
    val errorMessage: String? = null,
)

/**
 * Continuous survey (LandStar parity). Subscribes to [GnssStatusStore] and
 * auto-saves a point whenever the trigger fires:
 *  - DISTANCE mode: cumulative haversine distance from the last saved
 *    position exceeds `distanceThresholdM`.
 *  - TIME mode: at least `timeThresholdSec` seconds have elapsed since the
 *    previous save (regardless of motion).
 *
 * Names are auto-generated via [PointRepository.nextAutoName] using the
 * surveyor's configured prefix/padding. Code is taken from the in-memory
 * [ContinuousSurveyState.code] — surveyors typically lock one code per pass
 * (e.g. all "kerb" along a stretch of road).
 */
@HiltViewModel
class ContinuousSurveyViewModel
    @Inject
    constructor(
        private val store: GnssStatusStore,
        private val preferences: SurveyPreferences,
        private val activeProject: ActiveProjectStore,
        private val projectRepository: ProjectRepository,
        private val pointRepository: PointRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ContinuousSurveyState())
        val state: StateFlow<ContinuousSurveyState> = _state.asStateFlow()

        val codeLibrary: StateFlow<List<String>> = preferences.defaults
            .map { it.codeLibrary }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        private var loopJob: Job? = null

        fun setMode(mode: ContinuousMode) { _state.update { it.copy(mode = mode) } }
        fun setDistanceThreshold(v: Double) {
            _state.update { it.copy(distanceThresholdM = v.coerceAtLeast(0.1)) }
        }
        fun setTimeThreshold(v: Int) {
            _state.update { it.copy(timeThresholdSec = v.coerceAtLeast(1)) }
        }
        fun setCode(v: String) { _state.update { it.copy(code = v) } }

        fun start() {
            if (_state.value.running) return
            loopJob?.cancel()
            _state.update {
                it.copy(
                    running = true,
                    savedCount = 0,
                    lastSavedName = null,
                    distanceSinceLastSaveM = 0.0,
                    secondsSinceLastSave = 0,
                    errorMessage = null,
                )
            }
            loopJob = viewModelScope.launch { runLoop() }
        }

        fun stop() {
            loopJob?.cancel()
            loopJob = null
            _state.update { it.copy(running = false) }
        }

        private suspend fun runLoop() {
            val prefs = preferences.defaults.first()
            val activeId = activeProject.activeId.firstOrNull()
            if (activeId == null) {
                _state.update { it.copy(running = false, errorMessage = "Активный проект не выбран") }
                return
            }
            val project = projectRepository.observeById(activeId).firstOrNull()
            if (project == null) {
                _state.update { it.copy(running = false, errorMessage = "Активный проект не найден") }
                return
            }
            val targetCrs = CrsPresets.parse(project.crsConfig.presetId) ?: Crs.Wgs84Geo
            val minFix = prefs.minFix

            // "Last saved" anchors for trigger checks. Updated on each save.
            var lastLat: Double? = null
            var lastLon: Double? = null
            var lastSaveAtMs: Long = 0L
            // De-dup per GGA epoch: the store emits once per NMEA sentence, so
            // GST/GSA/GSV would otherwise re-evaluate the trigger on the same
            // position several times a second.
            var lastEpochTs = -1L

            store.status.collect { status ->
                val current = _state.value
                if (!current.running) return@collect
                _state.update { it.copy(currentFix = status.fix) }
                if (status.isStale || !status.fix.isAtLeast(minFix)) return@collect
                if (status.timestampUtc == lastEpochTs) return@collect
                lastEpochTs = status.timestampUtc
                val rawLat = status.latitude ?: return@collect
                val rawLon = status.longitude ?: return@collect

                // Reduce to the ground mark (height always; lean too when tilt
                // is on). Skip the epoch if tilt is requested but the IMU is
                // unusable — same discipline as the point-survey path.
                val sample = TiltCorrector.reduceToGround(status, prefs.poleHeightM, prefs.tiltCorrectionEnabled)
                    ?: return@collect
                val sampleLat = sample.latitude ?: rawLat
                val sampleLon = sample.longitude ?: rawLon
                val sampleH = sample.ellipsoidalHeight ?: 0.0
                val now = System.currentTimeMillis()

                // Distance / time deltas from the previous save.
                val distance = if (lastLat != null && lastLon != null) {
                    haversineMetres(lastLat!!, lastLon!!, sampleLat, sampleLon)
                } else {
                    0.0
                }
                val elapsedSec = if (lastSaveAtMs == 0L) 0 else ((now - lastSaveAtMs) / 1000L).toInt()

                val shouldSave = when (current.mode) {
                    ContinuousMode.DISTANCE ->
                        lastLat == null || distance >= current.distanceThresholdM
                    ContinuousMode.TIME ->
                        lastSaveAtMs == 0L || elapsedSec >= current.timeThresholdSec
                }

                if (shouldSave) {
                    val name = pointRepository.nextAutoName(
                        projectId = activeId,
                        prefix = prefs.namePrefix,
                        padding = prefs.namePadding,
                    )
                    val (n, e, h) = projectCoords(targetCrs, sampleLat, sampleLon, sampleH)
                    val observation = ObservationFactory.fromSamples(
                        listOf(sample),
                        prefs.poleHeightM,
                        prefs.tiltCorrectionEnabled,
                    )
                    val saveResult = runCatching {
                        pointRepository.save(
                            NewPoint(
                                projectId = activeId,
                                name = name,
                                code = current.code.trim().ifEmpty { null },
                                layerId = null,
                                n = n,
                                e = e,
                                h = h,
                                source = PointSource.SURVEY,
                                externalRef = "continuous",
                            ),
                            observation,
                        )
                    }
                    val saveError = saveResult.exceptionOrNull()
                    if (saveError != null) {
                        _state.update { s -> s.copy(errorMessage = saveError.message ?: "Не удалось сохранить точку") }
                        return@collect
                    }
                    lastLat = sampleLat
                    lastLon = sampleLon
                    lastSaveAtMs = now
                    _state.update {
                        it.copy(
                            savedCount = it.savedCount + 1,
                            lastSavedName = name,
                            distanceSinceLastSaveM = 0.0,
                            secondsSinceLastSave = 0,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            distanceSinceLastSaveM = distance,
                            secondsSinceLastSave = elapsedSec,
                        )
                    }
                }
            }
        }

        private fun projectCoords(
            crs: Crs,
            lat: Double,
            lon: Double,
            h: Double,
        ): Triple<Double, Double, Double> = when (crs) {
            is Crs.Projected -> {
                val proj = CrsTransformer.project(GeoPoint(lat, lon, h), Crs.Wgs84Geo, crs)
                Triple(proj.northingM, proj.eastingM, proj.heightM)
            }
            is Crs.Geographic -> Triple(lat, lon, h)
        }

        /** Spherical haversine — accurate to ~1 mm at any practical pace. */
        private fun haversineMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6_371_008.8 // mean Earth radius
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).let { it * it }
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

        private fun MutableStateFlow<ContinuousSurveyState>.update(
            block: (ContinuousSurveyState) -> ContinuousSurveyState,
        ) {
            value = block(value)
        }
    }
