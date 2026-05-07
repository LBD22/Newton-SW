package ru.newton.fieldapp.domain.model

/**
 * Recorded as-built outcome of a stakeout (point or line). SUR-132.
 *
 * Layout: target coords (where the surveyor was *trying* to put the stake),
 * actual coords (where they actually planted it), and the residuals
 * (`deltaHorizontalM` / `deltaVerticalM`) — these are what QA reports show.
 */
data class StakeoutResult(
    val id: Long,
    val projectId: Long,
    val targetPointId: Long?,
    val targetLabel: String,
    val mode: StakeoutMode,
    val targetN: Double,
    val targetE: Double,
    val targetH: Double,
    val actualN: Double,
    val actualE: Double,
    val actualH: Double,
    val deltaHorizontalM: Double,
    val deltaVerticalM: Double,
    val savedAtUtc: Long,
)

enum class StakeoutMode { POINT, LINE }
