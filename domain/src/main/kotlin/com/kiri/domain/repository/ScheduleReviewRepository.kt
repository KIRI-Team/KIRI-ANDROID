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

    // getReviews 와 같고, 일정의 대표 사진 id 도 함께 돌려준다. 대표 사진 정보가 없는 구현은 null 로 둔다
    suspend fun getReviewList(scheduleId: Long): Result<com.kiri.domain.model.review.ScheduleReviewList> =
        getReviews(scheduleId).map { com.kiri.domain.model.review.ScheduleReviewList(it) }

    // 내가 쓴 리뷰를 수정 화면의 최종 상태로 통째로 바꾼다. imageKeys 는 표시 순서대로
    // 유지할 기존 사진 Key(user/original/...)와 새로 올린 사진 Key(pending/...)를 섞어 보낸다.
    // 리뷰가 없으면 ReviewNotFoundException, 내 리뷰가 아니거나 다른 가족이면 ReviewAccessDeniedException,
    // 입력 오류면 InvalidReviewException 으로 실패한다
    suspend fun updateReview(scheduleId: Long, reviewId: Long, form: ScheduleReviewForm): Result<ScheduleReviewResult> =
        Result.failure(UnsupportedOperationException())

    // 내가 쓴 리뷰와 그 사진을 지운다. 일정은 완료 상태로 남는다
    // 리뷰가 없으면 ReviewNotFoundException, 내 리뷰가 아니거나 다른 가족이면 ReviewAccessDeniedException 으로 실패한다
    suspend fun deleteReview(scheduleId: Long, reviewId: Long): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    // 일정의 대표 사진을 그 일정의 리뷰 사진 하나로 정하고, 정해진 대표 사진 id 를 돌려준다. 가족 모두에게 반영된다
    // (지정하지 않은 일정은 작성순으로 가장 빠른 리뷰의 첫 사진이 대표 사진이다)
    // 그 일정의 리뷰 사진이 아니면 ReviewPhotoNotFoundException, 다른 가족의 일정이면 ReviewAccessDeniedException 으로 실패한다
    suspend fun changeRepresentativePhoto(scheduleId: Long, reviewPhotoId: Long): Result<Long> =
        Result.failure(UnsupportedOperationException())
}
