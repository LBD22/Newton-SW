package ru.newton.fieldapp.core.bluetooth

/**
 * State of the single Newton SPP transport.
 *
 * Both `@DataSpp` and `@CommandSpp` injection sites observe the same flow —
 * the qualifiers are role tags over a shared socket, not independent channels.
 */
sealed interface LinkState {
    data object Disconnected : LinkState

    /** A connect attempt is in progress. `attempt` = 1-based retry counter. */
    data class Connecting(
        val attempt: Int,
    ) : LinkState

    /** Socket is open and reading. */
    data class Connected(
        val deviceName: String,
    ) : LinkState

    /**
     * Temporary failure. The transport will retry on its own unless
     * `disconnect()` is called. `cause` is preserved for logging.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : LinkState
}
