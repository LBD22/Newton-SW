package ru.newton.fieldapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.lastDeviceDataStore by preferencesDataStore(name = "last_device")

/**
 * Persists the MAC of the receiver the user last connected to.
 *
 * Used by `MainActivity` to auto-start `GnssForegroundService` on cold boot
 * — if the surveyor closed the app yesterday with a connection, today the
 * status strip should show the link coming up without manual taps. The MAC
 * is the only piece of identity we need: the device must already be paired
 * via Android system Bluetooth settings.
 */
@Singleton
class LastDeviceStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val mac: Flow<String?> = context.lastDeviceDataStore.data
            .map { prefs -> prefs[KEY_MAC]?.takeIf { it.isNotBlank() } }

        suspend fun set(mac: String) {
            context.lastDeviceDataStore.edit { it[KEY_MAC] = mac }
        }

        suspend fun clear() {
            context.lastDeviceDataStore.edit { it.remove(KEY_MAC) }
        }

        suspend fun snapshot(): String? = mac.first()

        private companion object {
            val KEY_MAC = stringPreferencesKey("last_mac")
        }
    }
