package com.kiri.data.review

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiCommand
import com.kiri.data.network.apiResult
import com.kiri.domain.model.review.AlreadyReviewedException
import com.kiri.domain.model.review.InvalidReviewException
import com.kiri.domain.model.review.NotScheduleParticipantException
import com.kiri.domain.model.review.ReviewAccessDeniedException
import com.kiri.domain.model.review.ScheduleReview
import com.kiri.domain.model.review.ScheduleReviewForm
import com.kiri.domain.model.review.ScheduleReviewResult
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.repository.ScheduleReviewRepository
import javax.inject.Inject
import kotlinx.serialization.json.Json

internal class ScheduleReviewRepositoryImpl @Inject constructor(
    private val api: ScheduleReviewApi,
    private val json: Json,
) : ScheduleReviewRepository {

    override suspend fun writeReview(scheduleId: Long, form: ScheduleReviewForm): Result<ScheduleReviewResult> =
        apiResult(json) { api.writeReview(scheduleId, form.toRequest()) }
            .map { ScheduleReviewResult(it.reviewId, it.scheduleId) }
            .recoverCatching { error ->
                val api = error as? KiriApiException
                throw when (api?.code) {
                    NOT_PARTICIPANT -> NotScheduleParticipantException()
                    ALREADY_REVIEWED -> AlreadyReviewedException()
                    SCHEDULE_NOT_FOUND -> ScheduleNotFoundException()
                    INVALID_INPUT -> InvalidReviewException(api.message)
                    else -> error
                }
            }

    override suspend fun updateReview(scheduleId: Long, reviewId: Long, form: ScheduleReviewForm): Result<ScheduleReviewResult> =
        apiResult(json) { api.updateReview(scheduleId, reviewId, form.toRequest()) }
            .map { ScheduleReviewResult(it.reviewId, it.scheduleId) }
            .recoverMyReviewErrors()

    // 성공하면 result 가 null 이라 결과 없이 성공으로 본다
    override suspend fun deleteReview(scheduleId: Long, reviewId: Long): Result<Unit> =
        apiCommand(json) { api.deleteReview(scheduleId, reviewId) }.recoverMyReviewErrors()

    override suspend fun changeRepresentativePhoto(scheduleId: Long, reviewPhotoId: Long): Result<Long> =
        apiResult(json) { api.changeRepresentativePhoto(scheduleId, RepresentativePhotoRequest(reviewPhotoId)) }
            .map { it.representativePhotoId }
            .recoverCatching { error ->
                throw when ((error as? KiriApiException)?.code) {
                    PHOTO_NOT_FOUND -> com.kiri.domain.model.review.ReviewPhotoNotFoundException()
                    ACCESS_DENIED -> ReviewAccessDeniedException()
                    SCHEDULE_NOT_FOUND -> ScheduleNotFoundException()
                    else -> error
                }
            }

    // 수정·삭제가 함께 쓰는 실패 변환
    private fun <T> Result<T>.recoverMyReviewErrors(): Result<T> = recoverCatching { error ->
        val api = error as? KiriApiException
        throw when {
            api?.code == REVIEW_NOT_FOUND -> com.kiri.domain.model.review.ReviewNotFoundException()
            // 다른 가족의 리뷰(REVIEW403_2)와 본인 리뷰가 아닌 경우(같은 403) 모두 권한 없음으로 본다
            api?.code?.startsWith("REVIEW403") == true -> ReviewAccessDeniedException()
            api?.code == INVALID_INPUT -> InvalidReviewException(api.message)
            else -> error
        }
    }

    override suspend fun getReviews(scheduleId: Long): Result<List<ScheduleReview>> =
        getReviewList(scheduleId).map { it.reviews }

    override suspend fun getReviewList(scheduleId: Long): Result<com.kiri.domain.model.review.ScheduleReviewList> =
        apiResult(json) { api.getReviews(scheduleId) }
            .map { response ->
                com.kiri.domain.model.review.ScheduleReviewList(response.reviews.map { it.toScheduleReview() }, response.representativePhotoId)
            }
            .recoverCatching { error ->
                throw when ((error as? KiriApiException)?.code) {
                    ACCESS_DENIED -> ReviewAccessDeniedException()
                    SCHEDULE_NOT_FOUND -> ScheduleNotFoundException()
                    else -> error
                }
            }

    private companion object {
        const val ACCESS_DENIED = "REVIEW403_2"
        const val NOT_PARTICIPANT = "REVIEW403_1"
        const val ALREADY_REVIEWED = "REVIEW409_1"
        // 명세에는 회원 없음(MEMBER404_1) 예시만 있다. 일정 없음은 다른 일정 API 와 같은 코드로 본다
        const val SCHEDULE_NOT_FOUND = "SCHEDULE404_2"
        const val INVALID_INPUT = "COMMON400_1"
        const val REVIEW_NOT_FOUND = "REVIEW404_1"
        const val PHOTO_NOT_FOUND = "REVIEW404_2"
    }
}
