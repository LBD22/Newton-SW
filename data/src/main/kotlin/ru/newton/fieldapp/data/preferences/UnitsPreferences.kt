package ru.newton.fieldapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.unitsDataStore by preferencesDataStore(name = "units_prefs")

/**
 * SET-090/091 — display units and angular format.
 *
 * Stored as enum names so the persisted file remains stable when we add new
 * options later (e.g. METRES_KM, FEET_INTL_VS_US_SURVEY). Defaults match the
 * existing app behaviour: metres + decimal degrees.
 */
enum class LengthUnit { METERS, FEET }

enum class AngleFormat { DECIMAL_DEGREES, DMS }

data class UnitsConfig(
    val length: LengthUnit = LengthUnit.METERS,
    val angle: AngleFormat = AngleFormat.DECIMAL_DEGREES,
)

@Singleton
class UnitsPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val config: Flow<UnitsConfig> = context.unitsDataStore.data.map { prefs ->
            UnitsConfig(
                length = prefs[KEY_LENGTH]?.let { LengthUnit.valueOf(it) } ?: LengthUnit.METERS,
                angle = prefs[KEY_ANGLE]?.let { AngleFormat.valueOf(it) } ?: AngleFormat.DECIMAL_DEGREES,
            )
        }

        suspend fun setLength(unit: LengthUnit) {
            context.unitsDataStore.edit { it[KEY_LENGTH] = unit.name }
        }

        suspend fun setAngle(format: AngleFormat) {
            context.unitsDataStore.edit { it[KEY_ANGLE] = format.name }
        }

        private companion object {
            val KEY_LENGTH = stringPreferencesKey("length_unit")
            val KEY_ANGLE = stringPreferencesKey("angle_format")
        }
    }
