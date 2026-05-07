package ru.newton.fieldapp.features.cad.dxf

/**
 * In-memory DXF drawing for the survey app's needs.
 *
 * Scope is intentionally narrow: only the entity types that surveyors actually
 * import for stakeout — POINT, LINE, LWPOLYLINE — plus their layer name. We do
 * not represent splines, blocks, dimensions, hatches, or any of the other
 * 50+ entity types in the DXF spec. CAD-001 deliverable.
 */
data class DxfDrawing(
    val entities: List<DxfEntity>,
)

sealed interface DxfEntity {
    val layer: String

    data class Point(
        override val layer: String,
        val x: Double,
        val y: Double,
        val z: Double = 0.0,
    ) : DxfEntity

    data class Line(
        override val layer: String,
        val x1: Double,
        val y1: Double,
        val z1: Double = 0.0,
        val x2: Double,
        val y2: Double,
        val z2: Double = 0.0,
    ) : DxfEntity

    data class Polyline(
        override val layer: String,
        val vertices: List<Vertex>,
        val closed: Boolean = false,
    ) : DxfEntity

    /**
     * CIRCLE: centre + radius. We approximate the circle with a closed
     * 36-vertex polyline at render time, but keep the exact shape here so
     * round-trip export remains accurate.
     */
    data class Circle(
        override val layer: String,
        val cx: Double,
        val cy: Double,
        val cz: Double = 0.0,
        val radius: Double,
    ) : DxfEntity

    /** ARC: centre + radius + start/end angles (degrees, CCW from east). */
    data class Arc(
        override val layer: String,
        val cx: Double,
        val cy: Double,
        val cz: Double = 0.0,
        val radius: Double,
        val startAngleDeg: Double,
        val endAngleDeg: Double,
    ) : DxfEntity

    /** TEXT: insertion point + height + value. Rotation isn't used in MVP. */
    data class Text(
        override val layer: String,
        val x: Double,
        val y: Double,
        val z: Double = 0.0,
        val heightM: Double,
        val value: String,
    ) : DxfEntity
}

data class Vertex(val x: Double, val y: Double, val z: Double = 0.0)
