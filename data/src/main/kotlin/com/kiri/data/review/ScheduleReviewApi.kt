package com.kiri.data.review

import com.kiri.data.network.ApiResponse
import com.kiri.domain.model.member.FamilyRole
import com.kiri.domain.model.review.ReviewWriter
import com.kiri.domain.model.review.ScheduleReview
import com.kiri.domain.model.review.ScheduleReviewForm
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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

    // 로그인 토큰이 필요하다. 리뷰 작성과 같은 경로이며, 작성순 전체를 돌려준다 (리뷰가 없으면 빈 배열)
    // 내 가족의 일정이 아니면(가족 공간 없음 포함) REVIEW403_2 로 거절된다
    // 로그인 토큰이 필요하다. 본인이 쓴 리뷰를 전체 교체한다 (REVIEW200_3). 요청 형식·검증은 작성과 같다
    // 리뷰 없음 REVIEW404_1, 다른 가족·본인 리뷰 아님 403, 입력·이미지 오류 COMMON400_1
    @PUT("api/v1/schedules/{scheduleId}/reviews/{reviewId}")
    suspend fun updateReview(
        @Path("scheduleId") scheduleId: Long,
        @Path("reviewId") reviewId: Long,
        @Body request: WriteReviewRequest,
    ): ApiResponse<WriteReviewResponse>

    // 로그인 토큰이 필요하다. 본인이 쓴 리뷰와 사진을 지운다 (REVIEW200_4, result 는 null)
    // 리뷰 없음 REVIEW404_1, 다른 가족·본인 리뷰 아님 403
    @DELETE("api/v1/schedules/{scheduleId}/reviews/{reviewId}")
    suspend fun deleteReview(
        @Path("scheduleId") scheduleId: Long,
        @Path("reviewId") reviewId: Long,
    ): ApiResponse<kotlinx.serialization.json.JsonElement>

    @GET("api/v1/schedules/{scheduleId}/reviews")
    suspend fun getReviews(@Path("scheduleId") scheduleId: Long): ApiResponse<ReviewListResponse>
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

@Serializable
internal data class ReviewListResponse(
    val scheduleId: Long,
    val reviews: List<ReviewResponse> = emptyList(),
)

// createdAt 은 "2026-06-13T14:50:00" 처럼 온다. familyRole 은 앱이 모르는 값이 와도 읽을 수 있도록 문자열로 받는다
@Serializable
internal data class ReviewResponse(
    val reviewId: Long,
    val writer: ReviewWriterResponse,
    val content: String = "",
    val starRating: Int = 0,
    val isRejoinIntended: Boolean = false,
    // 표시 순서대로 온다. imageUrl 은 서명 URL, imageKey 는 수정할 때 유지할 사진으로 그대로 보내는 값이다
    val photos: List<ReviewPhotoResponse> = emptyList(),
    val createdAt: String? = null,
    val isMine: Boolean = false,
)

@Serializable
internal data class ReviewPhotoResponse(
    val reviewPhotoId: Long? = null,
    val imageKey: String? = null,
    val imageUrl: String? = null,
)

@Serializable
internal data class ReviewWriterResponse(
    val memberId: Long,
    val nickname: String,
    val familyRole: String? = null,
)

internal fun ReviewResponse.toScheduleReview() = ScheduleReview(
    id = reviewId,
    writer = ReviewWriter(
        memberId = writer.memberId,
        nickname = writer.nickname,
        familyRole = FamilyRole.entries.firstOrNull { it.name == writer.familyRole },
    ),
    content = content,
    starRating = starRating,
    rejoinIntended = isRejoinIntended,
    // 주소가 없는 사진은 그릴 수 없으니 뺀다
    imageUrls = photos.mapNotNull { it.imageUrl?.takeIf(String::isNotBlank) },
    createdAt = createdAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() },
    isMine = isMine,
    // 주소와 Key 가 모두 있는 사진만 수정 때 그대로 유지할 수 있다
    photoKeys = photos.mapNotNull { photo ->
        val url = photo.imageUrl?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        val key = photo.imageKey?.takeIf(String::isNotBlank) ?: return@mapNotNull null
        url to key
    }.toMap(),
)
