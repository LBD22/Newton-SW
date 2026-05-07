package ru.newton.fieldapp.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.newton.fieldapp.core.bluetooth.CommandSpp
import ru.newton.fieldapp.core.bluetooth.DataSpp
import ru.newton.fieldapp.core.bluetooth.LinkState
import ru.newton.fieldapp.core.bluetooth.SppTransport
import ru.newton.fieldapp.core.ui.components.NewtonStatusPill
import ru.newton.fieldapp.core.ui.theme.LocalFixStatusColors
import ru.newton.fieldapp.data.receiver.CommandQueueRepository
import ru.newton.fieldapp.gnss.data.FixQuality
import ru.newton.fieldapp.gnss.data.GnssStatus
import ru.newton.fieldapp.gnss.data.GnssStatusStore
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * Always-visible top strip that summarises the receiver state, styled after
 * the corporate dashboard reference: a horizontal row of pill chips with
 * icons, plus a high-emphasis filled chip in the centre showing the headline
 * fix label.
 *
 * Reads from [GnssStatusStore] + both [SppTransport.linkState]s (DataSPP,
 * CommandSPP) via a small dedicated ViewModel — the architecture rule is
 * that UI never touches transports or stores directly.
 */
@Composable
fun GnssStatusStrip(modifier: Modifier = Modifier, viewModel: GnssStatusStripViewModel = hiltViewModel()) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    GnssStatusStripContent(snapshot = snapshot, modifier = modifier)
}

