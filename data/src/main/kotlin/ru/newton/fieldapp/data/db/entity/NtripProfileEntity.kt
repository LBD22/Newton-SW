package ru.newton.fieldapp.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for an NTRIP caster profile.
 *
 * Password is split into ciphertext + IV BLOBs encrypted via [ru.newton.fieldapp.data.security.SecureStorage]
 * — never store plaintext. Mapping to/from `NtripProfile` happens in the
 * repository, where the SecureStorage is invoked.
 */
@Entity(tableName = "ntrip_profiles")
data class NtripProfileEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "profile_id")
    val profileId: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val mountpoint: String,
    val login: String,
    @ColumnInfo(name = "password_ct")
    val passwordCiphertext: ByteArray,
    @ColumnInfo(name = "password_iv")
    val passwordIv: ByteArray,
    @ColumnInfo(name = "send_nmea")
    val sendNmea: Boolean,
    @ColumnInfo(name = "use_tls", defaultValue = "0")
    val useTls: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NtripProfileEntity) return false
        return profileId == other.profileId &&
            name == other.name &&
            host == other.host &&
            port == other.port &&
            mountpoint == other.mountpoint &&
            login == other.login &&
            passwordCiphertext.contentEquals(other.passwordCiphertext) &&
            passwordIv.contentEquals(other.passwordIv) &&
            sendNmea == other.sendNmea &&
            useTls == other.useTls
    }

    override fun hashCode(): Int {
        var result = profileId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + port
        result = 31 * result + mountpoint.hashCode()
        result = 31 * result + login.hashCode()
        result = 31 * result + passwordCiphertext.contentHashCode()
        result = 31 * result + passwordIv.contentHashCode()
        result = 31 * result + sendNmea.hashCode()
        result = 31 * result + useTls.hashCode()
        return result
    }
}
