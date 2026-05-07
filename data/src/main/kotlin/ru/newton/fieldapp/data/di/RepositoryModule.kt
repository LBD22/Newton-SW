package ru.newton.fieldapp.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.newton.fieldapp.data.receiver.PendingPatchStore
import ru.newton.fieldapp.data.receiver.RoomPendingPatchStore
import ru.newton.fieldapp.data.repository.LayerRepositoryImpl
import ru.newton.fieldapp.data.repository.NtripProfileRepositoryImpl
import ru.newton.fieldapp.data.repository.PointRepositoryImpl
import ru.newton.fieldapp.data.repository.ProjectRepositoryImpl
import ru.newton.fieldapp.data.repository.StakeoutResultRepositoryImpl
import ru.newton.fieldapp.data.repository.TrackRepositoryImpl
import ru.newton.fieldapp.domain.repository.LayerRepository
import ru.newton.fieldapp.domain.repository.PointRepository
import ru.newton.fieldapp.domain.repository.ProjectRepository
import ru.newton.fieldapp.domain.repository.StakeoutResultRepository
import ru.newton.fieldapp.domain.repository.TrackRepository
import ru.newton.fieldapp.gnss.ntrip.NtripProfileRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    abstract fun bindPointRepository(impl: PointRepositoryImpl): PointRepository

    @Binds
    abstract fun bindNtripProfileRepository(impl: NtripProfileRepositoryImpl): NtripProfileRepository

    @Binds
    abstract fun bindTrackRepository(impl: TrackRepositoryImpl): TrackRepository

    @Binds
    abstract fun bindStakeoutResultRepository(impl: StakeoutResultRepositoryImpl): StakeoutResultRepository

    @Binds
    abstract fun bindPendingPatchStore(impl: RoomPendingPatchStore): PendingPatchStore

    @Binds
    abstract fun bindLayerRepository(impl: LayerRepositoryImpl): LayerRepository
}
