package ru.newton.fieldapp.data.mapper

import ru.newton.fieldapp.data.db.entity.PointEntity
import ru.newton.fieldapp.domain.model.NewPoint
import ru.newton.fieldapp.domain.model.Point
import ru.newton.fieldapp.domain.model.PointSource

internal fun PointEntity.toDomain(): Point = Point(
    id = pointId,
    projectId = projectId,
    name = name,
    code = code,
    layerId = layerId,
    revision = revision,
    n = northingM,
    e = eastingM,
    h = heightM,
    source = PointSource.valueOf(sourceName),
    externalRef = externalRef,
    note = note,
    photoPath = photoPath,
    createdAtUtc = createdAtUtc,
)

/** Build a fresh entity row from a [NewPoint] + revision + timestamp. */
internal fun NewPoint.toEntity(revision: Int, createdAtUtc: Long): PointEntity = PointEntity(
    pointId = 0,
    projectId = projectId,
    name = name,
    code = code,
    layerId = layerId,
    revision = revision,
    northingM = n,
    eastingM = e,
    heightM = h,
    sourceName = source.name,
    externalRef = externalRef,
    note = note,
    photoPath = photoPath,
    createdAtUtc = createdAtUtc,
)
