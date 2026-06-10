package ru.newton.fieldapp.features.settings.ntrip

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.newton.fieldapp.core.logging.AppLog
import ru.newton.fieldapp.gnss.ntrip.NtripClient
import ru.newton.fieldapp.gnss.ntrip.NtripProfile
import ru.newton.fieldapp.gnss.ntrip.NtripProfileRepository

@OptIn(ExperimentalCoroutinesApi::class)
class NtripProfileEditViewModelTest {
    // viewModelScope dispatches on Dispatchers.Main; Unconfined runs launched
    // save() eagerly so the result lands before the test assertions.
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private class FakeRepo : NtripProfileRepository {
        val saved = mutableListOf<NtripProfile>()
        private var nextId = 1L

        override fun observeAll(): Flow<List<NtripProfile>> = flowOf(emptyList())
        override suspend fun byId(id: Long): NtripProfile? = saved.firstOrNull { it.id == id }
        override suspend fun save(profile: NtripProfile): Long {
            saved += profile
            return nextId++
        }
        override suspend fun delete(id: Long) = Unit
    }

    // Save/validation tests never reach the network, so a real client with a
    // no-op logger is enough — it's only constructed, never invoked here.
    private val noopLog = object : AppLog {
        override fun bt(message: String, throwable: Throwable?) = Unit
        override fun gnss(message: String, throwable: Throwable?) = Unit
        override fun cmd(message: String, throwable: Throwable?) = Unit
        override fun ntrip(message: String, throwable: Throwable?) = Unit
        override fun ui(message: String, throwable: Throwable?) = Unit
        override fun general(message: String, throwable: Throwable?) = Unit
        override suspend fun exportArchive(daysBack: Int): String = ""
    }

    private fun newViewModel(repo: NtripProfileRepository) =
        NtripProfileEditViewModel(SavedStateHandle(), repo, NtripClient(noopLog))

    private fun fill(vm: NtripProfileEditViewModel, port: String) {
        vm.onNameChanged("Caster")
        vm.onHostChanged("rtk.example.ru")
        vm.onPortChanged(port)
        vm.onMountpointChanged("MSK")
    }

    // Regression: the old `toIntOrNull()?.takeIf { ... }?.let { null } ?: "error"`
    // produced the error string for VALID ports too, so saving never proceeded.
    @Test
    fun `valid port saves profile with no port error`() = runTest {
        val repo = FakeRepo()
        val vm = newViewModel(repo)
        fill(vm, "2101")

        vm.onSaveClicked()

        val s = vm.state.value
        assertNull(s.errors.port)
        assertNull(s.saveError)
        assertEquals(1, repo.saved.size)
        assertEquals(2101, repo.saved.first().port)
        assertEquals(1L, s.savedId)
    }

    @Test
    fun `out-of-range port blocks save and flags an error`() = runTest {
        val repo = FakeRepo()
        val vm = newViewModel(repo)
        fill(vm, "70000")

        vm.onSaveClicked()

        val s = vm.state.value
        assertEquals("Порт: 1..65535", s.errors.port)
        assertTrue(repo.saved.isEmpty())
        assertNull(s.savedId)
    }

    @Test
    fun `non-numeric port blocks save and flags an error`() = runTest {
        val repo = FakeRepo()
        val vm = newViewModel(repo)
        fill(vm, "abc")

        vm.onSaveClicked()

        assertEquals("Порт: 1..65535", vm.state.value.errors.port)
        assertTrue(repo.saved.isEmpty())
    }
}
