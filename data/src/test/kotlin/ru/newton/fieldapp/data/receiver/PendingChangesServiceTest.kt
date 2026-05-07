package ru.newton.fieldapp.data.receiver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.domain.receiver.ReceiverConfigPatch
import ru.newton.fieldapp.domain.receiver.RoverMode

class PendingChangesServiceTest {
    /**
     * `Dispatchers.Unconfined` runs every `launch` synchronously on the caller
     * thread, so persistence side-effects are observable immediately after
     * the public service call returns. Production uses `Dispatchers.IO`.
     */
    private fun newService(
        store: PendingPatchStore = FakePendingPatchStore(),
    ) = PendingChangesService(store = store, scope = CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun `initial patch is empty and not dirty`() = runTest {
        val service = newService()
        assertEquals(ReceiverConfigPatch(), service.patch.value)
        assertFalse(service.isDirty())
    }

    @Test
    fun `update mutates and marks dirty`() = runTest {
        val service = newService()
        service.update { it.copy(surveyMaskDeg = 10) }
        assertEquals(10, service.patch.value.surveyMaskDeg)
        assertTrue(service.isDirty())
    }

    @Test
    fun `clear restores empty patch`() = runTest {
        val service = newService()
        service.update { it.copy(surveyMaskDeg = 15, roverMode = RoverMode.ROVER) }
        service.clear()
        assertEquals(ReceiverConfigPatch(), service.patch.value)
        assertFalse(service.isDirty())
    }

    @Test
    fun `replace overwrites wholesale`() = runTest {
        val service = newService()
        service.update { it.copy(surveyMaskDeg = 5) }
        service.replace(ReceiverConfigPatch(roverMode = RoverMode.ROVER_BASE))
        assertEquals(null, service.patch.value.surveyMaskDeg)
        assertEquals(RoverMode.ROVER_BASE, service.patch.value.roverMode)
    }

    @Test
    fun `update is persisted to store`() = runTest {
        val store = FakePendingPatchStore()
        val service = newService(store)
        service.update { it.copy(surveyMaskDeg = 7) }
        assertEquals(ReceiverConfigPatch(surveyMaskDeg = 7), store.persisted)
    }

    @Test
    fun `clear removes the persisted patch`() = runTest {
        val store = FakePendingPatchStore(initial = ReceiverConfigPatch(surveyMaskDeg = 9))
        val service = newService(store)
        // load() runs eagerly on Dispatchers.Unconfined so the StateFlow has caught up.
        assertEquals(9, service.patch.value.surveyMaskDeg)
        service.clear()
        assertNull(store.persisted)
    }

    @Test
    fun `restores patch from store on construction`() = runTest {
        val store = FakePendingPatchStore(initial = ReceiverConfigPatch(roverMode = RoverMode.ROVER_MASTER))
        val service = newService(store)
        assertEquals(RoverMode.ROVER_MASTER, service.patch.value.roverMode)
    }
}

private class FakePendingPatchStore(initial: ReceiverConfigPatch? = null) : PendingPatchStore {
    var persisted: ReceiverConfigPatch? = initial

    override suspend fun load(): ReceiverConfigPatch? = persisted

    override suspend fun save(patch: ReceiverConfigPatch) {
        persisted = patch
    }

    override suspend fun clear() {
        persisted = null
    }
}
