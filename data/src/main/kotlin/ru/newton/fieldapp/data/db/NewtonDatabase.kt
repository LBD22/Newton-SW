package ru.newton.fieldapp.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import ru.newton.fieldapp.data.db.dao.CommandQueueDao
import ru.newton.fieldapp.data.db.dao.LayerDao
import ru.newton.fieldapp.data.db.dao.NtripProfileDao
import ru.newton.fieldapp.data.db.dao.PendingPatchDao
import ru.newton.fieldapp.data.db.dao.PointDao
import ru.newton.fieldapp.data.db.dao.ProjectDao
import ru.newton.fieldapp.data.db.dao.StakeoutResultDao
import ru.newton.fieldapp.data.db.dao.TrackDao
import ru.newton.fieldapp.data.db.entity.CommandQueueEntity
import ru.newton.fieldapp.data.db.entity.LayerEntity
import ru.newton.fieldapp.data.db.entity.NtripProfileEntity
import ru.newton.fieldapp.data.db.entity.ObservationEntity
import ru.newton.fieldapp.data.db.entity.PendingPatchEntity
import ru.newton.fieldapp.data.db.entity.PointEntity
import ru.newton.fieldapp.data.db.entity.ProjectEntity
import ru.newton.fieldapp.data.db.entity.StakeoutResultEntity
import ru.newton.fieldapp.data.db.entity.TrackPointEntity
import ru.newton.fieldapp.data.db.entity.TrackSessionEntity

/**
 * Room database for Newton Field App.
 */
@Database(
    entities = [
        ProjectEntity::class,
        PointEntity::class,
        NtripProfileEntity::class,
        TrackSessionEntity::class,
        TrackPointEntity::class,
        StakeoutResultEntity::class,
        PendingPatchEntity::class,
        CommandQueueEntity::class,
        LayerEntity::class,
        ObservationEntity::class,
    ],
    version = 11,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        // v11: add the `observations` table (new table → fully auto-migratable).
        AutoMigration(from = 10, to = 11),
    ],
)
abstract class NewtonDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun pointDao(): PointDao
    abstract fun ntripProfileDao(): NtripProfileDao
    abstract fun trackDao(): TrackDao
    abstract fun stakeoutResultDao(): StakeoutResultDao
    abstract fun pendingPatchDao(): PendingPatchDao
    abstract fun commandQueueDao(): CommandQueueDao
    abstract fun layerDao(): LayerDao
}
