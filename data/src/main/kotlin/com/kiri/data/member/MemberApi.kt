package com.kiri.data.member

import com.kiri.data.network.ApiResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.PATCH

internal interface MemberApi {
    // 로그인 토큰이 필요하다. 성공하면 result 로 저장된 닉네임이 오지만 보낸 값과 같아 읽지 않는다
    @PATCH("api/v1/members/me/nickname")
    suspend fun updateNickname(@Body request: NicknameRequest): ApiResponse<JsonElement>

    // 로그인 토큰이 필요하다. 성공하면 result 로 저장된 역할이 오지만 보낸 값과 같아 읽지 않는다
    @PATCH("api/v1/members/me/family-role")
    suspend fun updateFamilyRole(@Body request: FamilyRoleRequest): ApiResponse<JsonElement>
}

@Serializable
internal data class NicknameRequest(
    val nickname: String,
)

// familyRole: MOTHER, FATHER, DAUGHTER, SON
@Serializable
internal data class FamilyRoleRequest(
    val familyRole: String,
)
