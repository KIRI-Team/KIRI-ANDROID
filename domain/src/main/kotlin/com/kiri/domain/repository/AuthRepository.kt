package com.kiri.domain.repository

import com.kiri.domain.model.auth.LoginResult
import com.kiri.domain.model.auth.SocialProvider

interface AuthRepository {
    // 소셜 SDK 가 준 ID 토큰과, 그 토큰을 받을 때 쓴 nonce 로 서버에 로그인한다
    // 성공하면 서버가 준 액세스·리프레시 토큰을 저장한다
    suspend fun login(provider: SocialProvider, socialToken: String, nonce: String): Result<LoginResult>
}
