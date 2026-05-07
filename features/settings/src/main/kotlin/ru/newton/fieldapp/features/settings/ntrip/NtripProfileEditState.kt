package ru.newton.fieldapp.features.settings.ntrip

data class NtripProfileEditState(
    val id: Long = 0,
    val name: String = "",
    val host: String = "",
    val portText: String = "2101",
    val mountpoint: String = "",
    val login: String = "",
    val password: String = "",
    val sendNmea: Boolean = false,
    // TLS-by-default — Authorization: Basic over plain HTTP exposes the caster
    // password on shared Wi-Fi. User can flip it off for legacy ports/casters.
    val useTls: Boolean = true,
    val errors: FieldErrors = FieldErrors(),
    val saving: Boolean = false,
    val savedId: Long? = null,
) {
    data class FieldErrors(
        val name: String? = null,
        val host: String? = null,
        val port: String? = null,
        val mountpoint: String? = null,
    ) {
        val any: Boolean get() = name != null || host != null || port != null || mountpoint != null
    }
}
