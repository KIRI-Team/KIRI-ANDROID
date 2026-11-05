package com.kiri.data.schedule

import com.kiri.domain.home.HomeRepository
import com.kiri.domain.repository.CalendarScheduleRepository
import com.kiri.domain.usecase.schedule.GetScheduleEditAccessUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object CalendarScheduleApiModule {
    @Provides
    @Singleton
    fun provideCalendarScheduleApi(retrofit: Retrofit): CalendarScheduleApi =
        retrofit.create(CalendarScheduleApi::class.java)

    // domain 은 DI 에 의존하지 않으므로 UseCase 는 여기서 만들어 준다
    @Provides
    fun provideGetScheduleEditAccessUseCase(
        schedules: CalendarScheduleRepository,
        home: HomeRepository,
    ): GetScheduleEditAccessUseCase = GetScheduleEditAccessUseCase(schedules, home)
}

// UI 테스트에서 실제 서버 대신 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarScheduleRepositoryModule {
    @Binds
    internal abstract fun bindCalendarScheduleRepository(
        impl: CalendarScheduleRepositoryImpl,
    ): CalendarScheduleRepository
}
