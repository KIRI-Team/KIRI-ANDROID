package com.kiri.app.record

import com.kiri.data.review.ScheduleReviewRepositoryModule
import com.kiri.domain.review.model.ScheduleReview
import com.kiri.domain.review.model.ScheduleReviewForm
import com.kiri.domain.review.model.ScheduleReviewResult
import com.kiri.domain.review.repository.ScheduleReviewRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// UI 테스트에서는 실제 서버 대신 리뷰가 없는 것으로 보고, 작성은 성공한 것으로 본다
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ScheduleReviewRepositoryModule::class])
object FakeScheduleReviewModule {
    @Provides
    fun provideScheduleReviewRepository(): ScheduleReviewRepository = object : ScheduleReviewRepository {
        override suspend fun writeReview(scheduleId: Long, form: ScheduleReviewForm): Result<ScheduleReviewResult> =
            Result.success(ScheduleReviewResult(1, scheduleId))

        override suspend fun getReviews(scheduleId: Long): Result<List<ScheduleReview>> = Result.success(emptyList())
    }
}
