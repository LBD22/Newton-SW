package ru.newton.fieldapp.data.mapper

import kotlinx.serialization.json.Json
import ru.newton.fieldapp.data.db.entity.ProjectEntity
import ru.newton.fieldapp.domain.model.CrsConfig
import ru.newton.fieldapp.domain.model.Project

internal fun ProjectEntity.toDomain(json: Json): Project =
    Project(
        id = projectId,
        name = name,
        comment = comment,
        crsConfig = json.decodeFromString(CrsConfig.serializer(), crsConfigJson),
        createdAtUtc = createdAtUtc,
        updatedAtUtc = updatedAtUtc,
    )

internal fun Project.toEntity(json: Json): ProjectEntity =
    ProjectEntity(
        projectId = id,
        name = name,
        comment = comment,
        crsConfigJson = json.encodeToString(CrsConfig.serializer(), crsConfig),
        createdAtUtc = createdAtUtc,
        updatedAtUtc = updatedAtUtc,
    )
