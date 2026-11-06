package com.kiri.domain.repository

import com.kiri.domain.model.review.ScheduleReviewForm
import com.kiri.domain.model.review.ScheduleReviewResult

interface ScheduleReviewRepository {
    // 일정에 리뷰를 남긴다. 성공하면 서버가 그 일정을 완료(COMPLETED)로 바꾼다
    // 참여자가 아니면 NotScheduleParticipantException, 이미 썼으면 AlreadyReviewedException,
    // 없는 일정이면 ScheduleNotFoundException, 입력이 잘못되면 InvalidReviewException 으로 실패한다
    suspend fun writeReview(scheduleId: Long, form: ScheduleReviewForm): Result<ScheduleReviewResult>
}