@Composable
private fun GnssStatusStripContent(snapshot: StatusStripSnapshot, modifier: Modifier = Modifier) {
    val fixColors = LocalFixStatusColors.current
    val tintForFix = when (snapshot.fix) {
        FixQuality.NoFix -> fixColors.noFix
        FixQuality.Single, FixQuality.DGnss -> fixColors.single
        FixQuality.FloatRtk -> fixColors.float
        FixQuality.FixedRtk -> fixColors.fixed
        is FixQuality.Ppp -> fixColors.float
    }
    val timeText = remember(snapshot.timestampUtc) { formatTime(snapshot.timestampUtc) }
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NewtonStatusPill(
            text = "SAT ${snapshot.satsUsed}",
            icon = Icons.Default.SatelliteAlt,
        )
        NewtonStatusPill(
            text = describeFix(snapshot.fix),
            icon = Icons.Default.GpsFixed,
            background = tintForFix.copy(alpha = 0.16f),
            contentColor = tintForFix,
        )
        timeText?.let {
            NewtonStatusPill(
                text = it,
                icon = Icons.Default.AccessTime,
            )
        }
        snapshot.sigmaH?.let { sigma ->
            NewtonStatusPill(
                text = "σH ${"%.2f".format(sigma)} м",
                icon = Icons.Default.Straighten,
            )
        }
        // RTK age is the closest signal we have to "is the correction stream
        // healthy". Tinted red when over 10 s — a stale baseline can poison
        // the fix without a visible drop in fix-quality label.
        snapshot.correctionAgeSec?.let { age ->
            val stale = age > 10.0
            NewtonStatusPill(
                text = "Δt ${"%.0f".format(age)} с",
                icon = Icons.Default.History,
                background = if (stale) fixColors.noFix.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (stale) fixColors.noFix else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val mutedBg = MaterialTheme.colorScheme.surfaceVariant
        NewtonStatusPill(
            text = if (snapshot.dataLinked) "BT-D ✓" else "BT-D —",
            icon = Icons.Default.Bluetooth,
            background = if (snapshot.dataLinked) fixColors.fixed.copy(alpha = 0.16f) else mutedBg,
            contentColor = if (snapshot.dataLinked) fixColors.fixed else fixColors.noFix,
        )
        NewtonStatusPill(
            text = if (snapshot.commandLinked) "BT-C ✓" else "BT-C —",
            icon = Icons.Default.Sensors,
            background = if (snapshot.commandLinked) fixColors.fixed.copy(alpha = 0.16f) else mutedBg,
            contentColor = if (snapshot.commandLinked) fixColors.fixed else fixColors.noFix,
        )
        if (snapshot.phoneBatteryPct in 0..100) {
            // "тел" prefix to make clear this is the *phone*, not the receiver
            // — the Newton command-port spec doesn't expose battery state.
            val low = snapshot.phoneBatteryPct < 20
            NewtonStatusPill(
                text = "тел ${snapshot.phoneBatteryPct}%",
                icon = Icons.Default.BatteryStd,
                background = if (low) fixColors.noFix.copy(alpha = 0.16f) else mutedBg,
                contentColor = if (low) fixColors.noFix else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // DevHandbook §6.4: surface "N pending commands" so the surveyor
        // sees outstanding configuration changes without opening the
        // diagnostics screen.
        if (snapshot.pendingCommandCount > 0) {
            NewtonStatusPill(
                text = "очередь ${snapshot.pendingCommandCount}",
                icon = Icons.Default.PendingActions,
                background = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun describeFix(fix: FixQuality): String = when (fix) {
    FixQuality.NoFix -> "Нет фикса"
    FixQuality.Single -> "Single"
    FixQuality.DGnss -> "DGNSS"
    FixQuality.FloatRtk -> "FLOAT"
    FixQuality.FixedRtk -> "FIX"
    is FixQuality.Ppp -> "PPP"
}

private fun formatTime(timestampUtc: Long): String? {
    if (timestampUtc <= 0L) return null
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date(timestampUtc))
}

data class StatusStripSnapshot(
    val fix: FixQuality,
    val satsUsed: Int,
    val sigmaH: Double?,
    val correctionAgeSec: Double?,
    val dataLinked: Boolean,
    val commandLinked: Boolean,
    val timestampUtc: Long,
    val phoneBatteryPct: Int,
    /** DevHandbook §6.4 — number of commands waiting for Apply. */
    val pendingCommandCount: Int,
) {
    companion object {
        fun from(
            status: GnssStatus,
            dataState: LinkState,
            commandState: LinkState,
            phoneBattery: Int,
            pendingCommandCount: Int,
        ): StatusStripSnapshot = StatusStripSnapshot(
            fix = status.fix,
            satsUsed = status.satsUsed,
            sigmaH = status.sigmaH,
            correctionAgeSec = status.correctionAgeSec,
            dataLinked = dataState is LinkState.Connected,
            commandLinked = commandState is LinkState.Connected,
            timestampUtc = status.timestampUtc,
            phoneBatteryPct = phoneBattery,
            pendingCommandCount = pendingCommandCount,
        )
    }
}

@HiltViewModel
class GnssStatusStripViewModel
    @Inject
    constructor(
        store: GnssStatusStore,
        @DataSpp dataSpp: SppTransport,
        @CommandSpp commandSpp: SppTransport,
        battery: PhoneBatteryMonitor,
        commandQueue: CommandQueueRepository,
    ) : ViewModel() {
        val state: StateFlow<StatusStripSnapshot> = combine(
            store.status,
            dataSpp.linkState,
            commandSpp.linkState,
            battery.percentage,
            commandQueue.observePendingCount(),
        ) { values ->
            val status = values[0] as GnssStatus
            val data = values[1] as LinkState
            val command = values[2] as LinkState
            val pct = values[3] as Int
            val pending = values[4] as Int
            StatusStripSnapshot.from(status, data, command, pct, pending)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StatusStripSnapshot(
                fix = FixQuality.NoFix,
                satsUsed = 0,
                sigmaH = null,
                correctionAgeSec = null,
                dataLinked = false,
                commandLinked = false,
                timestampUtc = 0L,
                phoneBatteryPct = -1,
                pendingCommandCount = 0,
            ),
        )
    }
