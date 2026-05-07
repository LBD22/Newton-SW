package ru.newton.fieldapp.domain.model

/**
 * Per-project layer that points (and future lines/polygons) belong to.
 *
 * Coloured chips on the map screen, filterable in the points list, and
 * matched by name when CSV/DXF imports come in with their own layer
 * tags. Visibility is metadata-only — repos do not filter by it.
 */
data class Layer(
    val id: Long,
    val projectId: Long,
    val name: String,
    /** Packed RGB 0xRRGGBB (no alpha). 0xFFFFFF = no specific colour. */
    val colorRgb: Int,
    val visible: Boolean,
    val createdAtUtc: Long,
)

/** Minimum required to insert a new layer. */
data class NewLayer(
    val projectId: Long,
    val name: String,
    val colorRgb: Int = 0xFFFFFF,
    val visible: Boolean = true,
)
