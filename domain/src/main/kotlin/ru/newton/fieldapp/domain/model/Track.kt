package ru.newton.fieldapp.domain.model

/**
 * A track-recording session — sequence of high-rate position samples captured
 * while the surveyor walks a contour or feature. Distinguished from [Point]s
 * by the relationship: a session is one entity, points are many child samples.
 */
data class TrackSession(
    val id: Long,
    val projectId: Long,
    val name: String,
    val startedAtUtc: Long,
    val stoppedAtUtc: Long?,
) {
    val isActive: Boolean get() = stoppedAtUtc == null
}

data class TrackPointSample(
    val n: Double,
    val e: Double,
    val h: Double,
    val fixQuality: String,
    val timestampUtc: Long,
)
