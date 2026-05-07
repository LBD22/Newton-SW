package ru.newton.fieldapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.activeProjectDataStore by preferencesDataStore(name = "active_project")

/**
 * Single-source-of-truth for "which project is the surveyor working on right now".
 *
 * Survey, stakeout, and apply screens read from this instead of falling back to
 * `ProjectRepository.observeAll().first()` — that fallback was a Sprint 6 hack that
 * silently picked the most-recently-updated project and could surprise the user.
 *
 * `activeId` is `Flow<Long?>`: null when nothing is selected (fresh install or
 * user explicitly cleared the selection). Callers must handle the null case
 * with a UI message ("выберите проект"), not a silent default.
 */
@Singleton
class ActiveProjectStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val activeId: Flow<Long?> = context.activeProjectDataStore.data
            .map { prefs -> prefs[KEY_ACTIVE_ID]?.takeIf { it != 0L } }

        suspend fun setActive(id: Long) {
            context.activeProjectDataStore.edit { it[KEY_ACTIVE_ID] = id }
        }

        suspend fun clear() {
            context.activeProjectDataStore.edit { it.remove(KEY_ACTIVE_ID) }
        }

        private companion object {
            val KEY_ACTIVE_ID = longPreferencesKey("active_project_id")
        }
    }
