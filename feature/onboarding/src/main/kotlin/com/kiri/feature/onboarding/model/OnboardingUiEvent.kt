package com.kiri.feature.onboarding.model

import com.kiri.feature.onboarding.OnboardingPath

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface OnboardingUiEvent {
    data class NicknameChanged(val nickname: String) : OnboardingUiEvent
    data class RoleSelected(val role: FamilyRole) : OnboardingUiEvent
    data class PathSelected(val path: OnboardingPath) : OnboardingUiEvent
    data class FamilyNameChanged(val familyName: String) : OnboardingUiEvent
    data class InputFamilyCodeChanged(val code: String) : OnboardingUiEvent
    data object NextClicked : OnboardingUiEvent
    data object BackClicked : OnboardingUiEvent
    data object InviteClicked : OnboardingUiEvent
}
