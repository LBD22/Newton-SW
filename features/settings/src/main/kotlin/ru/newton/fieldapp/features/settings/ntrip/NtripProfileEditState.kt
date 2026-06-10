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
    // TLS-by-default — Authorization: Basic over plain HTTP exposes the caster
    // password on shared Wi-Fi. User can flip it off for legacy ports/casters.
    val useTls: Boolean = true,
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
