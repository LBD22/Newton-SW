package ru.newton.fieldapp.core.logging.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.newton.fieldapp.core.logging.AppLog
import ru.newton.fieldapp.core.logging.AppLogImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {
    @Binds
    @Singleton
    abstract fun bindAppLog(impl: AppLogImpl): AppLog
}
