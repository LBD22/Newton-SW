package ru.newton.fieldapp.crs

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Site calibration / local localisation — a 4-parameter 2D Helmert
 * (translation + rotation + scale) plus a 1D vertical offset, fit via
 * least-squares to a set of control-point pairs:
 *
 * ```
 *   X = a·x − b·y + dx       (a = s·cosθ, b = s·sinθ)
 *   Y = b·x + a·y + dy
 *   Z = z + dz
 * ```
 *
 * `(x, y, z)` are the surveyor's GNSS-derived coordinates (in the project's
 * CRS), `(X, Y, Z)` are the known local-grid coordinates from the
 * customer's benchmarks. The transform pulls the survey onto the local
 * grid without forcing every measurement to be repeated.
 *
 * 4 parameters → minimum 2 control pairs gives a unique fit (4 equations,
 * 4 unknowns); 3+ pairs make it least-squares. Vertical needs ≥1 pair.
 *
 * The math follows Ghilani & Wolf, *Adjustment Computations* §22.6 — closed
 * form, no matrix-library dependency, runs in microseconds for any sane
 * pair count. Lives in `:crs` because it composes with [CrsTransformer]
 * downstream when we want to apply the transform on every fix.
 */
object LocalCalibration {
    data class Pair2D(
        val measuredN: Double,
        val measuredE: Double,
        val measuredH: Double,
        val knownN: Double,
        val knownE: Double,
        val knownH: Double,
    )

    data class Params(
        /** scale·cos(rotation). */
        val a: Double,
        /** scale·sin(rotation). */
        val b: Double,
        val dx: Double,
        val dy: Double,
        val dz: Double,
    ) {
        val scale: Double get() = hypot(a, b)
        val rotationRad: Double get() = atan2(b, a)
        val rotationDeg: Double get() = Math.toDegrees(rotationRad)
        val translationN: Double get() = dx
        val translationE: Double get() = dy
        val translationH: Double get() = dz

        /** Apply the transform to a measured (n, e, h) coordinate. */
        fun apply(n: Double, e: Double, h: Double): Triple<Double, Double, Double> {
            // The fit was built on (x = N, y = E) so we keep the same convention.
            val xn = a * n - b * e + dx
            val ye = b * n + a * e + dy
            val zh = h + dz
            return Triple(xn, ye, zh)
        }
    }

    data class Residual(
        val pair: Pair2D,
        val deltaN: Double,
        val deltaE: Double,
        val deltaH: Double,
    ) {
        val planar: Double get() = hypot(deltaN, deltaE)
    }

    data class Result(
        val params: Params,
        val residuals: List<Residual>,
    ) {
        val rmsPlanar: Double = if (residuals.isEmpty()) {
            0.0
        } else {
            sqrt(
                residuals.sumOf { it.deltaN * it.deltaN + it.deltaE * it.deltaE } / residuals.size,
            )
        }
        val rmsVertical: Double = if (residuals.isEmpty()) {
            0.0
        } else {
            sqrt(
                residuals.sumOf { it.deltaH * it.deltaH } / residuals.size,
            )
        }
    }

    /**
     * Solve for the best-fit 4-param Helmert + vertical offset. Throws on
     * fewer than 2 pairs (degrees-of-freedom guard) or when the measured
     * points are collinear (denominator = 0 — the rotation is undefined).
     */
    fun solve(pairs: List<Pair2D>): Result {
        require(pairs.size >= 2) { "Нужны минимум 2 контрольные пары (получено ${pairs.size})" }

        // Centroid in measured (small letters) and known (capital letters).
        val n = pairs.size
        val xc = pairs.sumOf { it.measuredN } / n
        val yc = pairs.sumOf { it.measuredE } / n
        val xC = pairs.sumOf { it.knownN } / n
        val yC = pairs.sumOf { it.knownE } / n

        var num1 = 0.0 // Σ((xi−xc)(Xi−Xc) + (yi−yc)(Yi−Yc))   → numerator for a
        var num2 = 0.0 // Σ((xi−xc)(Yi−Yc) − (yi−yc)(Xi−Xc))   → numerator for b
        var den = 0.0  // Σ((xi−xc)² + (yi−yc)²)
        for (p in pairs) {
            val dx = p.measuredN - xc
            val dy = p.measuredE - yc
            val dX = p.knownN - xC
            val dY = p.knownE - yC
            num1 += dx * dX + dy * dY
            num2 += dx * dY - dy * dX
            den += dx * dx + dy * dy
        }
        require(den > 1e-9) {
            "Контрольные точки совпадают или коллинеарны — поворот не определён"
        }
        val a = num1 / den
        val b = num2 / den
        val dx = xC - a * xc + b * yc
        val dy = yC - b * xc - a * yc
        val dz = pairs.sumOf { it.knownH - it.measuredH } / n

        val params = Params(a = a, b = b, dx = dx, dy = dy, dz = dz)

        val residuals = pairs.map { p ->
            val (predN, predE, predH) = params.apply(p.measuredN, p.measuredE, p.measuredH)
            Residual(
                pair = p,
                deltaN = p.knownN - predN,
                deltaE = p.knownE - predE,
                deltaH = p.knownH - predH,
            )
        }
        return Result(params = params, residuals = residuals)
    }
}

/** Convenience wrapper: rotate (n, e) by [rotationRad] without scale or translation. */
internal fun rotate2D(n: Double, e: Double, rotationRad: Double): Pair<Double, Double> {
    val c = cos(rotationRad)
    val s = sin(rotationRad)
    return (c * n - s * e) to (s * n + c * e)
}
