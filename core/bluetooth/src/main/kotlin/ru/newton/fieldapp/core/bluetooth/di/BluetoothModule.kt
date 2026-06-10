package ru.newton.fieldapp.core.bluetooth.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.newton.fieldapp.core.bluetooth.BluetoothSppTransport
import ru.newton.fieldapp.core.bluetooth.CommandSpp
import ru.newton.fieldapp.core.bluetooth.DataSpp
import ru.newton.fieldapp.core.bluetooth.SppTransport
import ru.newton.fieldapp.core.logging.AppLog
import javax.inject.Singleton

/**
 * The Newton receiver exposes a **single** RFCOMM channel under the standard
 * SPP service UUID. Both NMEA/RTCM downstream traffic and the bidirectional
 * command text share that one pipe — the receiver firmware interleaves them.
 *
 * We keep the two Hilt qualifiers (`@DataSpp`, `@CommandSpp`) as semantic role
 * tags for call-site readability, but they resolve to the **same** transport
 * instance. Opening two parallel `BluetoothSocket`s against the same RFCOMM
 * endpoint was the historic source of `read failed, socket might closed or
 * timeout, read ret: -1` on whichever socket lost the race — there is no
 * second channel to bind to.
 */
@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {
    @Provides
    @Singleton
    fun provideBluetoothSppTransport(
        @ApplicationContext context: Context,
        log: AppLog,
    ): BluetoothSppTransport = BluetoothSppTransport(context = context, log = log)

    @Provides
    @Singleton
    @DataSpp
    fun provideDataSpp(transport: BluetoothSppTransport): SppTransport = transport

    @Provides
    @Singleton
    @CommandSpp
    fun provideCommandSpp(transport: BluetoothSppTransport): SppTransport = transport
}
