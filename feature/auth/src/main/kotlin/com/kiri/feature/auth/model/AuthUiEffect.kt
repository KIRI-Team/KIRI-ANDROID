package com.kiri.feature.auth.model

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동 등)
sealed interface AuthUiEffect {
    data object NavigateToOnboarding : AuthUiEffect
}
