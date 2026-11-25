package com.kiri.data.push.remote

import com.kiri.data.common.network.ApiResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Query

internal interface DeviceApi {
    // 로그인 토큰이 필요하다. 같은 deviceIdentifier 가 있으면 서버가 토큰을 바꾼다. 성공하면 result 는 null 이다
    @POST("api/v1/members/devices")
    suspend fun register(@Body request: DeviceRegisterRequest): ApiResponse<JsonElement>

    // 로그인 토큰이 필요하다. 등록된 기기가 없으면 MEMBER_DEVICE404_1 이다. 성공하면 result 는 null 이다
    @DELETE("api/v1/members/devices")
    suspend fun delete(@Query("deviceIdentifier") deviceIdentifier: String): ApiResponse<JsonElement>
}

// deviceType: ANDROID 또는 IOS, deviceIdentifier: 앱이 만든 UUID (DeviceIdStore)
@Serializable
internal data class DeviceRegisterRequest(
    val fcmToken: String,
    val deviceType: String,
    val deviceIdentifier: String,
)
