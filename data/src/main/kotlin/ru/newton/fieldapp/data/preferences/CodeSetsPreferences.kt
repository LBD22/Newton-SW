package ru.newton.fieldapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.codeSetsDataStore by preferencesDataStore(name = "code_sets")

/**
 * PRJ-020/021 — named code libraries the surveyor saves and reuses across
 * projects.
 *
 * Active code library on [ru.newton.fieldapp.features.survey.defaults.SurveyDefaults]
 * stays as a single flat list (the working set). This preference store
 * adds **named saved sets** the user can load on demand: «Топосъёмка»,
 * «Подземные коммуникации», «Стройка». Loading a set replaces the
 * working list; editing the working list does NOT touch the saved set
 * until the user explicitly «Сохранить как…».
 */
@Serializable
data class CodeSet(val name: String, val codes: List<String>)

@Singleton
class CodeSetsPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val json: Json,
    ) {
        val sets: Flow<List<CodeSet>> = context.codeSetsDataStore.data.map { prefs ->
            decodeSets(json, prefs[KEY_SETS])
        }

        suspend fun save(set: CodeSet) {
            context.codeSetsDataStore.edit { prefs ->
                val existing = decodeSets(json, prefs[KEY_SETS])
                prefs[KEY_SETS] = encodeSets(json, mergeSet(existing, set))
            }
        }

        suspend fun delete(name: String) {
            context.codeSetsDataStore.edit { prefs ->
                val existing = decodeSets(json, prefs[KEY_SETS])
                if (existing.isEmpty()) return@edit
                prefs[KEY_SETS] = encodeSets(json, removeSet(existing, name))
            }
        }

        private companion object {
            val KEY_SETS = stringPreferencesKey("sets_json")
        }
    }

internal val CODE_SETS_SERIALIZER = ListSerializer(CodeSet.serializer())

/** Decode a stored JSON blob into the saved sets, returning an empty list on
 *  null/blank/corrupt input. Corruption is silent — the caller has no recovery
 *  path other than overwriting, and surfacing errors to the user would be
 *  noise (the saved sets are not critical state). */
internal fun decodeSets(json: Json, raw: String?): List<CodeSet> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { json.decodeFromString(CODE_SETS_SERIALIZER, raw) }.getOrNull() ?: emptyList()
}

internal fun encodeSets(json: Json, sets: List<CodeSet>): String =
    json.encodeToString(CODE_SETS_SERIALIZER, sets)

/** Insert-or-replace by case-insensitive name. */
internal fun mergeSet(existing: List<CodeSet>, set: CodeSet): List<CodeSet> {
    val idx = existing.indexOfFirst { it.name.equals(set.name, ignoreCase = true) }
    return if (idx >= 0) {
        existing.toMutableList().apply { this[idx] = set }
    } else {
        existing + set
    }
}

internal fun removeSet(existing: List<CodeSet>, name: String): List<CodeSet> =
    existing.filterNot { it.name.equals(name, ignoreCase = true) }
