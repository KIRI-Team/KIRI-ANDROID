package com.kiri.data.family

import com.kiri.data.network.ApiResponse
import com.kiri.domain.model.family.Family
import com.kiri.domain.model.invite.Invite
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

internal interface FamilyApi {
    // 로그인 토큰이 필요하다. 성공하면 201 과 함께 초대 코드·링크가 온다
    @POST("api/v1/families")
    suspend fun createFamily(@Body request: CreateFamilyRequest): ApiResponse<CreateFamilyResponse>

    // 로그인 토큰이 필요하다. 그 가족의 구성원이 아니면 FAMILY403_1 로 거절된다
    @GET("api/v1/families/{familyId}/invite")
    suspend fun getInvite(@Path("familyId") familyId: Long): ApiResponse<InviteResponse>

    // 로그인 토큰이 필요하고 본문은 없다. 새 초대를 만들면 이전 초대는 쓸 수 없다
    @PATCH("api/v1/families/{familyId}/invite")
    suspend fun reissueInvite(@Path("familyId") familyId: Long): ApiResponse<InviteResponse>
}

@Serializable
internal data class InviteResponse(
    val inviteCode: String,
    val inviteUrl: String,
    val inviteExpiresAt: String? = null,
)

internal fun InviteResponse.toInvite() = invite(inviteCode, inviteUrl, inviteExpiresAt)

internal fun CreateFamilyResponse.toFamily() = Family(id = familyId, name = name, invite = invite(inviteCode, inviteUrl, inviteExpiresAt))

// 만료일을 읽지 못해도 초대는 쓸 수 있으니 만료일만 비운다
private fun invite(code: String, url: String, expiresAt: String?) = Invite(
    code = code,
    url = url,
    expiresAt = expiresAt?.let { runCatching { LocalDateTime.parse(it).toLocalDate() }.getOrNull() },
)

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
