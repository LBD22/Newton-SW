package ru.newton.fieldapp.features.project.create

sealed interface CreateProjectState {
    val name: String
    val comment: String
    val crsPresetId: String

    data class Editing(
        override val name: String = "",
        override val comment: String = "",
        override val crsPresetId: String = DEFAULT_CRS_PRESET_ID,
        val nameError: String? = null,
        val showCrsPicker: Boolean = false,
    ) : CreateProjectState

    data class Saving(
        override val name: String,
        override val comment: String,
        override val crsPresetId: String,
    ) : CreateProjectState

    data class Saved(
        override val name: String,
        override val comment: String,
        override val crsPresetId: String,
        val projectId: Long,
    ) : CreateProjectState

    data class Failed(
        override val name: String,
        override val comment: String,
        override val crsPresetId: String,
        val message: String,
    ) : CreateProjectState

    companion object {
        const val DEFAULT_CRS_PRESET_ID = "WGS84_GEO"
    }
}
