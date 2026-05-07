package ru.newton.fieldapp.data.preferences

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.offlineTilesDataStore by preferencesDataStore(name = "offline_tiles")

/**
 * UI-002 (carry-over) — pointer to the active offline MBTiles archive used by
 * the map screen. We store a relative file name (not an absolute path so the
 * config survives restore-from-backup), and resolve it under
 * [Context.getFilesDir]`/offline_tiles/` at read time. `null` = no archive
 * active, fall back to OSM online tiles.
 *
 * [import] copies the user-picked SAF document into our private dir because
 * OSMDroid's `OfflineTileProvider` needs a [File], not a stream. The copy
 * persists across reboots; deleting the file directory clears it.
 */
@Singleton
class OfflineTilesPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val activeFile: Flow<File?> = context.offlineTilesDataStore.data.map { prefs ->
            prefs[KEY_FILENAME]?.let { name -> resolve(name).takeIf { it.exists() } }
        }

        suspend fun import(source: Uri, originalName: String): File = withContext(Dispatchers.IO) {
            val safeName = originalName
                .substringAfterLast('/')
                .substringAfterLast('\\')
                .ifEmpty { "tiles.mbtiles" }
            val dir = File(context.filesDir, OFFLINE_DIR).apply { mkdirs() }
            val target = File(dir, safeName)
            context.contentResolver.openInputStream(source).use { input ->
                requireNotNull(input) { "ContentResolver returned null stream for $source" }
                target.outputStream().use { output -> input.copyTo(output) }
            }
            context.offlineTilesDataStore.edit { it[KEY_FILENAME] = safeName }
            target
        }

        suspend fun clear() {
            val dir = File(context.filesDir, OFFLINE_DIR)
            if (dir.exists()) dir.listFiles()?.forEach { it.delete() }
            context.offlineTilesDataStore.edit { it.remove(KEY_FILENAME) }
        }

        private fun resolve(name: String): File =
            File(File(context.filesDir, OFFLINE_DIR), name)

        private companion object {
            const val OFFLINE_DIR = "offline_tiles"
            val KEY_FILENAME = stringPreferencesKey("filename")
        }
    }
