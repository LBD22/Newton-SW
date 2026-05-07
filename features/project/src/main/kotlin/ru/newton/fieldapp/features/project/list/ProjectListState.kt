package ru.newton.fieldapp.features.project.list

import ru.newton.fieldapp.domain.model.Project

sealed interface ProjectListState {
    data object Loading : ProjectListState

    data object Empty : ProjectListState

    data class Content(
        val projects: List<Project>,
        val activeProjectId: Long? = null,
    ) : ProjectListState

    data class Error(
        val message: String,
    ) : ProjectListState
}
