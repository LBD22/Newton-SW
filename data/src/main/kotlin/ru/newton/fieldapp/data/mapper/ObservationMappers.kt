package ru.newton.fieldapp.data.mapper

import ru.newton.fieldapp.data.db.entity.ObservationEntity
import ru.newton.fieldapp.domain.model.AntennaMethod
import ru.newton.fieldapp.domain.model.NewObservation
import ru.newton.fieldapp.domain.model.Observation

internal fun ObservationEntity.toDomain(): Observation = Observation(
    id = observationId,
    pointId = pointId,
    revision = 0, // revision lives on the point; observations are 1:1 with the row
    fixType = fixType,
    sigmaN = sigmaN,
    sigmaE = sigmaE,
    sigmaH = sigmaH,
    hdop = hdop,
    pdop = pdop,
    satsUsed = satsUsed,
    correctionAgeSec = correctionAgeSec,
    epochs = epochs,
    antennaHeight = antennaHeightM,
    antennaMethod = runCatching { AntennaMethod.valueOf(antennaMethod) }.getOrDefault(AntennaMethod.VERTICAL),
    tiltApplied = tiltApplied,
    timestampUtc = createdAtUtc,
)

/** Build a fresh entity from a [NewObservation]. `pointId` is rewired on insert. */
internal fun NewObservation.toEntity(): ObservationEntity = ObservationEntity(
    observationId = 0,
    pointId = 0,
    fixType = fixType,
    sigmaN = sigmaN,
    sigmaE = sigmaE,
    sigmaH = sigmaH,
    hdop = hdop,
    pdop = pdop,
    satsUsed = satsUsed,
    correctionAgeSec = correctionAgeSec,
    epochs = epochs,
    antennaHeightM = antennaHeightM,
    antennaMethod = antennaMethod.name,
    tiltApplied = tiltApplied,
    createdAtUtc = timestampUtc,
)
