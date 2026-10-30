package com.kiri.data.family

import com.kiri.data.network.ApiResponse
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

internal interface FamilyApi {
    // 로그인 토큰이 필요하다. 성공하면 201 과 함께 초대 코드·링크가 온다
    @POST("api/v1/families")
    suspend fun createFamily(@Body request: CreateFamilyRequest): ApiResponse<CreateFamilyResponse>
}

@Serializable
internal data class CreateFamilyRequest(
    val name: String,
)

// inviteExpiresAt 은 "2026-10-14T11:51:36" 처럼 시간대 없는 날짜·시간으로 온다
@Serializable
internal data class CreateFamilyResponse(
    val familyId: Long,
    val name: String,
    val inviteCode: String,
    val inviteUrl: String,
    val inviteExpiresAt: String? = null,
)
