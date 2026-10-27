package com.kiri.data.mission

import com.kiri.domain.mission.MissionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MissionRepositoryModule {
    @Provides @Singleton fun missions(): MissionRepository = UnavailableMissionRepository()
}
