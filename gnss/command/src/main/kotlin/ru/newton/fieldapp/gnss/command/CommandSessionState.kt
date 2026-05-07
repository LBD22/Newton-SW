package ru.newton.fieldapp.gnss.command

/**
 * Lifecycle of the command port session.
 *
 * Transitions:
 *   Idle ──(handshake started)──▶ Handshaking ──(AT OK, command mode on)──▶ Ready
 *                                              ──(timeout / error)────────▶ Failed
 *   Ready / Failed ──(disconnect)──▶ Idle
 *
 * Commands may ONLY be sent while in Ready. Any code that sends while in
 * Idle / Handshaking / Failed is a bug.
 */
sealed interface CommandSessionState {
    data object Idle : CommandSessionState

    data object Handshaking : CommandSessionState

    data object Ready : CommandSessionState

    data class Failed(
        val reason: String,
    ) : CommandSessionState
}

/**
 * One of the four OK replies from the receiver. See `docs/protocol-newton.md`
 * § OK replies — four variants.
 */
enum class OkKind(
    val token: String,
) {
    /** `AT` response. */
    OK_HANDSHAKE("OK"),

    /** `set command mode on` confirmation. */
    OK_MODE_ON("OK+"),

    /** `set command mode off` confirmation. */
    OK_MODE_OFF("OK-"),

    /** Any queued command acknowledgement, and final `system save`. */
    OK_QUEUED("OK!"),
    ;

    companion object {
        fun fromToken(token: String): OkKind? = entries.firstOrNull { it.token == token.trim() }
    }
}
