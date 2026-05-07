package ru.newton.fieldapp.core.common

/**
 * Formatter for geographic coordinates.
 *
 * Responsible ONLY for number → string conversion. Does NOT handle locale
 * (period vs comma separator) — that's a UI concern handled by the screen
 * via Locale-aware NumberFormat when displaying to the user.
 */
object CoordinateFormatter {
    /** Degrees Decimal, e.g. 55.751244 */
    fun dd(
        valueDeg: Double,
        precision: Int = 8,
    ): String = "%.${precision}f".format(valueDeg)

    /** Degrees Minutes Seconds, e.g. 55°45'04.48"N */
    fun dms(
        valueDeg: Double,
        axis: Axis,
    ): String {
        val abs = kotlin.math.abs(valueDeg)
        val deg = abs.toInt()
        val minFull = (abs - deg) * 60.0
        val min = minFull.toInt()
        val sec = (minFull - min) * 60.0
        val hemi =
            when (axis) {
                Axis.LATITUDE -> if (valueDeg >= 0) "N" else "S"
                Axis.LONGITUDE -> if (valueDeg >= 0) "E" else "W"
            }
        return "%d°%02d'%06.3f\"%s".format(deg, min, sec, hemi)
    }

    /** Projected meters, e.g. 500123.456 */
    fun meters(
        value: Double,
        precision: Int = 3,
    ): String = "%.${precision}f".format(value)

    enum class Axis { LATITUDE, LONGITUDE }
}
