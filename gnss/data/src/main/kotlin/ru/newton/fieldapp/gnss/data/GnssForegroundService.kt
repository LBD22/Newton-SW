package ru.newton.fieldapp.gnss.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.bluetooth.DataSpp
import ru.newton.fieldapp.core.bluetooth.SppTransport
import ru.newton.fieldapp.core.logging.AppLog
import ru.newton.fieldapp.gnss.command.CommandSession
import ru.newton.fieldapp.gnss.data.parsers.NmeaDispatcher
import ru.newton.fieldapp.gnss.data.parsers.NmeaLineAggregator
import javax.inject.Inject

/**
 * Long-running service that owns the GNSS data pipeline:
 *
 *     SPP socket  →  NmeaLineAggregator  →  NmeaDispatcher  →  GnssStatusStore
 *                                                                      ↑
 *                                                              UI collects here
 *
 * The receiver exposes a single RFCOMM channel; @DataSpp and @CommandSpp
 * qualifiers resolve to the same transport, so one connect() call brings the
 * whole pipe up. The service keeps it alive across Doze and background modes,
 * satisfying the foreground-service-connectedDevice use case declared in
 * `AndroidManifest.xml`.
 */
@AndroidEntryPoint
class GnssForegroundService : Service() {
    @Inject @DataSpp
    lateinit var spp: SppTransport

    @Inject lateinit var statusStore: GnssStatusStore

    @Inject lateinit var commandSession: CommandSession

    @Inject lateinit var log: AppLog

    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(supervisor)
    private val aggregator = NmeaLineAggregator()
    private val dispatcher = NmeaDispatcher()
    private var pipelineJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val mac = intent.getStringExtra(EXTRA_DEVICE_MAC)
                if (mac.isNullOrBlank()) {
                    log.bt("Service start without device MAC — stopping")
                    stopSelf()
                } else {
                    startPipeline(mac)
                }
            }
            ACTION_STOP -> {
                stopPipeline()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPipeline()
        scope.cancel()
        super.onDestroy()
    }

    private fun startPipeline(mac: String) {
        startForegroundCompat(buildNotification(label = "Подключение…"))

        pipelineJob?.cancel()
        pipelineJob = scope.launch {
            launch { spp.connect(mac) }

            // CommandSession starts reading from the shared transport regardless
            // of link state (it filters by content). Handshake fires the first
            // time the link becomes Connected — Apply triggers handshake()
            // lazily otherwise.
            commandSession.start()

            launch {
                spp.incoming.collectLatest { chunk ->
                    val lines = aggregator.feed(chunk)
                    for (line in lines) statusStore.submit(dispatcher.dispatch(line))
                }
            }

            statusStore.status
                .onEach { status -> updateNotification(buildNotification(label = describe(status.fix))) }
                .launchIn(this)
        }
    }

    private fun stopPipeline() {
        pipelineJob?.cancel()
        pipelineJob = null
        statusStore.reset()
        aggregator.reset()
        commandSession.stop()
        scope.launch {
            runCatching { spp.disconnect() }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "GNSS приёмник",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = "Текущее состояние подключения и фикса" },
            )
        }
    }

    private fun updateNotification(notification: Notification) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(label: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Newton")
            .setContentText(label)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun describe(fix: FixQuality): String = when (fix) {
        FixQuality.NoFix -> "Нет фикса"
        FixQuality.Single -> "Single"
        FixQuality.DGnss -> "DGNSS"
        FixQuality.FloatRtk -> "Float RTK"
        FixQuality.FixedRtk -> "Fixed RTK"
        is FixQuality.Ppp -> "PPP (${fix.type})"
    }

    companion object {
        const val ACTION_START = "ru.newton.fieldapp.GnssForegroundService.START"
        const val ACTION_STOP = "ru.newton.fieldapp.GnssForegroundService.STOP"
        const val EXTRA_DEVICE_MAC = "device_mac"

        private const val CHANNEL_ID = "gnss-foreground"
        private const val NOTIFICATION_ID = 0x6E73 // "ns" — Newton service

        fun startIntent(context: Context, deviceMac: String): Intent =
            Intent(context, GnssForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DEVICE_MAC, deviceMac)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, GnssForegroundService::class.java).apply { action = ACTION_STOP }
    }
}
