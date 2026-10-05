package com.kiri.feature.auth.model

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동, 외부 링크 열기 등)
sealed interface AuthUiEffect {
    data object NavigateToOnboarding : AuthUiEffect
    data class OpenTermDetail(val url: String) : AuthUiEffect
}
