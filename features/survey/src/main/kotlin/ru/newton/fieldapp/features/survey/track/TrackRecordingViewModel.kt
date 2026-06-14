package ru.newton.fieldapp.features.survey.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.crs.CrsPresets
import ru.newton.fieldapp.crs.CrsTransformer
import ru.newton.fieldapp.crs.GeoPoint
import ru.newton.fieldapp.data.preferences.ActiveProjectStore
import ru.newton.fieldapp.domain.model.TrackPointSample
import ru.newton.fieldapp.domain.model.TrackSession
import ru.newton.fieldapp.domain.repository.ProjectRepository
import ru.newton.fieldapp.domain.repository.TrackRepository
import ru.newton.fieldapp.features.survey.defaults.SurveyPreferences
import ru.newton.fieldapp.features.survey.defaults.TiltCorrector
import ru.newton.fieldapp.gnss.data.FixQuality
import ru.newton.fieldapp.gnss.data.GnssStatus
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import javax.inject.Inject

data class TrackRecordingState(
    val sessions: List<TrackSession> = emptyList(),
    val activeSessionId: Long? = null,
    val activePointCount: Int = 0,
    val currentFix: FixQuality = FixQuality.NoFix,
    val errorMessage: String? = null,
)

@HiltViewModel
class TrackRecordingViewModel
    @OptIn(ExperimentalCoroutinesApi::class)
    @Inject
    constructor(
        private val store: GnssStatusStore,
        private val preferences: SurveyPreferences,
        private val projectRepository: ProjectRepository,
        private val trackRepository: TrackRepository,
        private val activeProject: ActiveProjectStore,
    ) : ViewModel() {
        private val activeSessionId = MutableStateFlow<Long?>(null)
        private var samplerJob: Job? = null
        private var lastSampleAtMs: Long = 0L

        @OptIn(ExperimentalCoroutinesApi::class)
        private val sessionsFlow = activeProject.activeId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else trackRepository.observeSessions(id)
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private val pointCountFlow = activeSessionId.flatMapLatest { id ->
            if (id == null) flowOf(0) else trackRepository.observePointCount(id)
        }

        val state: StateFlow<TrackRecordingState> = combine(
            sessionsFlow,
            activeSessionId,
            pointCountFlow,
            store.status,
        ) { sessions, activeId, pointCount, status ->
            TrackRecordingState(
                sessions = sessions,
                activeSessionId = activeId,
                activePointCount = pointCount,
                currentFix = status.fix,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackRecordingState())

        fun start() {
            viewModelScope.launch {
                runCatching {
                    val activeId = activeProject.activeId.firstOrNull()
                        ?: error("Активный проект не выбран")
                    val project = projectRepository.observeById(activeId).firstOrNull()
                        ?: error("Проект не найден")
                    val name = "Трек ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}"
                    val sessionId = trackRepository.startSession(project.id, name)
                    val crs = CrsPresets.parse(project.crsConfig.presetId) ?: Crs.Wgs84Geo
                    val prefs = preferences.defaults.firstOrNull()
                    val minFix = prefs?.minFix ?: FixQuality.FixedRtk
                    val poleH = prefs?.poleHeightM ?: 0.0
                    val tiltOn = prefs?.tiltCorrectionEnabled ?: false
                    activeSessionId.value = sessionId
                    samplerJob?.cancel()
                    samplerJob = viewModelScope.launch { sample(sessionId, crs, minFix, poleH, tiltOn) }
                }.onFailure {
                    activeSessionId.value = null
                }
            }
        }

        fun stop() {
            val id = activeSessionId.value ?: return
            samplerJob?.cancel()
            samplerJob = null
            activeSessionId.value = null
            viewModelScope.launch { trackRepository.stopSession(id) }
        }

        fun delete(sessionId: Long) {
            viewModelScope.launch { trackRepository.deleteSession(sessionId) }
        }

        /**
         * Sampler — collects [GnssStatusStore.status] continuously, throttles to
         * one sample per second so a 5 Hz receiver doesn't bloat the DB. Skips
         * epochs without a fix.
         */
        private suspend fun sample(sessionId: Long, crs: Crs, minFix: FixQuality, poleH: Double, tiltOn: Boolean) {
            store.status.collect { status -> appendIfDue(sessionId, crs, status, minFix, poleH, tiltOn) }
        }

        private suspend fun appendIfDue(
            sessionId: Long,
            crs: Crs,
            status: GnssStatus,
            minFix: FixQuality,
            poleH: Double,
            tiltOn: Boolean,
        ) {
            // Skip stale (NMEA stalled) and sub-threshold epochs — a track must
            // not record metre-level autonomous points as if they were surveyed.
            if (status.isStale || !status.fix.isAtLeast(minFix)) return
            val now = System.currentTimeMillis()
            if (now - lastSampleAtMs < SAMPLE_INTERVAL_MS) return
            // Reduce to the ground mark (height always; lean too when tilt is on).
            val reduced = TiltCorrector.reduceToGround(status, poleH, tiltOn) ?: return
            val lat = reduced.latitude ?: return
            val lon = reduced.longitude ?: return
            lastSampleAtMs = now
            val height = reduced.ellipsoidalHeight ?: 0.0
            val (n, e, h) = when (crs) {
                is Crs.Projected -> {
                    val proj = CrsTransformer.project(GeoPoint(lat, lon, height), Crs.Wgs84Geo, crs)
                    Triple(proj.northingM, proj.eastingM, proj.heightM)
                }
                is Crs.Geographic -> Triple(lat, lon, height)
            }
            trackRepository.appendPoint(
                sessionId,
                TrackPointSample(
                    n = n,
                    e = e,
                    h = h,
                    fixQuality = status.fix.javaClass.simpleName,
                    timestampUtc = now,
                ),
            )
        }

        private companion object {
            const val SAMPLE_INTERVAL_MS = 1_000L
        }
    }
