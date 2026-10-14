package com.kiri.data.auth

// 서버가 준 로그인 토큰
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

// 로그인 토큰 보관소. 앱을 다시 켜도 남아 있어야 한다
interface TokenStore {
    suspend fun save(tokens: AuthTokens)

    // 저장한 토큰 (로그인하지 않았으면 null)
    suspend fun get(): AuthTokens?

    suspend fun clear()
}
