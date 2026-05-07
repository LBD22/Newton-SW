package ru.newton.fieldapp.features.survey.stakeout

import ru.newton.fieldapp.domain.model.Point
import ru.newton.fieldapp.gnss.data.FixQuality

data class StakeoutToLineState(
    val availablePoints: List<Point> = emptyList(),
    val pointA: Point? = null,
    val pointB: Point? = null,
    val vector: LineStakeoutVector? = null,
    val fix: FixQuality = FixQuality.NoFix,
    val errorMessage: String? = null,
)
