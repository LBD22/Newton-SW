package ru.newton.fieldapp.data.receiver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.newton.fieldapp.domain.receiver.ReceiverConfigPatch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for pending receiver configuration changes (CMD-005)
 * persisted via [PendingPatchStore] (CMD-004).
 *
 * Settings screens add to this; the diagnostics/apply screen reads it and
 * triggers the actual sequence via [ApplyReceiverConfigUseCase]. This is the
 * "queue" that the protocol's `system save` flushes.
 *
 * **Invariant:** nothing on the receiver changes until the user presses Apply.
 * Bypassing this service to send commands directly is the autopilot-rejection
 * case from CLAUDE.md.
 *
 * The persisted patch is restored on construction so a crash mid-session does
 * not lose queued settings. Writes are fire-and-forget on the supplied scope
 * — the in-memory `StateFlow` updates synchronously so UI reads are immediate.
 */
@Singleton
class PendingChangesService(
    private val store: PendingPatchStore,
    private val scope: CoroutineScope,
) {
    @Inject
    constructor(store: PendingPatchStore) : this(
        store = store,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    private val _patch = MutableStateFlow(ReceiverConfigPatch())
    val patch: StateFlow<ReceiverConfigPatch> = _patch.asStateFlow()

    init {
        scope.launch {
            store.load()?.let { _patch.value = it }
        }
    }

    /** Apply a transform on the current patch atomically. */
    fun update(transform: (ReceiverConfigPatch) -> ReceiverConfigPatch) {
        _patch.update(transform)
        persist(_patch.value)
    }

    /** Replace the patch wholesale — used when reloading from persisted state. */
    fun replace(patch: ReceiverConfigPatch) {
        _patch.value = patch
        persist(patch)
    }

    /** Clear all pending changes (called after a successful Apply). */
    fun clear() {
        _patch.value = ReceiverConfigPatch()
        scope.launch { store.clear() }
    }

    /** True iff the patch contains any non-null fields. */
    fun isDirty(): Boolean = _patch.value != ReceiverConfigPatch()

    private fun persist(patch: ReceiverConfigPatch) {
        scope.launch {
            if (patch == ReceiverConfigPatch()) store.clear() else store.save(patch)
        }
    }
}
