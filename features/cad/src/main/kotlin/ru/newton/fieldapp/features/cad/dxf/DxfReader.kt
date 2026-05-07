package ru.newton.fieldapp.features.cad.dxf

/**
 * Hand-rolled ASCII-DXF reader (CAD-001).
 *
 * Why we don't use kabeja: the canonical artifact `org.kabeja:kabeja:0.4`
 * referenced in the original PRD does not exist on Maven Central / Google
 * Maven, and the alternative coordinates we tried also fail. Surveyors only
 * import a tiny subset of DXF (axes are LINE / LWPOLYLINE, control points
 * are POINT) — writing that ourselves is a few hundred lines and is fully
 * testable. If we later need full DXF features (blocks, splines, hatches),
 * we revisit kabeja-or-equivalent.
 *
 * Format reference: DXF is a stream of `group-code` / `value` pairs, one per
 * line. Group code 0 starts a new entity; 8 is the layer; 10/20/30 are X/Y/Z
 * for the primary point; 11/21/31 are the secondary point (LINE end). For
 * LWPOLYLINE, 70 is the flag (1 = closed) and successive 10/20 pairs are
 * vertices.
 */
object DxfReader {
    fun parse(content: String): DxfDrawing {
        val tokens = tokenize(content)
        val entities = mutableListOf<DxfEntity>()
        var i = 0
        // Walk forward to ENTITIES section.
        while (i < tokens.size) {
            val pair = tokens[i]
            if (pair.code == 0 && pair.value == "SECTION") {
                val name = tokens.getOrNull(i + 1)
                if (name?.code == 2 && name.value == "ENTITIES") {
                    i += 2
                    val (parsed, next) = parseEntitiesSection(tokens, i)
                    entities += parsed
                    i = next
                    continue
                }
            }
            i++
        }
        return DxfDrawing(entities)
    }

    private fun parseEntitiesSection(tokens: List<DxfToken>, start: Int): Pair<List<DxfEntity>, Int> {
        val out = mutableListOf<DxfEntity>()
        var i = start
        while (i < tokens.size) {
            val pair = tokens[i]
            if (pair.code == 0 && pair.value == "ENDSEC") return out to (i + 1)
            if (pair.code == 0) {
                val (entity, next) = parseEntity(tokens, i)
                if (entity != null) out += entity
                i = next
                continue
            }
            i++
        }
        return out to i
    }

    private fun parseEntity(tokens: List<DxfToken>, start: Int): Pair<DxfEntity?, Int> {
        val type = tokens[start].value
        var i = start + 1
        var layer = "0"
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var x2 = 0.0
        var y2 = 0.0
        var z2 = 0.0
        var flag = 0
        // CIRCLE/ARC use group code 40 = radius, 50 = start angle, 51 = end angle.
        var radius = 0.0
        var startAngleDeg = 0.0
        var endAngleDeg = 360.0
        // TEXT uses group code 1 for the value, 40 for height.
        var textValue = ""
        var textHeight = 0.0
        val polyVertices = mutableListOf<Vertex>()

        while (i < tokens.size) {
            val pair = tokens[i]
            if (pair.code == 0) break
            when (pair.code) {
                1 -> textValue = pair.value
                8 -> layer = pair.value
                10 -> {
                    val xv = pair.value.toDoubleOrNull() ?: 0.0
                    if (type == "LWPOLYLINE") {
                        polyVertices += Vertex(xv, 0.0, 0.0)
                    } else {
                        x = xv
                    }
                }
                20 -> {
                    val yv = pair.value.toDoubleOrNull() ?: 0.0
                    if (type == "LWPOLYLINE" && polyVertices.isNotEmpty()) {
                        polyVertices[polyVertices.lastIndex] =
                            polyVertices.last().copy(y = yv)
                    } else {
                        y = yv
                    }
                }
                30 -> z = pair.value.toDoubleOrNull() ?: 0.0
                11 -> x2 = pair.value.toDoubleOrNull() ?: 0.0
                21 -> y2 = pair.value.toDoubleOrNull() ?: 0.0
                31 -> z2 = pair.value.toDoubleOrNull() ?: 0.0
                40 -> {
                    val v = pair.value.toDoubleOrNull() ?: 0.0
                    if (type == "TEXT" || type == "MTEXT") textHeight = v else radius = v
                }
                50 -> startAngleDeg = pair.value.toDoubleOrNull() ?: 0.0
                51 -> endAngleDeg = pair.value.toDoubleOrNull() ?: 360.0
                70 -> flag = pair.value.toIntOrNull() ?: 0
            }
            i++
        }

        val entity: DxfEntity? = when (type) {
            "POINT" -> DxfEntity.Point(layer = layer, x = x, y = y, z = z)
            "LINE" -> DxfEntity.Line(layer = layer, x1 = x, y1 = y, z1 = z, x2 = x2, y2 = y2, z2 = z2)
            "LWPOLYLINE" -> DxfEntity.Polyline(
                layer = layer,
                vertices = polyVertices,
                closed = (flag and 1) != 0,
            )
            "CIRCLE" -> DxfEntity.Circle(
                layer = layer,
                cx = x,
                cy = y,
                cz = z,
                radius = radius,
            )
            "ARC" -> DxfEntity.Arc(
                layer = layer,
                cx = x,
                cy = y,
                cz = z,
                radius = radius,
                startAngleDeg = startAngleDeg,
                endAngleDeg = endAngleDeg,
            )
            "TEXT", "MTEXT" -> DxfEntity.Text(
                layer = layer,
                x = x,
                y = y,
                z = z,
                heightM = textHeight,
                value = textValue,
            )
            else -> null
        }
        return entity to i
    }

    private fun tokenize(content: String): List<DxfToken> {
        val out = mutableListOf<DxfToken>()
        val lines = content.lines().map { it.trim() }
        var i = 0
        while (i < lines.size - 1) {
            val codeLine = lines[i]
            val valueLine = lines[i + 1]
            val code = codeLine.toIntOrNull()
            if (code != null) {
                out += DxfToken(code = code, value = valueLine)
                i += 2
            } else {
                i++
            }
        }
        return out
    }

    private data class DxfToken(val code: Int, val value: String)
}
