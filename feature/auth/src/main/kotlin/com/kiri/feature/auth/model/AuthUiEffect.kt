package com.kiri.feature.auth.model

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동 등)
sealed interface AuthUiEffect {
    // SDK 로그인 창을 띄운다 (Activity 가 필요해 화면에서 실행하고 결과를 SocialLoginFinished 로 돌려준다)
    data class StartSocialLogin(val provider: LoginProvider, val nonce: String) : AuthUiEffect

    data object NavigateToOnboarding : AuthUiEffect

    // 약관에 이미 동의한 기존 회원은 온보딩 없이 메인으로 간다
    data object NavigateToMain : AuthUiEffect

    // 로그인하지 못했다는 안내. cause 는 원인 확인용으로 로그에 남긴다
    data class ShowLoginError(val cause: Throwable) : AuthUiEffect
}
