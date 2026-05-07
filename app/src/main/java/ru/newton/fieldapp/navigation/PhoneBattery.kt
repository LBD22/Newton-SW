package ru.newton.fieldapp.navigation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phone battery monitor. Registers for the sticky `ACTION_BATTERY_CHANGED`
 * broadcast and emits the latest percentage as a [Flow]<Int> (0..100, or
 * -1 if unavailable). Cheap — broadcast comes from the OS at every level
 * change, no polling.
 *
 * Newton receiver-side battery isn't accessible: the command-port spec at
 * `reference/OSdoc_command_port_*.docx` does not expose a battery query.
 * If a future firmware adds one, swap this for a Newton-sourced flow.
 */
@Singleton
class PhoneBatteryMonitor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        val percentage: Flow<Int> = callbackFlow {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    intent ?: return
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
                    trySend(pct)
                }
            }
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            // registerReceiver returns the sticky intent immediately so we
            // emit the current level without waiting for a level-change event.
            val sticky = context.registerReceiver(receiver, filter)
            if (sticky != null) {
                val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
                trySend(pct)
            }
            awaitClose { context.unregisterReceiver(receiver) }
        }
    }
