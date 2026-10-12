package com.kiri.data.home

import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.HomeTutorialRepository
import com.kiri.domain.mission.MissionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object HomeRepositoryModule {
    @Provides
    fun provideHomeTutorialRepository(local: LocalHomeTutorialRepository): HomeTutorialRepository = local

    @Provides
    @Singleton
    fun provideHomeRepository(missions: MissionRepository): HomeRepository = FakeHomeRepository(missionRepository = missions)

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
