package com.kiri.data.auth

import com.kiri.data.network.ApiResponse
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// 토큰 재발급 전용 API. OkHttp Authenticator 안에서 바로 결과가 필요해서 suspend 대신 Call 로 부른다
internal interface ReissueApi {
    @NoAuth
    @POST("api/v1/auth/reissue")
    fun reissue(@Body request: ReissueRequest): Call<ApiResponse<ReissueResponse>>
}

@Serializable
internal data class ReissueRequest(
    val refreshToken: String,
)

// 재발급할 때마다 refreshToken 도 새로 온다. 함께 오는 termsAgreed 는 요청을 다시 보내는 데 필요 없어 읽지 않는다
@Serializable
internal data class ReissueResponse(
    val accessToken: String,
    val refreshToken: String,
)
