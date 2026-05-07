package ru.newton.fieldapp.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.newton.fieldapp.domain.model.Layer
import ru.newton.fieldapp.domain.model.NewLayer

/**
 * PRJ-030/031 — gateway for the layers table.
 *
 * Implementations are responsible for resolving "ensure this layer name
 * exists" requests from CSV and DXF importers without duplicate insertion
 * (idempotent on `(projectId, name)`).
 */
interface LayerRepository {
    fun observeByProject(projectId: Long): Flow<List<Layer>>
    suspend fun byId(id: Long): Layer?
    suspend fun byName(projectId: Long, name: String): Layer?
    suspend fun create(layer: NewLayer): Layer
    suspend fun update(layer: Layer)
    suspend fun delete(id: Long)

    /**
     * Returns an existing layer with the given name, or creates one with
     * default settings. Used by importers so an unknown layer tag in the
     * file ends up as a real entry the surveyor can colour and toggle.
     */
    suspend fun ensure(projectId: Long, name: String): Layer
}
