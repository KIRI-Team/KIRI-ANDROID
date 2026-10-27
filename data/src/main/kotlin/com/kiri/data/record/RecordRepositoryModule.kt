package com.kiri.data.record

import com.kiri.data.local.LocalStateStore
import com.kiri.domain.detail.DetailRepository
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.record.*
import com.kiri.domain.schedule.ScheduleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecordRepositoryModule {
    @Provides
    @Singleton
    fun records(
        home: HomeRepository,
        missions: MissionRepository,
        details: DetailRepository,
        schedules: ScheduleRepository,
        buckets: BucketRepository,
        clock: Clock,
        store: LocalStateStore,
        media: DeviceRecordMediaStore,
    ): RecordRepository =
        LocalRecordRepository(home, missions, details, schedules, buckets, clock, store, media)
}
