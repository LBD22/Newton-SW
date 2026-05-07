package ru.newton.fieldapp.domain.receiver

/**
 * Progress events emitted by the Apply flow.
 *
 * A UseCase returning `Flow<ApplyProgress>` starts with Idle, emits a sequence
 * of Sending events as each command goes out, and finishes with either
 * Succeeded or Failed.
 */
sealed interface ApplyProgress {
    data object Idle : ApplyProgress

    data class Sending(
        val current: Int,
        val total: Int,
        val commandText: String,
        val description: String,
    ) : ApplyProgress

    data object Succeeded : ApplyProgress

    data class Failed(
        val reason: String,
        val atStep: Int? = null,
    ) : ApplyProgress
}
