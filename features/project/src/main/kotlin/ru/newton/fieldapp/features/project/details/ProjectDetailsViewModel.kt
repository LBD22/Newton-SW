package ru.newton.fieldapp.features.project.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.common.csv.CsvFormat
import ru.newton.fieldapp.core.logging.AppLog
import ru.newton.fieldapp.domain.model.HeightMode
import ru.newton.fieldapp.domain.repository.PointRepository
import ru.newton.fieldapp.domain.repository.ProjectRepository
import ru.newton.fieldapp.domain.usecase.ExportPointsToCsvUseCase
import ru.newton.fieldapp.domain.usecase.ImportPointsFromCsvUseCase
import javax.inject.Inject

@HiltViewModel
class ProjectDetailsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val projectRepository: ProjectRepository,
        pointRepository: PointRepository,
        private val importUseCase: ImportPointsFromCsvUseCase,
        private val exportUseCase: ExportPointsToCsvUseCase,
        private val log: AppLog,
    ) : ViewModel() {
        private val projectId: Long = checkNotNull(savedStateHandle["projectId"]) {
            "ProjectDetailsViewModel requires `projectId` nav argument"
        }

        val state: StateFlow<ProjectDetailsState> = combine(
            projectRepository.observeById(projectId),
            pointRepository.observePoints(projectId),
        ) { project, points ->
            if (project == null) {
                ProjectDetailsState.NotFound
            } else {
                ProjectDetailsState.Content(project, points)
            }
        }
            .catch { t ->
                log.ui("ProjectDetails load failed for id=$projectId", t)
                emit(ProjectDetailsState.Error(t.message ?: "Не удалось загрузить проект"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectDetailsState.Loading)

        private val _events = MutableSharedFlow<ProjectDetailsEvent>(extraBufferCapacity = 4)
        val events: SharedFlow<ProjectDetailsEvent> = _events.asSharedFlow()

        fun onImportCsv(content: String) {
            viewModelScope.launch {
                runCatching { importUseCase(projectId, content, CsvFormat.DEFAULT) }
                    .onSuccess(::emitImportFinished)
                    .onFailure(::emitImportFailed)
            }
        }

        fun onImportCsvWithMapping(
            content: String,
            mapping: List<ru.newton.fieldapp.core.common.csv.CsvColumn?>,
            delimiter: Char,
            decimalSeparator: Char,
        ) {
            viewModelScope.launch {
                runCatching {
                    importUseCase.importWithMapping(
                        projectId = projectId,
                        content = content,
                        mapping = mapping,
                        delimiter = delimiter,
                        decimalSeparator = decimalSeparator,
                    )
                }
                    .onSuccess(::emitImportFinished)
                    .onFailure(::emitImportFailed)
            }
        }

        private fun emitImportFinished(result: ImportPointsFromCsvUseCase.Result) {
            _events.tryEmit(
                ProjectDetailsEvent.ImportFinished(
                    saved = result.saved,
                    skipped = result.issues.size,
                ),
            )
        }

        private fun emitImportFailed(t: Throwable) {
            log.ui("Import CSV failed for project=$projectId", t)
            _events.tryEmit(ProjectDetailsEvent.ImportFailed(t.message ?: "Ошибка импорта"))
        }

        suspend fun prepareExport(): String = exportUseCase(projectId, CsvFormat.DEFAULT)

        /** Set the project's stored-height system. Applies to future saves only. */
        fun setHeightMode(mode: HeightMode) {
            viewModelScope.launch { projectRepository.setHeightMode(projectId, mode) }
        }
    }

sealed interface ProjectDetailsEvent {
    data class ImportFinished(val saved: Int, val skipped: Int) : ProjectDetailsEvent
    data class ImportFailed(val message: String) : ProjectDetailsEvent
}
