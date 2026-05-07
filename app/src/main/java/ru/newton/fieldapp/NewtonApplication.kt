package ru.newton.fieldapp

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import timber.log.Timber

@HiltAndroidApp
class NewtonApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // AppLogImpl writes to <filesDir>/logs/<category>.log directly; no Timber tree
        // is required for file output in release. Logcat in release stays silent by design.

        // OSMDroid Configuration must be loaded before the first MapView is constructed,
        // and the User-Agent must be unique per app to avoid OSM tile-server bans.
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
    }
}
