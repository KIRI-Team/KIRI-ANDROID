package com.kiri.domain.repository

import com.kiri.domain.model.review.ScheduleReview
import com.kiri.domain.model.review.ScheduleReviewForm
import com.kiri.domain.model.review.ScheduleReviewResult

interface ScheduleReviewRepository {
    // 일정에 리뷰를 남긴다. 성공하면 서버가 그 일정을 완료(COMPLETED)로 바꾼다
    // 참여자가 아니면 NotScheduleParticipantException, 이미 썼으면 AlreadyReviewedException,
    // 없는 일정이면 ScheduleNotFoundException, 입력이 잘못되면 InvalidReviewException 으로 실패한다
    suspend fun writeReview(scheduleId: Long, form: ScheduleReviewForm): Result<ScheduleReviewResult>

    // 일정 참여자들이 쓴 리뷰를 작성순으로 모두 불러온다 (페이지 없음). 리뷰가 없으면 빈 목록이다
    // 내 가족의 일정이 아니면(가족 공간 없음 포함) ReviewAccessDeniedException, 없는 일정이면 ScheduleNotFoundException 으로 실패한다
    suspend fun getReviews(scheduleId: Long): Result<List<ScheduleReview>>
}
