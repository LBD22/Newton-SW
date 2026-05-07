package ru.newton.fieldapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

/**
 * APP-002…005 — gates the first-run wizard. Single boolean: once the user
 * finishes the wizard (or explicitly skips), [completed] flips to true and
 * the wizard never shows again. There is no per-step persistence — the
 * wizard is short enough that resuming mid-flow buys nothing.
 */
@Singleton
class OnboardingPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val completed: Flow<Boolean> = context.onboardingDataStore.data
            .map { it[KEY_COMPLETED] ?: false }

        suspend fun setCompleted() {
            context.onboardingDataStore.edit { it[KEY_COMPLETED] = true }
        }

        private companion object {
            val KEY_COMPLETED = booleanPreferencesKey("completed")
        }
    }
