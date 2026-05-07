package ru.newton.fieldapp.core.bluetooth

/**
 * State of one SPP channel. Per-channel, not aggregated.
 *
 * The two transport instances (`@DataSpp` and `@CommandSpp`) each expose their
 * own `StateFlow<LinkState>`. UI combines them where needed (e.g. status strip)
 * but never conflates them into one.
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
        /** RSSI in dBm if available on this platform, else null. */
        val rssi: Int? = null,
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
