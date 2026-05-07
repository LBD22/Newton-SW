package ru.newton.fieldapp.features.survey.defaults

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.surveyDataStore by preferencesDataStore(name = "survey_defaults")

/**
 * SET-111 — survey defaults persisted in DataStore. Lives in the survey
 * feature module because nothing else cares about these values.
 *
 * Defaults match the field-checklist convention: 10 epochs averaging gives
 * cm-level repeatability under stable RTK; 30 mm horizontal / 50 mm vertical
 * are reasonable starting tolerances for topo work.
 *
 * Also holds quick-survey ergonomics (LandStar parity):
 *  - [SurveyDefaults.namePrefix] / [SurveyDefaults.namePadding] feed the
 *    auto-name generator on the point-survey screen.
 *  - [SurveyDefaults.codeLibrary] populates the quick-tap code chips.
 */
@Singleton
class SurveyPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val defaults: Flow<SurveyDefaults> = context.surveyDataStore.data.map { it.toDefaults() }

        suspend fun update(transform: SurveyDefaults.() -> SurveyDefaults) {
            context.surveyDataStore.edit { prefs ->
                val next = prefs.toDefaults().transform()
                prefs[KEY_MIN_EPOCHS] = next.minEpochs.coerceIn(1, 600)
                prefs[KEY_TOLERANCE_H] = next.toleranceHorizontalM.coerceAtLeast(0.001)
                prefs[KEY_TOLERANCE_V] = next.toleranceVerticalM.coerceAtLeast(0.001)
                prefs[KEY_NAME_PREFIX] = next.namePrefix
                prefs[KEY_NAME_PADDING] = next.namePadding.coerceIn(0, 8)
                // Codes joined with U+001F (Unit Separator) so user-typed
                // codes containing commas or pipes survive a round-trip.
                prefs[KEY_CODE_LIBRARY] = next.codeLibrary.joinToString(SEPARATOR)
                prefs[KEY_TILT_ENABLED] = next.tiltCorrectionEnabled
                prefs[KEY_POLE_HEIGHT] = next.poleHeightM.coerceIn(0.0, 5.0)
            }
        }

        private fun Preferences.toDefaults() = SurveyDefaults(
            minEpochs = this[KEY_MIN_EPOCHS] ?: DEFAULT_MIN_EPOCHS,
            toleranceHorizontalM = this[KEY_TOLERANCE_H] ?: DEFAULT_TOL_H,
            toleranceVerticalM = this[KEY_TOLERANCE_V] ?: DEFAULT_TOL_V,
            namePrefix = this[KEY_NAME_PREFIX] ?: DEFAULT_NAME_PREFIX,
            namePadding = this[KEY_NAME_PADDING] ?: DEFAULT_NAME_PADDING,
            codeLibrary = this[KEY_CODE_LIBRARY]
                ?.split(SEPARATOR)
                ?.filter { it.isNotBlank() }
                ?: DEFAULT_CODE_LIBRARY,
            tiltCorrectionEnabled = this[KEY_TILT_ENABLED] ?: DEFAULT_TILT_ENABLED,
            poleHeightM = this[KEY_POLE_HEIGHT] ?: DEFAULT_POLE_HEIGHT,
        )

        private companion object {
            val KEY_MIN_EPOCHS = intPreferencesKey("min_epochs")
            val KEY_TOLERANCE_H = doublePreferencesKey("tolerance_h_m")
            val KEY_TOLERANCE_V = doublePreferencesKey("tolerance_v_m")
            val KEY_NAME_PREFIX = stringPreferencesKey("name_prefix")
            val KEY_NAME_PADDING = intPreferencesKey("name_padding")
            val KEY_CODE_LIBRARY = stringPreferencesKey("code_library")
            val KEY_TILT_ENABLED = booleanPreferencesKey("tilt_enabled")
            val KEY_POLE_HEIGHT = doublePreferencesKey("pole_height_m")
            const val DEFAULT_MIN_EPOCHS = 10
            const val DEFAULT_TOL_H = 0.030
            const val DEFAULT_TOL_V = 0.050
            const val DEFAULT_NAME_PREFIX = "Pt-"
            const val DEFAULT_NAME_PADDING = 3
            const val DEFAULT_TILT_ENABLED = false
            const val DEFAULT_POLE_HEIGHT = 2.0
            const val SEPARATOR = ""
            val DEFAULT_CODE_LIBRARY = listOf(
                "забор",
                "столб",
                "дерево",
                "люк",
                "бордюр",
                "ось",
                "угол",
                "репер",
            )
        }
    }

data class SurveyDefaults(
    val minEpochs: Int,
    val toleranceHorizontalM: Double,
    val toleranceVerticalM: Double,
    /** Prefix applied to auto-generated point names, e.g. "Pt-" → Pt-001. */
    val namePrefix: String = "Pt-",
    /** Zero-pad numeric suffix to this many digits. 0 means no padding. */
    val namePadding: Int = 3,
    /** Recently-used codes shown as quick-tap chips on PointSurveyScreen. */
    val codeLibrary: List<String> = emptyList(),
    /**
     * When true and the receiver's IMU is valid, [TiltCorrector] reduces each
     * GNSS epoch to the pole tip during point survey. Off by default —
     * surveyors who don't use a tilt-capable receiver should see no change.
     */
    val tiltCorrectionEnabled: Boolean = false,
    /** Pole length in metres (antenna phase centre to ground tip). */
    val poleHeightM: Double = 2.0,
)
