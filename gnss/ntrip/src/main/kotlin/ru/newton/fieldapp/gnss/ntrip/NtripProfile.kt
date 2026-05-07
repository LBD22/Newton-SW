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
     * Default to TLS — `Authorization: Basic` over plain HTTP exposes the caster
     * password on shared Wi-Fi. Legacy NTRIP/1.0 casters that only speak port 2101
     * over HTTP can be opted into via the SET-013 toggle.
     */
    val useTls: Boolean = true,
) {
    /** `https://` if [useTls] is set, otherwise `http://`. */
    val scheme: String get() = if (useTls) "https" else "http"
}
