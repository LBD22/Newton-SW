package ru.newton.fieldapp.features.survey.stakeout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import ru.newton.fieldapp.crs.Crs
import ru.newton.fieldapp.crs.CrsPresets
import ru.newton.fieldapp.crs.CrsTransformer
import ru.newton.fieldapp.crs.GeoPoint
import ru.newton.fieldapp.data.preferences.ActiveProjectStore
import ru.newton.fieldapp.domain.model.Point
import ru.newton.fieldapp.domain.repository.PointRepository
import ru.newton.fieldapp.domain.repository.ProjectRepository
import ru.newton.fieldapp.gnss.data.GnssStatus
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import javax.inject.Inject

@HiltViewModel
class StakeoutToLineViewModel
    @OptIn(ExperimentalCoroutinesApi::class)
    @Inject
    constructor(
        private val store: GnssStatusStore,
        projectRepository: ProjectRepository,
        pointRepository: PointRepository,
        activeProject: ActiveProjectStore,
    ) : ViewModel() {
        private val pointA = MutableStateFlow<Point?>(null)
        private val pointB = MutableStateFlow<Point?>(null)

        @OptIn(ExperimentalCoroutinesApi::class)
        private val activePresetId: StateFlow<String?> = activeProject.activeId
            .flatMapLatest { id ->
                if (id == null) {
                    flowOf(null)
                } else {
                    projectRepository.observeById(id)
                }
            }
            .let { flow ->
                @Suppress("UNCHECKED_CAST")
                (flow as kotlinx.coroutines.flow.Flow<Any?>)
            }
            .let { mixed ->
                kotlinx.coroutines.flow.flow {
                    mixed.collect { v ->
                        emit((v as? ru.newton.fieldapp.domain.model.Project)?.crsConfig?.presetId)
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        @OptIn(ExperimentalCoroutinesApi::class)
        private val availablePoints: StateFlow<List<Point>> = activeProject.activeId
            .flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else pointRepository.observePoints(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val state: StateFlow<StakeoutToLineState> = combine(
            store.status,
            availablePoints,
            pointA,
            pointB,
            activePresetId,
        ) { values: Array<Any?> ->
            val status = values[0] as GnssStatus

            @Suppress("UNCHECKED_CAST")
            val points = values[1] as List<Point>
            val a = values[2] as Point?
            val b = values[3] as Point?
            val presetId = values[4] as String?

            val vector = if (a != null && b != null && presetId != null && status.latitude != null && status.longitude != null) {
                computeVector(status, a, b, presetId)
            } else {
                null
            }

            StakeoutToLineState(
                availablePoints = points,
                pointA = a,
                pointB = b,
                vector = vector,
                fix = status.fix,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StakeoutToLineState())

        fun selectA(id: Long) {
            pointA.value = availablePoints.value.firstOrNull { it.id == id }
        }

        fun selectB(id: Long) {
            pointB.value = availablePoints.value.firstOrNull { it.id == id }
        }

        fun clear() {
            pointA.value = null
            pointB.value = null
        }

        private fun computeVector(status: GnssStatus, a: Point, b: Point, presetId: String): LineStakeoutVector {
            val lat = status.latitude!!
            val lon = status.longitude!!
            val height = status.ellipsoidalHeight ?: 0.0
            val crs = CrsPresets.parse(presetId) ?: Crs.Wgs84Geo
            val (currentN, currentE, currentH) = when (crs) {
                is Crs.Projected -> {
                    val p = CrsTransformer.project(GeoPoint(lat, lon, height), Crs.Wgs84Geo, crs)
                    Triple(p.northingM, p.eastingM, p.heightM)
                }
                is Crs.Geographic -> Triple(lat, lon, height)
            }
            return LineStakeoutVector.between(
                currentN = currentN,
                currentE = currentE,
                currentH = currentH,
                aN = a.n,
                aE = a.e,
                aH = a.h,
                bN = b.n,
                bE = b.e,
                bH = b.h,
            )
        }
    }
