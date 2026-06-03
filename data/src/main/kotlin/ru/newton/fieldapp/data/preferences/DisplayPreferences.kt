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

private val Context.displayDataStore by preferencesDataStore(name = "display_prefs")

/**
 * Display-related user preferences. Currently:
 *  - `fieldMode` — high-visibility outdoor profile (warmer surface tint,
 *    stronger hairlines, bolder body weight). See `NewtonColors.kt`
 *    `FieldModeNewtonColors`. Default off — sensible indoor look.
 *
 * Backed by a dedicated DataStore so changing this never invalidates the
 * `units_prefs` or `onboarding_prefs` caches that other features rely on.
 */
data class DisplayConfig(
    val fieldMode: Boolean = false,
)

@Singleton
class DisplayPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val config: Flow<DisplayConfig> = context.displayDataStore.data.map { prefs ->
            DisplayConfig(
                fieldMode = prefs[KEY_FIELD_MODE] ?: false,
            )
        }

        suspend fun setFieldMode(enabled: Boolean) {
            context.displayDataStore.edit { it[KEY_FIELD_MODE] = enabled }
        }

        private companion object {
            val KEY_FIELD_MODE = booleanPreferencesKey("field_mode")
        }
    }
