package com.kiri.app.record

import com.kiri.data.schedule.UpcomingScheduleRepositoryModule
import com.kiri.domain.model.schedule.UpcomingSchedules
import com.kiri.domain.repository.UpcomingScheduleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// UI 테스트에서는 실제 서버 대신 다가오는 일정이 없는 것으로 본다
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [UpcomingScheduleRepositoryModule::class])
object FakeUpcomingScheduleModule {
    @Provides
    fun provideUpcomingScheduleRepository(): UpcomingScheduleRepository = object : UpcomingScheduleRepository {
        override suspend fun getUpcomingSchedules(): Result<UpcomingSchedules> = Result.success(UpcomingSchedules())
    }
}
