package ru.newton.fieldapp.core.logging

/**
 * Application logger with categorised output.
 *
 * Use this instead of `Timber.d(...)` / `Log.d(...)` directly. It tags every
 * entry with a category so the diagnostics screen (SET-080) can filter and
 * export subsets.
 *
 * All categories log to:
 *  - Logcat (debug builds via Timber DebugTree, release builds filtered to WARN+)
 *  - File (always): rolling buffer, 7 days, ~20 MB per category.
 *
 * Implementation lives in [AppLogImpl]. Injected as singleton via Hilt.
 */
interface AppLog {
    /** Bluetooth transport events: connect, disconnect, socket IO failures. */
    fun bt(
        message: String,
        throwable: Throwable? = null,
    )

    /** GNSS data parsing and status updates. High-volume — guard with debug filter. */
    fun gnss(
        message: String,
        throwable: Throwable? = null,
    )

    /** Newton command port: commands sent, replies, handshake status. */
    fun cmd(
        message: String,
        throwable: Throwable? = null,
    )

    /** NTRIP client events: connect, mount selection, byte counters, reconnects. */
    fun ntrip(
        message: String,
        throwable: Throwable? = null,
    )

    /** UI-layer anomalies: unexpected state transitions, user errors. */
    fun ui(
        message: String,
        throwable: Throwable? = null,
    )

    /** Any other events. Use sparingly — prefer adding a dedicated category. */
    fun general(
        message: String,
        throwable: Throwable? = null,
    )

    /**
     * Export the last N days of logs as a zip file.
     * @return absolute path to the written archive.
     */
    suspend fun exportArchive(daysBack: Int = 7): String
}
