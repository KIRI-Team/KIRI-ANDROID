package com.kiri.feature.onboarding.model

import com.kiri.domain.model.invite.Invite

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동, 토스트 등)
sealed interface OnboardingUiEffect {
    data object NavigateToMainFlow : OnboardingUiEffect

    // 시스템 공유 시트로 초대 링크를 공유한다
    data class ShareInvite(val invite: Invite) : OnboardingUiEffect

    // 초대 코드를 발급하지 못했다
    data object ShowInviteError : OnboardingUiEffect
}
