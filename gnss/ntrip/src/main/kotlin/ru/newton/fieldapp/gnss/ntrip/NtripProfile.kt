package ru.newton.fieldapp.gnss.ntrip

/**
 * Stored NTRIP caster profile. Persisted in Room and read back when the user
 * opens SET-012 / SET-013.
 *
 * Lives in `:gnss:ntrip` (no `:domain` dep allowed) so it can be passed into
 * the client without crossing module boundaries. The persistence layer in
 * `:data` mirrors this with its own `NtripProfileEntity` and maps it to/from
 * this type. The `password` field carries plaintext at this layer; the storage
 * layer is responsible for envelope encryption (NTR-004).
 */
data class NtripProfile(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val mountpoint: String,
    val login: String,
    val password: String,
    val sendNmea: Boolean = false,
    /**
     * Plain TCP by default. The overwhelming majority of NTRIP casters — and
     * every Russian caster we test against (ORSYST/4ГНСС on 2101/2103) — speak
     * plain HTTP/ICY; defaulting to TLS made the first connection fail the SSL
     * handshake with a cryptic error (field report Баг-003). Turn TLS on via the
     * SET-013 toggle for the rare caster that requires it.
     */
    val useTls: Boolean = false,
) {
    /** `https://` if [useTls] is set, otherwise `http://`. */
    val scheme: String get() = if (useTls) "https" else "http"
}
