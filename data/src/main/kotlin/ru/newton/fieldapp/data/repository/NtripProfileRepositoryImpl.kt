package ru.newton.fieldapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.newton.fieldapp.data.db.dao.NtripProfileDao
import ru.newton.fieldapp.data.db.entity.NtripProfileEntity
import ru.newton.fieldapp.data.security.EncryptedBytes
import ru.newton.fieldapp.data.security.SecureStorage
import ru.newton.fieldapp.gnss.ntrip.NtripProfile
import ru.newton.fieldapp.gnss.ntrip.NtripProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NtripProfileRepositoryImpl
    @Inject
    constructor(
        private val dao: NtripProfileDao,
        private val secureStorage: SecureStorage,
    ) : NtripProfileRepository {
        override fun observeAll(): Flow<List<NtripProfile>> =
            dao.observeAll().map { rows -> rows.map { it.toDomain() } }

        override suspend fun byId(id: Long): NtripProfile? = dao.byId(id)?.toDomain()

        override suspend fun save(profile: NtripProfile): Long {
            val encrypted = secureStorage.encrypt(profile.password)
            return if (profile.id == 0L) {
                dao.insert(profile.toNewEntity(encrypted))
            } else {
                dao.update(profile.toEntity(encrypted))
                profile.id
            }
        }

        override suspend fun delete(id: Long) = dao.deleteById(id)

        private fun NtripProfileEntity.toDomain(): NtripProfile = NtripProfile(
            id = profileId,
            name = name,
            host = host,
            port = port,
            mountpoint = mountpoint,
            login = login,
            password = secureStorage.decrypt(EncryptedBytes(passwordCiphertext, passwordIv)),
            sendNmea = sendNmea,
            useTls = useTls,
        )

        private fun NtripProfile.toNewEntity(encrypted: EncryptedBytes): NtripProfileEntity =
            NtripProfileEntity(
                profileId = 0,
                name = name,
                host = host,
                port = port,
                mountpoint = mountpoint,
                login = login,
                passwordCiphertext = encrypted.ciphertext,
                passwordIv = encrypted.iv,
                sendNmea = sendNmea,
                useTls = useTls,
            )

        private fun NtripProfile.toEntity(encrypted: EncryptedBytes): NtripProfileEntity =
            toNewEntity(encrypted).copy(profileId = id)
    }
