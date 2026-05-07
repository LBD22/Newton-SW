package ru.newton.fieldapp.features.project.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.logging.AppLog
import ru.newton.fieldapp.data.backup.ProjectBackupService
import ru.newton.fieldapp.data.preferences.ActiveProjectStore
import ru.newton.fieldapp.domain.repository.ProjectRepository
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@HiltViewModel
class ProjectListViewModel
    @Inject
    constructor(
        repository: ProjectRepository,
        private val activeProject: ActiveProjectStore,
        private val backupService: ProjectBackupService,
        private val log: AppLog,
    ) : ViewModel() {
        val state: StateFlow<ProjectListState> = combine(
            repository.observeAll(),
            activeProject.activeId,
        ) { projects, activeId ->
            if (projects.isEmpty()) {
                ProjectListState.Empty
            } else {
                ProjectListState.Content(projects = projects, activeProjectId = activeId)
            }
        }
            .catch { t ->
                log.ui("ProjectList load failed", t)
                emit(ProjectListState.Error(t.message ?: "Не удалось загрузить проекты"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectListState.Loading)

        fun makeActive(id: Long) {
            viewModelScope.launch { activeProject.setActive(id) }
        }

        suspend fun exportBackup(projectId: Long, output: OutputStream): ProjectBackupService.ExportResult =
            backupService.export(projectId, output)

        suspend fun importBackup(input: InputStream): ProjectBackupService.ImportResult =
            backupService.import(input)
    }
