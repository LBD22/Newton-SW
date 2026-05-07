package ru.newton.fieldapp.features.project.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.logging.AppLog
import ru.newton.fieldapp.data.preferences.ActiveProjectStore
import ru.newton.fieldapp.domain.repository.ProjectRepository
import javax.inject.Inject

@HiltViewModel
class CreateProjectViewModel
    @Inject
    constructor(
        private val repository: ProjectRepository,
        private val activeProject: ActiveProjectStore,
        private val log: AppLog,
    ) : ViewModel() {
        private val _state = MutableStateFlow<CreateProjectState>(CreateProjectState.Editing())
        val state: StateFlow<CreateProjectState> = _state.asStateFlow()

        fun onNameChanged(value: String) {
            val current = _state.value as? CreateProjectState.Editing ?: return
            _state.value = current.copy(name = value, nameError = null)
        }

        fun onCommentChanged(value: String) {
            val current = _state.value as? CreateProjectState.Editing ?: return
            _state.value = current.copy(comment = value)
        }

        fun onCrsPickerOpen() {
            val current = _state.value as? CreateProjectState.Editing ?: return
            _state.value = current.copy(showCrsPicker = true)
        }

        fun onCrsPickerDismiss() {
            val current = _state.value as? CreateProjectState.Editing ?: return
            _state.value = current.copy(showCrsPicker = false)
        }

        fun onCrsSelected(presetId: String) {
            val current = _state.value as? CreateProjectState.Editing ?: return
            _state.value = current.copy(crsPresetId = presetId, showCrsPicker = false)
        }

        fun onSaveClicked() {
            val current = _state.value
            val trimmedName = current.name.trim()
            if (trimmedName.isEmpty()) {
                _state.value = CreateProjectState.Editing(
                    name = current.name,
                    comment = current.comment,
                    crsPresetId = current.crsPresetId,
                    nameError = "Имя проекта обязательно",
                )
                return
            }

            _state.value = CreateProjectState.Saving(
                name = trimmedName,
                comment = current.comment,
                crsPresetId = current.crsPresetId,
            )
            viewModelScope.launch {
                try {
                    val project = repository.create(
                        name = trimmedName,
                        comment = current.comment.trim().ifEmpty { null },
                        crsPresetId = current.crsPresetId,
                    )
                    // Auto-activate the newly-created project — surveyors create
                    // a project to immediately work in it; making them tap
                    // "Сделать активным" right after Save is friction.
                    activeProject.setActive(project.id)
                    _state.value = CreateProjectState.Saved(
                        name = project.name,
                        comment = project.comment.orEmpty(),
                        crsPresetId = project.crsConfig.presetId,
                        projectId = project.id,
                    )
                } catch (t: Throwable) {
                    log.ui("Project create failed", t)
                    _state.value = CreateProjectState.Failed(
                        name = trimmedName,
                        comment = current.comment,
                        crsPresetId = current.crsPresetId,
                        message = t.message ?: "Не удалось сохранить проект",
                    )
                }
            }
        }

        fun onAcknowledgeFailure() {
            val current = _state.value
            _state.value = CreateProjectState.Editing(
                name = current.name,
                comment = current.comment,
                crsPresetId = current.crsPresetId,
            )
        }
    }
