package com.kiri.data.term

import com.kiri.data.network.ApiResponse
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// 두 API 모두 로그인 토큰이 필요하다
internal interface TermApi {
    @GET("api/v1/terms")
    suspend fun getTerms(): ApiResponse<List<TermResponse>>

    // 성공하면 result 는 null 이다
    @POST("api/v1/terms/agreements")
    suspend fun agree(@Body request: TermAgreeRequest): ApiResponse<Unit>
}

// shortDescription, updatedAt 도 오지만 동의 화면에서 쓰지 않아 읽지 않는다
@Serializable
internal data class TermResponse(
    val termId: Long,
    val title: String,
    val description: String,
    val isRequired: Boolean,
)

@Serializable
internal data class TermAgreeRequest(
    val agreements: List<TermAgreement>,
)

@Serializable
internal data class TermAgreement(
    val termId: Long,
    val isAgreed: Boolean,
)
