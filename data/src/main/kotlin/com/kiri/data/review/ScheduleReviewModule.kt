package com.kiri.data.review

import com.kiri.domain.repository.ScheduleReviewRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
internal object ScheduleReviewApiModule {
    @Provides
    @Singleton
    fun provideScheduleReviewApi(retrofit: Retrofit): ScheduleReviewApi = retrofit.create(ScheduleReviewApi::class.java)
}

// UI 테스트에서 실제 서버 대신 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module
@InstallIn(SingletonComponent::class)
abstract class ScheduleReviewRepositoryModule {
    @Binds
    internal abstract fun bindScheduleReviewRepository(impl: ScheduleReviewRepositoryImpl): ScheduleReviewRepository
}
