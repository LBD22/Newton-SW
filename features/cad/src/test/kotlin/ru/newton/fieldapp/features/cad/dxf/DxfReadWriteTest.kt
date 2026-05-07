package ru.newton.fieldapp.features.cad.dxf

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DxfReadWriteTest {
    @Test
    fun `reads POINT and LINE entities from a minimal DXF`() {
        val content =
            """
            0
            SECTION
            2
            ENTITIES
            0
            POINT
            8
            CONTROL
            10
            123.456
            20
            789.012
            30
            5.0
            0
            LINE
            8
            AXIS
            10
            0.0
            20
            0.0
            30
            0.0
            11
            10.0
            21
            5.0
            31
            0.0
            0
            ENDSEC
            0
            EOF
            """.trimIndent()
        val drawing = DxfReader.parse(content)
        assertEquals(2, drawing.entities.size)
        val point = drawing.entities[0] as DxfEntity.Point
        assertEquals("CONTROL", point.layer)
        assertEquals(123.456, point.x)
        assertEquals(789.012, point.y)
        val line = drawing.entities[1] as DxfEntity.Line
        assertEquals("AXIS", line.layer)
        assertEquals(10.0, line.x2)
        assertEquals(5.0, line.y2)
    }

    @Test
    fun `read-write round-trip preserves entities`() {
        val original = DxfDrawing(
            entities = listOf(
                DxfEntity.Point("MARKERS", x = 1.0, y = 2.0, z = 0.5),
                DxfEntity.Line("AXIS", x1 = 0.0, y1 = 0.0, x2 = 10.0, y2 = 5.0),
                DxfEntity.Polyline(
                    layer = "OUTLINE",
                    vertices = listOf(
                        Vertex(0.0, 0.0),
                        Vertex(10.0, 0.0),
                        Vertex(10.0, 5.0),
                        Vertex(0.0, 5.0),
                    ),
                    closed = true,
                ),
            ),
        )
        val text = DxfWriter.write(original)
        val parsed = DxfReader.parse(text)
        assertEquals(original.entities.size, parsed.entities.size)
        // Compare each entity within tolerance for the doubles we wrote.
        val parsedPoint = parsed.entities[0] as DxfEntity.Point
        assertEquals("MARKERS", parsedPoint.layer)
        assertEquals(1.0, parsedPoint.x, 1.0e-9)
        val parsedLine = parsed.entities[1] as DxfEntity.Line
        assertEquals(10.0, parsedLine.x2, 1.0e-9)
        val parsedPoly = parsed.entities[2] as DxfEntity.Polyline
        assertEquals(4, parsedPoly.vertices.size)
        assertTrue(parsedPoly.closed)
    }

    @Test
    fun `unknown entity types are skipped without aborting`() {
        // SPLINE is not in our supported set — should be silently dropped.
        val content =
            """
            0
            SECTION
            2
            ENTITIES
            0
            SPLINE
            8
            SP
            10
            0.0
            20
            0.0
            0
            POINT
            8
            P
            10
            1.0
            20
            2.0
            0
            ENDSEC
            0
            EOF
            """.trimIndent()
        val drawing = DxfReader.parse(content)
        assertEquals(1, drawing.entities.size)
        assertTrue(drawing.entities.first() is DxfEntity.Point)
    }

    @Test
    fun `circle arc and text are parsed`() {
        val content =
            """
            0
            SECTION
            2
            ENTITIES
            0
            CIRCLE
            8
            CIRC
            10
            5.0
            20
            7.0
            40
            3.0
            0
            ARC
            8
            ARCS
            10
            0.0
            20
            0.0
            40
            10.0
            50
            0.0
            51
            90.0
            0
            TEXT
            8
            LBL
            10
            1.0
            20
            2.0
            40
            0.5
            1
            Pt-001
            0
            ENDSEC
            0
            EOF
            """.trimIndent()
        val drawing = DxfReader.parse(content)
        assertEquals(3, drawing.entities.size)
        val circle = drawing.entities[0] as DxfEntity.Circle
        assertEquals(5.0, circle.cx, 1e-9)
        assertEquals(3.0, circle.radius, 1e-9)
        val arc = drawing.entities[1] as DxfEntity.Arc
        assertEquals(0.0, arc.startAngleDeg, 1e-9)
        assertEquals(90.0, arc.endAngleDeg, 1e-9)
        val text = drawing.entities[2] as DxfEntity.Text
        assertEquals("Pt-001", text.value)
        assertEquals(0.5, text.heightM, 1e-9)
    }

    @Test
    fun `empty DXF returns no entities`() {
        val drawing = DxfReader.parse("0\nSECTION\n2\nENTITIES\n0\nENDSEC\n0\nEOF\n")
        assertTrue(drawing.entities.isEmpty())
    }
}
