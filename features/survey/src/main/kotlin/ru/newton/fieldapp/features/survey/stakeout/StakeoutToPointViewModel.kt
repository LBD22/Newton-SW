package ru.newton.fieldapp.features.survey.stakeout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.crs.CrsPresets
import ru.newton.fieldapp.crs.CrsTransformer
import ru.newton.fieldapp.crs.GeoPoint
import ru.newton.fieldapp.data.preferences.ActiveProjectStore
import ru.newton.fieldapp.domain.model.NewPoint
import ru.newton.fieldapp.domain.model.Point
import ru.newton.fieldapp.domain.model.PointSource
import ru.newton.fieldapp.domain.model.StakeoutMode
import ru.newton.fieldapp.domain.repository.PointRepository
import ru.newton.fieldapp.domain.repository.ProjectRepository
import ru.newton.fieldapp.domain.repository.StakeoutResultRepository
import ru.newton.fieldapp.features.survey.defaults.SurveyPreferences
import ru.newton.fieldapp.gnss.data.GnssStatus
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import javax.inject.Inject

@HiltViewModel
class StakeoutToPointViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val store: GnssStatusStore,
        projectRepository: ProjectRepository,
        private val pointRepository: PointRepository,
        private val stakeoutHistory: StakeoutResultRepository,
        activeProject: ActiveProjectStore,
        preferences: SurveyPreferences,
    ) : ViewModel() {
        private val targetId: Long = checkNotNull(savedStateHandle["targetId"]) {
            "StakeoutToPointViewModel requires `targetId` nav arg"
        }

        private val target = MutableStateFlow<Point?>(null)
        private val crsPresetId = MutableStateFlow<String?>(null)

        val state: StateFlow<StakeoutState> = combine(
            store.status,
            target,
            crsPresetId,
            preferences.defaults,
        ) { status, t, presetId, defaults ->
            when {
                t == null || presetId == null -> StakeoutState.Loading
                else -> resolveStakeout(status, t, presetId, defaults.toleranceHorizontalM)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StakeoutState.Loading)

        init {
            viewModelScope.launch {
                val activeId = activeProject.activeId.firstOrNull() ?: return@launch
                val project = projectRepository.observeById(activeId).firstOrNull() ?: return@launch
                crsPresetId.value = project.crsConfig.presetId
                target.value = pointRepository.observePoints(project.id).firstOrNull()
                    ?.firstOrNull { it.id == targetId }
            }
        }

        fun saveAsBuilt() {
            val targetPoint = target.value ?: return
            val presetId = crsPresetId.value ?: "WGS84_GEO"
            viewModelScope.launch {
                runCatching {
                    val status = store.status.value
                    val crs = CrsPresets.parse(presetId) ?: Crs.Wgs84Geo
                    val (n, e, h) = currentNeh(status, crs)
                    pointRepository.save(
                        NewPoint(
                            projectId = targetPoint.projectId,
                            name = "${targetPoint.name}_AB",
                            code = "AB",
                            layerId = null,
                            n = n,
                            e = e,
                            h = h,
                            source = PointSource.SURVEY,
                            externalRef = "stakeout-of-${targetPoint.id}",
                        ),
                    )
                    val deltaH = kotlin.math.hypot(n - targetPoint.n, e - targetPoint.e)
                    val deltaV = h - targetPoint.h
                    stakeoutHistory.record(
                        projectId = targetPoint.projectId,
                        targetPointId = targetPoint.id,
                        targetLabel = targetPoint.name,
                        mode = StakeoutMode.POINT,
                        targetN = targetPoint.n,
                        targetE = targetPoint.e,
                        targetH = targetPoint.h,
                        actualN = n,
                        actualE = e,
                        actualH = h,
                        deltaHorizontalM = deltaH,
                        deltaVerticalM = deltaV,
                    )
                }
            }
        }

        private fun resolveStakeout(
            status: GnssStatus,
            target: Point,
            presetId: String,
            toleranceM: Double,
        ): StakeoutState {
            val lat = status.latitude ?: return StakeoutState.WaitingForFix
            val lon = status.longitude ?: return StakeoutState.WaitingForFix
            val height = status.ellipsoidalHeight ?: 0.0
            val crs = CrsPresets.parse(presetId) ?: Crs.Wgs84Geo
            val (currentN, currentE, currentH) = currentNehFromGeo(lat, lon, height, crs)
            val vector = StakeoutVector.between(
                currentN = currentN,
                currentE = currentE,
                currentH = currentH,
                targetN = target.n,
                targetE = target.e,
                targetH = target.h,
            )
            return StakeoutState.Active(
                targetName = target.name,
                vector = vector,
                fix = status.fix,
                toleranceM = toleranceM,
            )
        }

        private fun currentNeh(status: GnssStatus, crs: Crs): Triple<Double, Double, Double> {
            val lat = status.latitude ?: 0.0
            val lon = status.longitude ?: 0.0
            val h = status.ellipsoidalHeight ?: 0.0
            return currentNehFromGeo(lat, lon, h, crs)
        }

        private fun currentNehFromGeo(
            lat: Double,
            lon: Double,
            h: Double,
            crs: Crs,
        ): Triple<Double, Double, Double> = when (crs) {
            is Crs.Projected -> {
                val p = CrsTransformer.project(GeoPoint(lat, lon, h), Crs.Wgs84Geo, crs)
                Triple(p.northingM, p.eastingM, p.heightM)
            }
            is Crs.Geographic -> Triple(lat, lon, h)
        }
    }
