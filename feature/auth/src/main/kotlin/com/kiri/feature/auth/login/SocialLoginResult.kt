package com.kiri.feature.auth.login

import com.kiri.feature.auth.model.LoginProvider

// 카카오·구글 SDK 로그인 결과
sealed interface SocialLoginResult {
    // SDK가 준 ID 토큰(nonce 포함). 서버에 보내 앱 로그인을 마친다
    data class Success(val provider: LoginProvider, val token: String) : SocialLoginResult

    // 사용자가 로그인 창을 닫았다. 따로 안내하지 않는다
    data object Cancelled : SocialLoginResult

    // 로그인하지 못했다 (키 미설정, 네트워크, 계정 없음 등). 오류 안내를 보여준다
    data class Failure(val provider: LoginProvider, val cause: Throwable) : SocialLoginResult
}
