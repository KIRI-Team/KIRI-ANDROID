package com.kiri.data.review

import com.kiri.data.network.ApiResponse
import com.kiri.domain.model.review.ScheduleReviewForm
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

internal interface ScheduleReviewApi {
    // 로그인 토큰이 필요하다. 참여자가 아니면 REVIEW403_1, 이미 썼으면 REVIEW409_1,
    // 입력이 잘못되면 COMMON400_1 로 거절된다. 성공하면 일정이 COMPLETED 가 된다
    // 명세의 경로는 schedules/{scheduleId}/reviews 이고, 다른 일정 API 처럼 api/v1 아래에 둔다
    @POST("api/v1/schedules/{scheduleId}/reviews")
    suspend fun writeReview(
        @Path("scheduleId") scheduleId: Long,
        @Body request: WriteReviewRequest,
    ): ApiResponse<WriteReviewResponse>
}

@Serializable
internal data class WriteReviewRequest(
    val starRating: Int,
    val imageKeys: List<String>,
    val content: String,
    val isRejoinIntended: Boolean,
)

@Serializable
internal data class WriteReviewResponse(val reviewId: Long, val scheduleId: Long)

internal fun ScheduleReviewForm.toRequest() = WriteReviewRequest(
    starRating = starRating,
    imageKeys = imageKeys,
    content = content.trim(),
    isRejoinIntended = rejoinIntended,
)
