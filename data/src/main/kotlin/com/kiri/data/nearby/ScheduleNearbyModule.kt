package com.kiri.data.nearby

import com.kiri.domain.repository.ScheduleNearbyRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object ScheduleNearbyApiModule {
    @Provides
    @Singleton
    fun provideScheduleNearbyApi(retrofit: Retrofit): ScheduleNearbyApi = retrofit.create(ScheduleNearbyApi::class.java)
}

// UI 테스트에서 실제 서버 대신 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module
@InstallIn(SingletonComponent::class)
abstract class ScheduleNearbyRepositoryModule {
    @Binds
    internal abstract fun bindScheduleNearbyRepository(impl: ScheduleNearbyRepositoryImpl): ScheduleNearbyRepository
}
