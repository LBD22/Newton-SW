package ru.newton.fieldapp.features.project.details

import ru.newton.fieldapp.domain.model.Point
import ru.newton.fieldapp.domain.model.Project

sealed interface ProjectDetailsState {
    data object Loading : ProjectDetailsState

    data class Content(
        val project: Project,
        val points: List<Point>,
    ) : ProjectDetailsState

    /** Project id was not found in the database. */
    data object NotFound : ProjectDetailsState

    data class Error(val message: String) : ProjectDetailsState
}
