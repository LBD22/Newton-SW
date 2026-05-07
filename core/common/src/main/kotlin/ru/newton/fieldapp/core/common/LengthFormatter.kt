package ru.newton.fieldapp.core.common

/**
 * SET-090 — convert and label distances stored internally in metres to the
 * surveyor's preferred display unit. Internal math is always metres; only the
 * presentation layer touches feet.
 *
 * Conversion uses the international foot (0.3048 m exactly), which is what
 * professional Russian survey kit uses when "ft" is even relevant. We do not
 * support the US survey foot (0.30480061…) — flag with the team if a project
 * surfaces that requirement.
 */
object LengthFormatter {
    private const val M_PER_FT = 0.3048

    fun format(meters: Double, unit: Unit, precision: Int = 3): String =
        when (unit) {
            Unit.METERS -> "%.${precision}f м".format(meters)
            Unit.FEET -> "%.${precision}f фт".format(meters / M_PER_FT)
        }

    enum class Unit { METERS, FEET }
}
