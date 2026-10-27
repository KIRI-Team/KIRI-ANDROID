package com.kiri.data.auth

import com.kiri.data.network.ApiResponse
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

internal interface AuthApi {
    @NoAuth
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginResponse>
}

// socialProvider: KAKAO 또는 GOOGLE, socialProviderToken: SDK 가 준 ID 토큰
@Serializable
internal data class LoginRequest(
    val socialProvider: String,
    val socialProviderToken: String,
    val nonce: String,
)

@Serializable
internal data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean,
    val termsAgreed: Boolean,
)
