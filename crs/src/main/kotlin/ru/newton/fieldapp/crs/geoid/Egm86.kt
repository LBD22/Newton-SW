package ru.newton.fieldapp.crs.geoid

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.floor

/**
 * Built-in EGM-86 geoid model.
 *
 * Expects a packed binary grid at the classpath resource [RESOURCE_PATH]:
 *   - 1440 × 721 float32 cells (little-endian)
 *   - Latitude rows from +90° (row 0) down to −90° (row 720) in 0.25° steps
 *   - Longitude columns from 0° (col 0) east through to 359.75° (col 1439)
 *   - Each cell value: geoid undulation in metres.
 *
 * The grid file is **not** shipped with the source tree — it must be dropped
 * into `:crs/src/main/resources/egm86_15min.bin` before release builds.
 * Until then, [undulationM] returns `0.0` everywhere (i.e. acts like
 * [NoGeoid]) and CRS-005 cannot be considered shipped.
 *
 * Reference: Defense Mapping Agency TR 8350.2 (1996), § 5.2 EGM86.
 */
object Egm86 : Geoid {
    private const val RESOURCE_PATH = "/egm86_15min.bin"
    private const val LON_COUNT = 1440
    private const val LAT_COUNT = 721
    private const val STEP_DEG = 0.25

    private val grid: FloatArray? by lazy { tryLoad() }

    override fun undulationM(
        latDeg: Double,
        lonDeg: Double,
    ): Double {
        val data = grid ?: return 0.0

        val normalisedLon = ((lonDeg % 360.0) + 360.0) % 360.0
        val clampedLat = latDeg.coerceIn(-90.0, 90.0)

        val rowReal = (90.0 - clampedLat) / STEP_DEG
        val colReal = normalisedLon / STEP_DEG

        val row0 = floor(rowReal).toInt().coerceIn(0, LAT_COUNT - 2)
        val col0 = floor(colReal).toInt() % LON_COUNT
        val col1 = (col0 + 1) % LON_COUNT

        val dr = rowReal - row0
        val dc = colReal - floor(colReal)

        val v00 = data[row0 * LON_COUNT + col0].toDouble()
        val v01 = data[row0 * LON_COUNT + col1].toDouble()
        val v10 = data[(row0 + 1) * LON_COUNT + col0].toDouble()
        val v11 = data[(row0 + 1) * LON_COUNT + col1].toDouble()

        val v0 = v00 + dc * (v01 - v00)
        val v1 = v10 + dc * (v11 - v10)
        return v0 + dr * (v1 - v0)
    }

    private fun tryLoad(): FloatArray? {
        val stream = Egm86::class.java.getResourceAsStream(RESOURCE_PATH) ?: return null
        return runCatching {
            stream.buffered().use { input ->
                // The grid is little-endian float32 (see class KDoc). DataInputStream
                // reads big-endian, which silently byte-swaps every undulation into
                // garbage — read through a LITTLE_ENDIAN ByteBuffer instead.
                val buffer = ByteBuffer.wrap(input.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
                FloatArray(LON_COUNT * LAT_COUNT) { buffer.float }
            }
        }.getOrNull()
    }
}
