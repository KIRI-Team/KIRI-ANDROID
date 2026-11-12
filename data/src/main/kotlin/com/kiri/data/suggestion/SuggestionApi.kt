package com.kiri.data.suggestion

import com.kiri.data.network.ApiResponse
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

internal interface SuggestionApi {
    // 로그인 토큰이 필요하다. 내용이 없거나 500자를 넘으면 COMMON400_1, 회원이 없으면 MEMBER404_1 로 거절된다
    // 명세의 경로는 suggestions 이고, 다른 API 처럼 api/v1 아래에 둔다
    @POST("api/v1/suggestions")
    suspend fun send(@Body request: SuggestionRequest): ApiResponse<SuggestionResponse>
}

// imageKey 는 이미지 업로드로 받은 키. 사진이 없으면 null 이고 요청에서 빠진다
@Serializable
internal data class SuggestionRequest(
    val content: String,
    val imageKey: String? = null,
)

@Serializable
internal data class SuggestionResponse(
    val suggestionId: Long,
)
