package ru.newton.fieldapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.newton.fieldapp.core.ui.theme.NewtonTheme
import ru.newton.fieldapp.data.preferences.LastDeviceStore
import ru.newton.fieldapp.gnss.data.GnssForegroundService
import ru.newton.fieldapp.navigation.NewtonNavHost
import ru.newton.fieldapp.splash.SplashScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var lastDevice: LastDeviceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        autoReconnectIfRemembered()
        setContent {
            NewtonTheme {
                // `rememberSaveable` so a config change (rotation) doesn't
                // replay the intro every time the activity recreates.
                var splashDone by rememberSaveable { mutableStateOf(false) }
                if (splashDone) {
                    NewtonNavHost()
                } else {
                    SplashScreen(onFinished = { splashDone = true })
                }
            }
        }
    }

    /**
     * If the user previously connected to a receiver, the MAC is in
     * [LastDeviceStore]. Starting the foreground service here brings the
     * Bluetooth link up before the user even reaches the Settings tab —
     * matches the expectation that "yesterday's device" is still ours today.
     *
     * Skips when the BT runtime permission isn't granted yet — the user
     * will see the BluetoothConnectScreen prompt and we'll re-trigger
     * naturally from there.
     */
    private fun autoReconnectIfRemembered() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        lifecycleScope.launch {
            val mac = lastDevice.snapshot() ?: return@launch
            ContextCompat.startForegroundService(
                this@MainActivity,
                GnssForegroundService.startIntent(this@MainActivity, mac),
            )
        }
    }
}
