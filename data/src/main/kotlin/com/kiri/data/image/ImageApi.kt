package com.kiri.data.image

import com.kiri.data.network.ApiResponse
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

internal interface ImageApi {
    // 로그인 토큰이 필요하다. 형식(image/jpeg, image/png, image/webp)이나 크기(최대 10MB)가 맞지 않으면 COMMON400_1,
    // 이미지 저장소 오류면 IMAGE500_1 로 거절된다. 발급된 주소는 5분간 유효한 PUT 용 주소다
    // 명세의 경로는 images/presigned-url 이고, 다른 API 처럼 api/v1 아래에 둔다
    @POST("api/v1/images/presigned-url")
    suspend fun issuePresignedUrl(@Body request: PresignedUrlRequest): ApiResponse<PresignedUrlResponse>
}

@Serializable
internal data class PresignedUrlRequest(val contentType: String, val contentLength: Long)

// expiresAt 은 "2026-10-29T14:35:00" 처럼 온다. 받은 즉시 올리므로 지금은 쓰지 않는다
@Serializable
internal data class PresignedUrlResponse(
    val presignedUrl: String,
    val imageKey: String,
    val expiresAt: String? = null,
)
