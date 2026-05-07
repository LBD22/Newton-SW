package ru.newton.fieldapp.features.project.addpoint

sealed interface AddPointState {
    val name: String
    val code: String
    val northing: String
    val easting: String
    val height: String

    data class Editing(
        override val name: String = "",
        override val code: String = "",
        override val northing: String = "",
        override val easting: String = "",
        override val height: String = "",
        val errors: FieldErrors = FieldErrors(),
    ) : AddPointState

    data class Saving(
        override val name: String,
        override val code: String,
        override val northing: String,
        override val easting: String,
        override val height: String,
    ) : AddPointState

    data class Saved(
        override val name: String,
        override val code: String,
        override val northing: String,
        override val easting: String,
        override val height: String,
        val pointId: Long,
        val revision: Int,
    ) : AddPointState

    data class Failed(
        override val name: String,
        override val code: String,
        override val northing: String,
        override val easting: String,
        override val height: String,
        val message: String,
    ) : AddPointState

    /** One slot per validatable field; null means OK, non-null is the user-facing error message. */
    data class FieldErrors(
        val name: String? = null,
        val northing: String? = null,
        val easting: String? = null,
        val height: String? = null,
    ) {
        val any: Boolean get() = name != null || northing != null || easting != null || height != null
    }
}
