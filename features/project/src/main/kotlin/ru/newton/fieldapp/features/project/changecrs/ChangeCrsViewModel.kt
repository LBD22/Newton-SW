package ru.newton.fieldapp.features.project.changecrs

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
import ru.newton.fieldapp.crs.ProjectedPoint
import ru.newton.fieldapp.domain.model.Point
import ru.newton.fieldapp.domain.model.Project
import ru.newton.fieldapp.domain.repository.PointRepository
import ru.newton.fieldapp.domain.repository.ProjectRepository
import javax.inject.Inject

/**
 * PRJ-004 — change a project's CRS with a recalc preview.
 *
 * Flow: user picks a target preset, we show before/after for the first few
 * points, user confirms, we re-project every point in the project and flip
 * the project's CRS metadata. Math runs entirely on the device — `:crs` is
 * pure Kotlin so this is safe to call from a ViewModel.
 *
 * Preview only re-projects up to [PREVIEW_LIMIT] points to keep the UI
 * snappy when the project has thousands. The full pass on Apply is
 * synchronous-per-point but executed inside `viewModelScope` so the screen
 * shows a "Применение…" state while it runs.
 */
@HiltViewModel
class ChangeCrsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val projectRepository: ProjectRepository,
        private val pointRepository: PointRepository,
    ) : ViewModel() {
        private val projectId: Long = checkNotNull(savedStateHandle["projectId"]) {
            "ChangeCrsViewModel requires `projectId` nav argument"
        }

        private val targetPresetId = MutableStateFlow<String?>(null)
        private val phase = MutableStateFlow<Phase>(Phase.Editing)

        val state: StateFlow<ChangeCrsState> = combine(
            projectRepository.observeById(projectId),
            pointRepository.observePoints(projectId),
            targetPresetId,
            phase,
        ) { project, points, targetId, currentPhase ->
            buildState(project, points, targetId, currentPhase)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChangeCrsState.Loading)

        fun onPickPreset(presetId: String) {
            targetPresetId.value = presetId
        }

        fun apply() {
            val target = targetPresetId.value ?: return
            viewModelScope.launch {
                phase.value = Phase.Applying
                runCatching {
                    val project = projectRepository.observeById(projectId).firstOrNull()
                        ?: error("Проект не найден")
                    val sourceCrs = CrsPresets.parse(project.crsConfig.presetId)
                        ?: error("Текущая СК «${project.crsConfig.presetId}» не поддерживается")
                    val targetCrs = CrsPresets.parse(target)
                        ?: error("Целевая СК «$target» не поддерживается")
                    if (sourceCrs.presetId == targetCrs.presetId) return@runCatching
                    val points = pointRepository.observePoints(projectId).firstOrNull().orEmpty()
                    points.forEach { point ->
                        val (n, e, h) = reproject(point, sourceCrs, targetCrs)
                        pointRepository.updateCoordinates(point.id, n, e, h)
                    }
                    projectRepository.setCrs(projectId, targetCrs.presetId)
                }.onSuccess {
                    phase.value = Phase.Done
                }.onFailure {
                    phase.value = Phase.Failed(it.message ?: "Ошибка пересчёта")
                }
            }
        }

        fun acknowledgeError() {
            phase.value = Phase.Editing
        }

        private fun buildState(
            project: Project?,
            points: List<Point>,
            targetId: String?,
            currentPhase: Phase,
        ): ChangeCrsState {
            project ?: return ChangeCrsState.NotFound
            val sourceCrs = CrsPresets.parse(project.crsConfig.presetId) ?: return ChangeCrsState.NotFound
            val targetCrs = targetId?.let(CrsPresets::parse)
            val previewRows = if (targetCrs != null && targetCrs.presetId != sourceCrs.presetId) {
                points.take(PREVIEW_LIMIT).map { point ->
                    val (n, e, h) = reproject(point, sourceCrs, targetCrs)
                    PreviewRow(name = point.name, oldN = point.n, oldE = point.e, oldH = point.h, newN = n, newE = e, newH = h)
                }
            } else {
                emptyList()
            }
            return ChangeCrsState.Editing(
                projectName = project.name,
                currentPresetId = sourceCrs.presetId,
                targetPresetId = targetCrs?.presetId,
                totalPoints = points.size,
                preview = previewRows,
                phase = currentPhase,
            )
        }

        /**
         * Re-project a single [Point] from [source] to [target]. Centred on
         * `point.n / point.e` interpreted in the source CRS's natural units
         * (metres for projected, degrees for geographic).
         */
        private fun reproject(point: Point, source: Crs, target: Crs): Triple<Double, Double, Double> {
            val sourceGeo = when (source) {
                is Crs.Projected -> CrsTransformer.unproject(
                    ProjectedPoint(northingM = point.n, eastingM = point.e, heightM = point.h),
                    source,
                )
                is Crs.Geographic -> GeoPoint(latDeg = point.n, lonDeg = point.e, ellipsoidalHeightM = point.h)
            }
            return when (target) {
                is Crs.Projected -> {
                    val proj = when (source) {
                        is Crs.Projected -> CrsTransformer.reproject(
                            ProjectedPoint(northingM = point.n, eastingM = point.e, heightM = point.h),
                            source,
                            target,
                        )
                        is Crs.Geographic -> CrsTransformer.project(sourceGeo, source, target)
                    }
                    Triple(proj.northingM, proj.eastingM, proj.heightM)
                }
                is Crs.Geographic -> {
                    val onTarget = CrsTransformer.transformGeo(sourceGeo, source, target)
                    Triple(onTarget.latDeg, onTarget.lonDeg, onTarget.ellipsoidalHeightM)
                }
            }
        }

        sealed interface Phase {
            data object Editing : Phase
            data object Applying : Phase
            data object Done : Phase
            data class Failed(val message: String) : Phase
        }

        private companion object {
            const val PREVIEW_LIMIT = 5
        }
    }

sealed interface ChangeCrsState {
    data object Loading : ChangeCrsState
    data object NotFound : ChangeCrsState
    data class Editing(
        val projectName: String,
        val currentPresetId: String,
        val targetPresetId: String?,
        val totalPoints: Int,
        val preview: List<PreviewRow>,
        val phase: ChangeCrsViewModel.Phase,
    ) : ChangeCrsState
}

data class PreviewRow(
    val name: String,
    val oldN: Double,
    val oldE: Double,
    val oldH: Double,
    val newN: Double,
    val newE: Double,
    val newH: Double,
)
