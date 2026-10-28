package com.kiri.domain.repository

import com.kiri.domain.model.auth.LoginResult
import com.kiri.domain.model.auth.SocialProvider
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    // 저장된 로그인 토큰이 있는지. 토큰 재발급이 거절되면 false 가 된다
    val isLoggedIn: Flow<Boolean>

    // 소셜 SDK 가 준 ID 토큰과, 그 토큰을 받을 때 쓴 nonce 로 서버에 로그인한다
    // 성공하면 서버가 준 액세스·리프레시 토큰을 저장한다
    suspend fun login(provider: SocialProvider, socialToken: String, nonce: String): Result<LoginResult>

    // 서버에 로그아웃을 알리고, 결과와 상관없이 저장된 토큰을 지운다
    // 서버 요청이 실패하면 그 실패를 돌려주지만 이 기기에서는 이미 로그아웃된 상태다
    suspend fun logout(): Result<Unit>
}
