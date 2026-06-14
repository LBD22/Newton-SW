package ru.newton.fieldapp.features.survey.line

import ru.newton.fieldapp.gnss.data.FixQuality

/**
 * SUR-012 — multi-vertex line capture.
 *
 * The surveyor walks along a line feature (kerb, fence, axis), pausing at
 * each vertex; each vertex averages [target] fix epochs. Vertices accumulate
 * into [vertices] and are saved as paired Points with names `<line>_v0..vN`
 * when the user finishes.
 */
sealed interface LineSurveyState {
    data class Idle(val lineName: String) : LineSurveyState

    data class Collecting(
        val lineName: String,
        val collectedVertexCount: Int,
        val collectedAtCurrentVertex: Int,
        val target: Int,
        val currentFix: FixQuality,
    ) : LineSurveyState

    /** Vertex finished averaging — user can start the next or finish. */
    data class BetweenVertices(
        val lineName: String,
        val vertices: List<Vertex>,
    ) : LineSurveyState

    data class Saving(val lineName: String, val total: Int) : LineSurveyState
    data class Saved(val lineName: String, val savedCount: Int) : LineSurveyState
    data class Error(val message: String) : LineSurveyState
}

data class Vertex(
    val n: Double,
    val e: Double,
    val h: Double,
    val sigmaH: Double,
    /** Quality metadata for this vertex's epochs, persisted with the saved point. */
    val observation: ru.newton.fieldapp.domain.model.NewObservation? = null,
)
