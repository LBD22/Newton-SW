package ru.newton.fieldapp.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.data.db.dao.LayerDao
import ru.newton.fieldapp.data.db.entity.LayerEntity
import ru.newton.fieldapp.domain.model.NewLayer

class LayerRepositoryImplTest {
    @Test
    fun `create trims name and assigns generated id`() = runTest {
        val dao = FakeLayerDao()
        val repo = LayerRepositoryImpl(dao)

        val layer = repo.create(NewLayer(projectId = 1L, name = "  Topo  ", colorRgb = 0xFF8800))

        assertEquals("Topo", layer.name)
        assertEquals(1L, layer.id)
        assertEquals(0xFF8800, layer.colorRgb)
        assertEquals(1, dao.insertCalls)
    }

    @Test
    fun `create returns existing row when insert hits unique conflict`() = runTest {
        val dao = FakeLayerDao().apply {
            preinsert(LayerEntity(layerId = 42L, projectId = 1L, name = "Boundary", colorRgb = 0xCCCCCC, visible = true, createdAtUtc = 0L))
            simulateConflict = true
        }
        val repo = LayerRepositoryImpl(dao)

        val result = repo.create(NewLayer(projectId = 1L, name = "Boundary"))

        assertEquals(42L, result.id)
        assertEquals("Boundary", result.name)
        assertEquals(0xCCCCCC, result.colorRgb)
    }

    @Test
    fun `create throws if conflict reported but byName lookup also misses`() = runTest {
        val dao = FakeLayerDao().apply { simulateConflict = true }
        val repo = LayerRepositoryImpl(dao)

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                repo.create(NewLayer(projectId = 1L, name = "Ghost"))
            }
        }
    }

    @Test
    fun `ensure returns existing row without calling insert`() = runTest {
        val dao = FakeLayerDao().apply {
            preinsert(LayerEntity(layerId = 5L, projectId = 1L, name = "Roads", colorRgb = 0xFFFFFF, visible = true, createdAtUtc = 0L))
        }
        val repo = LayerRepositoryImpl(dao)

        val result = repo.ensure(1L, "Roads")

        assertEquals(5L, result.id)
        assertEquals(0, dao.insertCalls)
    }

    @Test
    fun `ensure inserts a default-coloured layer when missing`() = runTest {
        val dao = FakeLayerDao()
        val repo = LayerRepositoryImpl(dao)

        val result = repo.ensure(7L, "  Buildings  ")

        assertEquals("Buildings", result.name)
        assertEquals(7L, result.projectId)
        assertEquals(0xFFFFFF, result.colorRgb)
        assertEquals(1, dao.insertCalls)
    }

    @Test
    fun `ensure rejects empty or whitespace-only name`() {
        val repo = LayerRepositoryImpl(FakeLayerDao())
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { repo.ensure(1L, "   ") }
        }
    }

    @Test
    fun `update writes through to dao`() = runTest {
        val dao = FakeLayerDao().apply {
            preinsert(LayerEntity(layerId = 9L, projectId = 1L, name = "Old", colorRgb = 0, visible = true, createdAtUtc = 0L))
        }
        val repo = LayerRepositoryImpl(dao)
        val existing = repo.byId(9L)!!

        repo.update(existing.copy(name = "  New  ", visible = false, colorRgb = 0x123456))

        val refreshed = repo.byId(9L)!!
        assertEquals("New", refreshed.name)
        assertEquals(false, refreshed.visible)
        assertEquals(0x123456, refreshed.colorRgb)
    }

    @Test
    fun `delete removes by id`() = runTest {
        val dao = FakeLayerDao().apply {
            preinsert(LayerEntity(layerId = 11L, projectId = 1L, name = "Tmp", colorRgb = 0, visible = true, createdAtUtc = 0L))
        }
        val repo = LayerRepositoryImpl(dao)

        repo.delete(11L)

        assertNull(repo.byId(11L))
    }

    @Test
    fun `observeByProject filters by project and sorts by name`() = runTest {
        val dao = FakeLayerDao().apply {
            preinsert(LayerEntity(layerId = 1, projectId = 1, name = "Buildings", colorRgb = 0, visible = true, createdAtUtc = 0))
            preinsert(LayerEntity(layerId = 2, projectId = 1, name = "Roads", colorRgb = 0, visible = true, createdAtUtc = 0))
            preinsert(LayerEntity(layerId = 3, projectId = 2, name = "OtherProject", colorRgb = 0, visible = true, createdAtUtc = 0))
        }
        val repo = LayerRepositoryImpl(dao)

        val rows = repo.observeByProject(1L).first()

        assertEquals(listOf("Buildings", "Roads"), rows.map { it.name })
    }
}

private class FakeLayerDao : LayerDao {
    private val rows = mutableMapOf<Long, LayerEntity>()
    private val flow = MutableStateFlow<List<LayerEntity>>(emptyList())
    private var nextId = 1L
    var insertCalls = 0
    var simulateConflict = false

    fun preinsert(entity: LayerEntity) {
        rows[entity.layerId] = entity
        if (entity.layerId >= nextId) nextId = entity.layerId + 1
        flow.value = rows.values.toList()
    }

    override fun observeByProject(projectId: Long): Flow<List<LayerEntity>> =
        kotlinx.coroutines.flow.MutableStateFlow(
            rows.values.filter { it.projectId == projectId }.sortedBy { it.name },
        )

    override suspend fun byId(id: Long): LayerEntity? = rows[id]

    override suspend fun byName(projectId: Long, name: String): LayerEntity? =
        rows.values.firstOrNull { it.projectId == projectId && it.name == name }

    override suspend fun insert(layer: LayerEntity): Long {
        insertCalls++
        if (simulateConflict) return -1L
        val id = nextId++
        rows[id] = layer.copy(layerId = id)
        flow.value = rows.values.toList()
        return id
    }

    override suspend fun update(layer: LayerEntity) {
        rows[layer.layerId] = layer
        flow.value = rows.values.toList()
    }

    override suspend fun deleteById(id: Long) {
        rows.remove(id)
        flow.value = rows.values.toList()
    }
}
