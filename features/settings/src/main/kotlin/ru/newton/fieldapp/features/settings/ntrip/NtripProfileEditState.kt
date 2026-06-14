package ru.newton.fieldapp.features.settings.ntrip

import ru.newton.fieldapp.gnss.ntrip.MountpointInfo

data class NtripProfileEditState(
    val id: Long = 0,
    val name: String = "",
    val host: String = "",
    val portText: String = "2101",
    val mountpoint: String = "",
    val login: String = "",
    val password: String = "",
    val sendNmea: Boolean = false,
    // Plain TCP by default — Russian casters (ORSYST/4ГНСС on 2101/2103) speak
    // plain HTTP/ICY; TLS-by-default failed the SSL handshake (Баг-003). User
    // flips TLS on for the rare caster that requires it.
    val useTls: Boolean = false,
    val errors: FieldErrors = FieldErrors(),
    val saving: Boolean = false,
    val savedId: Long? = null,
    // Surfaced when repository.save() throws (e.g. Keystore unavailable, DB
    // constraint). Without this the failure was swallowed and the user saw no
    // feedback — the screen just stayed open looking like nothing happened.
    val saveError: String? = null,
    // Source-table picker (NTR-002): mountpoints fetched from the caster so the
    // user selects instead of hand-typing (a wrong mountpoint silently breaks
    // RTK — see Баг-002/003). Empty until the user taps "Загрузить список точек".
    val mountpoints: List<MountpointInfo> = emptyList(),
    val loadingMountpoints: Boolean = false,
    val mountpointError: String? = null,
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
