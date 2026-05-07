package ru.newton.fieldapp.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around the Android Keystore for envelope-encrypting NTRIP
 * passwords (NTR-004) and other small secrets.
 *
 * Algorithm: AES-256-GCM with a hardware-backed key when available (TEE /
 * StrongBox). The Keystore guarantees the key never leaves the device; the
 * resulting [EncryptedBytes] can be persisted in Room as plain BLOBs.
 *
 * GCM IVs are generated fresh per encryption — never reused — and must be
 * stored alongside the ciphertext. The 16-byte authentication tag is
 * appended to the ciphertext output by the cipher itself; we treat
 * ciphertext as opaque bytes.
 */
@Singleton
class SecureStorage
    @Inject
    constructor() {
        private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        fun encrypt(plaintext: String): EncryptedBytes {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            return EncryptedBytes(ciphertext = ciphertext, iv = iv)
        }

        fun decrypt(payload: EncryptedBytes): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(GCM_TAG_BITS, payload.iv))
            return String(cipher.doFinal(payload.ciphertext), Charsets.UTF_8)
        }

        private fun getOrCreateKey(): SecretKey {
            getKeyOrNull()?.let { return it }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            generator.init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            return generator.generateKey()
        }

        private fun getKey(): SecretKey =
            getKeyOrNull() ?: error("Encryption key '$KEY_ALIAS' missing — was secret saved in a previous app install?")

        private fun getKeyOrNull(): SecretKey? = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey

        private companion object {
            const val KEYSTORE_PROVIDER = "AndroidKeyStore"
            const val KEY_ALIAS = "newton-secure-storage"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val GCM_TAG_BITS = 128
        }
    }

/** Ciphertext + IV pair. Both must be stored alongside each other. */
data class EncryptedBytes(val ciphertext: ByteArray, val iv: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedBytes) return false
        return ciphertext.contentEquals(other.ciphertext) && iv.contentEquals(other.iv)
    }
    override fun hashCode(): Int = 31 * ciphertext.contentHashCode() + iv.contentHashCode()
}
