package ru.newton.fieldapp.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import ru.newton.fieldapp.data.db.NewtonDatabase
import ru.newton.fieldapp.data.db.dao.CommandQueueDao
import ru.newton.fieldapp.data.db.dao.NtripProfileDao
import ru.newton.fieldapp.data.db.dao.PendingPatchDao
import ru.newton.fieldapp.data.db.dao.PointDao
import ru.newton.fieldapp.data.db.dao.ProjectDao
import ru.newton.fieldapp.data.db.dao.StakeoutResultDao
import ru.newton.fieldapp.data.db.dao.TrackDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): NewtonDatabase =
        Room.databaseBuilder(context, NewtonDatabase::class.java, "newton.db")
            // Migrations are declared in @Database(autoMigrations = [...]).
            // Add manual migrations to the builder here when the change is destructive.
            .build()

    @Provides
    fun provideProjectDao(db: NewtonDatabase): ProjectDao = db.projectDao()

    @Provides
    fun providePointDao(db: NewtonDatabase): PointDao = db.pointDao()

    @Provides
    fun provideNtripProfileDao(db: NewtonDatabase): NtripProfileDao = db.ntripProfileDao()

    @Provides
    fun provideTrackDao(db: NewtonDatabase): TrackDao = db.trackDao()

    @Provides
    fun provideStakeoutResultDao(db: NewtonDatabase): StakeoutResultDao = db.stakeoutResultDao()

    @Provides
    fun providePendingPatchDao(db: NewtonDatabase): PendingPatchDao = db.pendingPatchDao()

    @Provides
    fun provideCommandQueueDao(db: NewtonDatabase): CommandQueueDao = db.commandQueueDao()

    @Provides
    fun provideLayerDao(db: NewtonDatabase): ru.newton.fieldapp.data.db.dao.LayerDao = db.layerDao()
}
