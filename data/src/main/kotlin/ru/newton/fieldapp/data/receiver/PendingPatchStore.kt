package ru.newton.fieldapp.data.receiver

import kotlinx.serialization.json.Json
import ru.newton.fieldapp.data.db.dao.PendingPatchDao
import ru.newton.fieldapp.data.db.entity.PendingPatchEntity
import ru.newton.fieldapp.domain.receiver.ReceiverConfigPatch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CMD-004 — persistence boundary for [ReceiverConfigPatch]. Defining an
 * interface keeps [PendingChangesService] testable: unit tests inject an
 * in-memory fake instead of requiring Room + Android Test infrastructure.
 */
interface PendingPatchStore {
    suspend fun load(): ReceiverConfigPatch?
    suspend fun save(patch: ReceiverConfigPatch)
    suspend fun clear()
}

/**
 * Room-backed implementation. Single-row table — every save is an upsert on
 * `id = SINGLETON_ID`. Failure to deserialize (e.g. after a schema change
 * that adds a non-nullable field) is treated as "no persisted patch" so the
 * user lands in a clean state instead of a crash loop.
 */
@Singleton
class RoomPendingPatchStore
    @Inject
    constructor(
        private val dao: PendingPatchDao,
        private val json: Json,
    ) : PendingPatchStore {
        override suspend fun load(): ReceiverConfigPatch? = runCatching {
            dao.get()?.patchJson?.let { json.decodeFromString<ReceiverConfigPatch>(it) }
        }.getOrNull()

        override suspend fun save(patch: ReceiverConfigPatch) {
            dao.upsert(
                PendingPatchEntity(
                    patchJson = json.encodeToString(ReceiverConfigPatch.serializer(), patch),
                    updatedAtUtc = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun clear() {
            dao.deleteById()
        }
    }
