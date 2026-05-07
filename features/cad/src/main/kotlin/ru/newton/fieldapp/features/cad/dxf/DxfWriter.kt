package ru.newton.fieldapp.features.cad.dxf

/**
 * Minimal DXF writer (EXP-001). Emits an ASCII DXF that NanoCAD / AutoCAD
 * accept without warnings for the entity subset we model.
 *
 * Round-trip with [DxfReader] is exact for POINT and LINE; LWPOLYLINE preserves
 * vertices and the closed flag. Layer names are passed through verbatim — the
 * caller is responsible for sanitising (no leading numerics, no spaces) per
 * AutoCAD's layer naming rules.
 */
object DxfWriter {
    fun write(drawing: DxfDrawing): String = buildString {
        appendPair(0, "SECTION")
        appendPair(2, "ENTITIES")
        for (entity in drawing.entities) writeEntity(entity)
        appendPair(0, "ENDSEC")
        appendPair(0, "EOF")
    }

    private fun StringBuilder.writeEntity(entity: DxfEntity) {
        when (entity) {
            is DxfEntity.Point -> {
                appendPair(0, "POINT")
                appendPair(8, entity.layer)
                appendPair(10, format(entity.x))
                appendPair(20, format(entity.y))
                appendPair(30, format(entity.z))
            }
            is DxfEntity.Line -> {
                appendPair(0, "LINE")
                appendPair(8, entity.layer)
                appendPair(10, format(entity.x1))
                appendPair(20, format(entity.y1))
                appendPair(30, format(entity.z1))
                appendPair(11, format(entity.x2))
                appendPair(21, format(entity.y2))
                appendPair(31, format(entity.z2))
            }
            is DxfEntity.Polyline -> {
                appendPair(0, "LWPOLYLINE")
                appendPair(8, entity.layer)
                appendPair(90, entity.vertices.size.toString())
                appendPair(70, if (entity.closed) "1" else "0")
                for (v in entity.vertices) {
                    appendPair(10, format(v.x))
                    appendPair(20, format(v.y))
                }
            }
            is DxfEntity.Circle -> {
                appendPair(0, "CIRCLE")
                appendPair(8, entity.layer)
                appendPair(10, format(entity.cx))
                appendPair(20, format(entity.cy))
                appendPair(30, format(entity.cz))
                appendPair(40, format(entity.radius))
            }
            is DxfEntity.Arc -> {
                appendPair(0, "ARC")
                appendPair(8, entity.layer)
                appendPair(10, format(entity.cx))
                appendPair(20, format(entity.cy))
                appendPair(30, format(entity.cz))
                appendPair(40, format(entity.radius))
                appendPair(50, format(entity.startAngleDeg))
                appendPair(51, format(entity.endAngleDeg))
            }
            is DxfEntity.Text -> {
                appendPair(0, "TEXT")
                appendPair(8, entity.layer)
                appendPair(10, format(entity.x))
                appendPair(20, format(entity.y))
                appendPair(30, format(entity.z))
                appendPair(40, format(entity.heightM))
                appendPair(1, entity.value)
            }
        }
    }

    private fun StringBuilder.appendPair(code: Int, value: String) {
        // DXF group codes are right-padded to 3 chars in some legacy tools, but
        // AutoCAD itself accepts unpadded ASCII — so we keep it simple.
        append(code).append('\n').append(value).append('\n')
    }

    private fun format(value: Double): String =
        // 6 fractional digits = µm precision in metres. Force Locale.US so the
        // decimal separator is always `.` regardless of device locale — DXF
        // expects ASCII numerics, NanoCAD chokes on commas.
        String.format(java.util.Locale.US, "%.6f", value)
}
