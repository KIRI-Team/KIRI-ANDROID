package com.kiri.app.record

import com.kiri.data.nearby.ScheduleNearbyRepositoryModule
import com.kiri.domain.detail.NearbyPlace
import com.kiri.domain.repository.ScheduleNearbyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// UI 테스트에서는 실제 서버 대신 근처 놀거리가 없는 것으로 본다
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ScheduleNearbyRepositoryModule::class])
object FakeScheduleNearbyModule {
    @Provides
    fun provideScheduleNearbyRepository(): ScheduleNearbyRepository = object : ScheduleNearbyRepository {
        override suspend fun getNearby(scheduleId: Long): Result<List<NearbyPlace>> = Result.success(emptyList())
    }
}
