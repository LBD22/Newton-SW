package ru.newton.fieldapp.features.cad.dxf

import ru.newton.fieldapp.domain.model.NewPoint
import ru.newton.fieldapp.domain.model.PointSource
import ru.newton.fieldapp.domain.repository.PointRepository
import javax.inject.Inject

/**
 * Imports a DXF drawing into a project as point records (CAD-002).
 *
 * Mapping rules:
 *  - `POINT` → 1 Point row, name = `<layer>_<i>`.
 *  - `LINE` → 2 rows, names `<layer>_<i>_A` and `<layer>_<i>_B`.
 *  - `LWPOLYLINE` → N rows, names `<layer>_<i>_v0..vN-1`.
 *
 * Native LINE/POLYLINE entities aren't representable in the current point
 * schema (no LineEntity yet), so we expand them into points and tag them by
 * layer name. The user re-creates the geometry in their CAD package after
 * stakeout — surveyors usually discard the line and keep the as-builts anyway.
 *
 * `code` defaults to the DXF layer name so the output CSV/DXF round-trips
 * with the layer information visible. `externalRef` records the originating
 * entity index so duplicate imports can be filtered.
 */
class DxfImportUseCase
    @Inject
    constructor(
        private val pointRepository: PointRepository,
    ) {
        /**
         * @param allowedLayers if non-null, only entities on these layers are
         *   imported. `null` (default) keeps the legacy "import everything"
         *   behaviour for callers that don't show a layer picker.
         */
        suspend operator fun invoke(
            projectId: Long,
            dxfText: String,
            allowedLayers: Set<String>? = null,
        ): Result {
            val drawing = DxfReader.parse(dxfText)
            var saved = 0
            val filteredEntities = if (allowedLayers == null) {
                drawing.entities
            } else {
                drawing.entities.filter { it.layer in allowedLayers }
            }
            filteredEntities.forEachIndexed { index, entity ->
                val baseName = "${entity.layer}_$index"
                when (entity) {
                    is DxfEntity.Point -> {
                        pointRepository.save(
                            NewPoint(
                                projectId = projectId,
                                name = baseName,
                                code = entity.layer,
                                layerId = null,
                                n = entity.y,
                                e = entity.x,
                                h = entity.z,
                                source = PointSource.IMPORT,
                                externalRef = "dxf:point:$index",
                            ),
                        )
                        saved++
                    }
                    is DxfEntity.Line -> {
                        pointRepository.save(
                            NewPoint(
                                projectId = projectId,
                                name = "${baseName}_A",
                                code = entity.layer,
                                layerId = null,
                                n = entity.y1,
                                e = entity.x1,
                                h = entity.z1,
                                source = PointSource.IMPORT,
                                externalRef = "dxf:line:$index:A",
                            ),
                        )
                        pointRepository.save(
                            NewPoint(
                                projectId = projectId,
                                name = "${baseName}_B",
                                code = entity.layer,
                                layerId = null,
                                n = entity.y2,
                                e = entity.x2,
                                h = entity.z2,
                                source = PointSource.IMPORT,
                                externalRef = "dxf:line:$index:B",
                            ),
                        )
                        saved += 2
                    }
                    is DxfEntity.Polyline -> {
                        entity.vertices.forEachIndexed { vIdx, vertex ->
                            pointRepository.save(
                                NewPoint(
                                    projectId = projectId,
                                    name = "${baseName}_v$vIdx",
                                    code = entity.layer,
                                    layerId = null,
                                    n = vertex.y,
                                    e = vertex.x,
                                    h = vertex.z,
                                    source = PointSource.IMPORT,
                                    externalRef = "dxf:poly:$index:$vIdx",
                                ),
                            )
                            saved++
                        }
                    }
                    // CIRCLE / ARC: save the centre as one point. Surveyors
                    // typically need the centre for stakeout; the geometry
                    // itself stays available via the on-map overlay.
                    is DxfEntity.Circle -> {
                        pointRepository.save(
                            NewPoint(
                                projectId = projectId,
                                name = "${baseName}_c",
                                code = entity.layer,
                                layerId = null,
                                n = entity.cy,
                                e = entity.cx,
                                h = entity.cz,
                                source = PointSource.IMPORT,
                                externalRef = "dxf:circle:$index",
                            ),
                        )
                        saved++
                    }
                    is DxfEntity.Arc -> {
                        pointRepository.save(
                            NewPoint(
                                projectId = projectId,
                                name = "${baseName}_a",
                                code = entity.layer,
                                layerId = null,
                                n = entity.cy,
                                e = entity.cx,
                                h = entity.cz,
                                source = PointSource.IMPORT,
                                externalRef = "dxf:arc:$index",
                            ),
                        )
                        saved++
                    }
                    // TEXT: insertion point becomes a point named with the
                    // text value. Useful for labelled control points coming
                    // from the office.
                    is DxfEntity.Text -> {
                        val labelled = entity.value.takeIf { it.isNotBlank() } ?: baseName
                        pointRepository.save(
                            NewPoint(
                                projectId = projectId,
                                name = labelled,
                                code = entity.layer,
                                layerId = null,
                                n = entity.y,
                                e = entity.x,
                                h = entity.z,
                                source = PointSource.IMPORT,
                                externalRef = "dxf:text:$index",
                            ),
                        )
                        saved++
                    }
                }
            }
            return Result(savedPointCount = saved, totalEntities = drawing.entities.size)
        }

        data class Result(val savedPointCount: Int, val totalEntities: Int)
    }

/**
 * CAD-002 — per-layer entity tally for the import wizard's checkbox list.
 * Counts each entity once on its declared layer; LWPOLYLINEs count as 1 even
 * though they expand into multiple points at import time, because the user is
 * selecting features (a polyline is one feature), not save-rows.
 */
data class DxfLayerSummary(val layer: String, val entityCount: Int)

fun DxfDrawing.layerSummaries(): List<DxfLayerSummary> =
    entities
        .groupingBy { it.layer }
        .eachCount()
        .map { (layer, count) -> DxfLayerSummary(layer, count) }
        .sortedBy { it.layer }
