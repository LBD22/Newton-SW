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
 * Two-transport binding for the Newton receiver: one carries NMEA/RTCM down to
 * the phone (DataSPP), the other handles bidirectional command traffic and
 * carries phone→receiver RTCM uploads (CommandSPP).
 *
 * They are functionally identical class-wise — only the role tag differs, used
 * for log prefixing. Channel selection happens at the protocol layer when both
 * sockets are open against the same UUID.
 */
@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {
    @Provides
    @Singleton
    @DataSpp
    fun provideDataSpp(
        @ApplicationContext context: Context,
        log: AppLog,
    ): SppTransport = BluetoothSppTransport(context = context, role = "DataSPP", log = log)

    @Provides
    @Singleton
    @CommandSpp
    fun provideCommandSpp(
        @ApplicationContext context: Context,
        log: AppLog,
    ): SppTransport = BluetoothSppTransport(context = context, role = "CommandSPP", log = log)
}
