package com.kiri.data.family

import com.kiri.data.network.ApiResponse
import com.kiri.domain.model.family.Family
import com.kiri.domain.model.invite.Invite
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

internal interface FamilyApi {
    @retrofit2.http.PUT("api/v1/families/{familyId}/address")
    suspend fun saveAddress(@Path("familyId") familyId: Long, @Body address: FamilyAddressRequest): ApiResponse<JsonElement>

    // 로그인 토큰이 필요하다. 내가 속한 가족 공간의 ID 를 돌려준다. 회원이 없으면 MEMBER404_1,
    // 소속된 가족이 없으면 404 로 거절된다 (가족 없음 코드는 명세에 예시가 없다)
    // 전달받은 경로는 familes/me/id 이다. 다른 가족 API 처럼 api/v1/families 아래에 둔다
    @GET("api/v1/families/me/id")
    suspend fun getMyFamilyId(): ApiResponse<FamilyIdResponse>

    // 로그인 토큰이 필요하다. 성공하면 201 과 함께 초대 코드·링크가 온다
    @POST("api/v1/families")
    suspend fun createFamily(@Body request: CreateFamilyRequest): ApiResponse<CreateFamilyResponse>

    // 로그인 토큰이 필요하다. 초대 코드로 가족에 가입한다 (FAMILY200_7)
    // 코드 누락·형식 오류 COMMON400_1, 회원 없음 MEMBER404_1, 코드에 맞는 가족 없음 FAMILY404_1,
    // 이미 다른 가족 FAMILY409_2, 코드 만료 FAMILY410_1
    // 전달받은 경로는 families/join/code 이다. 다른 가족 API 처럼 api/v1 아래에 둔다
    @POST("api/v1/families/join/code")
    suspend fun joinByCode(@Body request: JoinFamilyRequest): ApiResponse<JoinFamilyResponse>

    // 로그인 토큰이 필요하다. 그 가족의 구성원이 아니면 FAMILY403_1 로 거절된다
    @GET("api/v1/families/{familyId}/invite")
    suspend fun getInvite(@Path("familyId") familyId: Long): ApiResponse<InviteResponse>

    // 로그인 토큰이 필요하다. 지금 초대 코드·링크를 지운다 (응답 result 는 쓰지 않는다)
    @DELETE("api/v1/families/{familyId}/invite")
    suspend fun deleteInvite(@Path("familyId") familyId: Long): ApiResponse<JsonElement>

    // 로그인 토큰이 필요하고 본문은 없다. 새 초대를 만들면 이전 초대는 쓸 수 없다
    @PATCH("api/v1/families/{familyId}/invite")
    suspend fun reissueInvite(@Path("familyId") familyId: Long): ApiResponse<InviteResponse>

    // 로그인 토큰이 필요하다. 같은 이름의 가족이 있으면 FAMILY409_1, 구성원이 아니면 FAMILY403_1 로 거절된다
    @PATCH("api/v1/families/{familyId}")
    suspend fun renameFamily(
        @Path("familyId") familyId: Long,
        @Body request: RenameFamilyRequest,
    ): ApiResponse<RenameFamilyResponse>
}

@Serializable
internal data class JoinFamilyRequest(val inviteCode: String)

@Serializable
internal data class JoinFamilyResponse(val familyId: Long, val familyName: String)

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
internal data class FamilyIdResponse(val familyId: Long)

@Serializable
internal data class RenameFamilyRequest(
    val name: String,
)

@Serializable
internal data class RenameFamilyResponse(
    val familyId: Long,
    val name: String,
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

@Serializable internal data class FamilyAddressRequest(val postalCode: String, val address: String, val detailAddress: String,
    val regionCode: String, val latitude: Double, val longitude: Double)
