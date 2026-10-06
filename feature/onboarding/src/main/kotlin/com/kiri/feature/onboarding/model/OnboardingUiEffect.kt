package com.kiri.feature.onboarding.model

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동, 토스트 등)
sealed interface OnboardingUiEffect {
    data object NavigateToMainFlow : OnboardingUiEffect
}
