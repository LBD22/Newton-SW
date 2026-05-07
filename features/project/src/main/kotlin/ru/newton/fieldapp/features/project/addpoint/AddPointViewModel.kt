package ru.newton.fieldapp.features.project.addpoint

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.logging.AppLog
import ru.newton.fieldapp.domain.model.NewPoint
import ru.newton.fieldapp.domain.model.PointSource
import ru.newton.fieldapp.domain.repository.PointRepository
import javax.inject.Inject

@HiltViewModel
class AddPointViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: PointRepository,
        private val log: AppLog,
    ) : ViewModel() {
        private val projectId: Long = checkNotNull(savedStateHandle["projectId"]) {
            "AddPointViewModel requires `projectId` nav argument"
        }

        private val _state = MutableStateFlow<AddPointState>(AddPointState.Editing())
        val state: StateFlow<AddPointState> = _state.asStateFlow()

        fun onNameChanged(value: String) = updateField { copy(name = value, errors = errors.copy(name = null)) }
        fun onCodeChanged(value: String) = updateField { copy(code = value) }
        fun onNorthingChanged(value: String) = updateField { copy(northing = value, errors = errors.copy(northing = null)) }
        fun onEastingChanged(value: String) = updateField { copy(easting = value, errors = errors.copy(easting = null)) }
        fun onHeightChanged(value: String) = updateField { copy(height = value, errors = errors.copy(height = null)) }

        fun onSaveClicked() {
            val current = _state.value as? AddPointState.Editing ?: return
            val name = current.name.trim()
            val n = parseDecimal(current.northing)
            val e = parseDecimal(current.easting)
            val h = parseDecimal(current.height)

            val errors = AddPointState.FieldErrors(
                name = if (name.isEmpty()) "Имя точки обязательно" else null,
                northing = if (n == null) "N: ожидается число" else null,
                easting = if (e == null) "E: ожидается число" else null,
                height = if (h == null) "H: ожидается число" else null,
            )
            if (errors.any) {
                _state.value = current.copy(errors = errors)
                return
            }

            _state.value = AddPointState.Saving(
                name = name,
                code = current.code,
                northing = current.northing,
                easting = current.easting,
                height = current.height,
            )
            viewModelScope.launch {
                try {
                    val saved = repository.save(
                        NewPoint(
                            projectId = projectId,
                            name = name,
                            code = current.code.trim().ifEmpty { null },
                            layerId = null,
                            n = n!!,
                            e = e!!,
                            h = h!!,
                            source = PointSource.MANUAL,
                        ),
                    )
                    val rev = repository.latestRevisionByName(projectId, name)?.revision ?: 1
                    _state.value = AddPointState.Saved(
                        name = name,
                        code = current.code,
                        northing = current.northing,
                        easting = current.easting,
                        height = current.height,
                        pointId = saved,
                        revision = rev,
                    )
                } catch (t: Throwable) {
                    log.ui("Point save failed for project=$projectId", t)
                    _state.value = AddPointState.Failed(
                        name = name,
                        code = current.code,
                        northing = current.northing,
                        easting = current.easting,
                        height = current.height,
                        message = t.message ?: "Не удалось сохранить точку",
                    )
                }
            }
        }

        fun onAcknowledgeFailure() {
            val current = _state.value
            _state.value = AddPointState.Editing(
                name = current.name,
                code = current.code,
                northing = current.northing,
                easting = current.easting,
                height = current.height,
            )
        }

        private inline fun updateField(transform: AddPointState.Editing.() -> AddPointState.Editing) {
            val current = _state.value as? AddPointState.Editing ?: return
            _state.value = current.transform()
        }

        /**
         * Accept Russian (comma) and English (period) decimal separators —
         * surveyors swap keyboards constantly and either should produce a value.
         */
        private fun parseDecimal(raw: String): Double? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null
            return trimmed.replace(',', '.').toDoubleOrNull()
        }
    }
