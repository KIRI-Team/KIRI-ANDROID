package com.kiri.data.term

import com.kiri.data.network.ApiResponse
import com.kiri.domain.model.term.Term
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// 두 API 모두 로그인 토큰이 필요하다
internal interface TermApi {
    @GET("api/v1/terms")
    suspend fun getTerms(): ApiResponse<List<TermResponse>>

    // 성공하면 result 는 null 이다. 나중에 값이 와도 읽기에 실패하지 않도록 JsonElement 로 받고 쓰지 않는다
    @POST("api/v1/terms/agreements")
    suspend fun agree(@Body request: TermAgreeRequest): ApiResponse<JsonElement>
}

// shortDescription 도 오지만 화면에서 쓰지 않아 읽지 않는다
// updatedAt 은 "2026-10-04T12:00:00" 처럼 시간대 없는 날짜·시간으로 온다
@Serializable
internal data class TermResponse(
    val termId: Long,
    val title: String,
    val description: String,
    val isRequired: Boolean,
    val updatedAt: String? = null,
)

// 업데이트일을 읽지 못해도 약관은 보여줄 수 있으니 업데이트일만 비운다
internal fun TermResponse.toTerm() = Term(
    id = termId,
    title = title,
    content = description,
    required = isRequired,
    updatedAt = updatedAt?.let { runCatching { LocalDateTime.parse(it).toLocalDate() }.getOrNull() },
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
