package com.kiri.data.detail

import com.kiri.data.local.LocalStateStore
import com.kiri.domain.detail.DetailRepository
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.schedule.ScheduleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DetailRepositoryModule {
    @Provides
    @Singleton
    fun details(
        home: HomeRepository,
        missions: MissionRepository,
        schedules: ScheduleRepository,
        clock: Clock,
        store: LocalStateStore,
    ): DetailRepository = LocalDetailRepository(home, missions, schedules, clock, store)
}
