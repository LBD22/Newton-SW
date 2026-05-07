package ru.newton.fieldapp.core.common

/**
 * Tagged result type for domain operations.
 *
 * Prefer this over `kotlin.Result` when:
 *  - The error has a specific domain meaning that should be typed.
 *  - You want exhaustive `when` on failures, not just `onFailure { _ -> }`.
 *
 * If neither applies, just use `kotlin.Result`.
 */
sealed interface AppResult<out T, out E> {
    data class Ok<T>(
        val value: T,
    ) : AppResult<T, Nothing>

    data class Err<E>(
        val error: E,
    ) : AppResult<Nothing, E>

    fun <R> map(transform: (T) -> R): AppResult<R, E> =
        when (this) {
            is Ok -> Ok(transform(value))
            is Err -> this
        }

    fun <F> mapError(transform: (E) -> F): AppResult<T, F> =
        when (this) {
            is Ok -> this
            is Err -> Err(transform(error))
        }
}

fun <T> T.asOk(): AppResult<T, Nothing> = AppResult.Ok(this)

fun <E> E.asErr(): AppResult<Nothing, E> = AppResult.Err(this)
