package com.kiri.app.record

import com.kiri.data.schedule.CalendarScheduleRepositoryModule
import com.kiri.domain.schedule.model.CalendarSchedule
import com.kiri.domain.schedule.model.ScheduleDetail
import com.kiri.domain.schedule.model.ScheduleNotFoundException
import com.kiri.domain.schedule.repository.CalendarScheduleRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.time.YearMonth

// UI 테스트에서는 실제 서버 대신 서버 일정이 없는 것으로 본다 (캘린더는 기기에 저장된 활동만 보여주고, 상세는 없는 일정으로 본다)
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [CalendarScheduleRepositoryModule::class])
object FakeCalendarScheduleModule {
    @Provides
    fun provideCalendarScheduleRepository(): CalendarScheduleRepository = object : CalendarScheduleRepository {
        override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> =
            Result.success(emptyList())

        override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> =
            Result.failure(ScheduleNotFoundException())

        override suspend fun updateSchedule(scheduleId: Long, update: com.kiri.domain.schedule.model.ScheduleUpdate): Result<Unit> =
            Result.failure(ScheduleNotFoundException())

        // 버킷의 일정 잡기는 성공한 것으로 본다
        override suspend fun createSchedule(bucketItemId: Long, form: com.kiri.domain.schedule.model.ScheduleUpdate): Result<Long> =
            Result.success(1)
    }
}
