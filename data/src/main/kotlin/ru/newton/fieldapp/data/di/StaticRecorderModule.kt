package ru.newton.fieldapp.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.newton.fieldapp.data.staticobs.StaticRecorderRoot
import ru.newton.fieldapp.data.staticobs.StaticRecorderScope
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StaticRecorderModule {
    @Provides
    @Singleton
    @StaticRecorderRoot
    fun provideStaticRoot(
        @ApplicationContext context: Context,
    ): File =
        File(context.filesDir, "static")

    @Provides
    @Singleton
    @StaticRecorderScope
    fun provideStaticScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
