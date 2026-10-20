package com.kiri.data.schedule

import com.kiri.data.record.LocalBucketRepository
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.record.BucketRepository
import com.kiri.domain.schedule.ScheduleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScheduleModule {
    @Provides @Singleton fun buckets(home: HomeRepository, missions: MissionRepository, clock: Clock): BucketRepository = LocalBucketRepository(home, missions, clock)
    @Provides @Singleton fun schedules(home: HomeRepository, missions: MissionRepository, buckets: BucketRepository, clock: Clock): ScheduleRepository = LocalScheduleRepository(home, missions, buckets, clock)
}
