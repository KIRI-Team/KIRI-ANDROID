package com.kiri.feature.onboarding.model

import com.kiri.domain.model.invite.Invite

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동, 토스트 등)
sealed interface OnboardingUiEffect {
    data object NavigateToMainFlow : OnboardingUiEffect

    // 시스템 공유 시트로 초대 링크를 공유한다
    data class ShareInvite(val invite: Invite) : OnboardingUiEffect

    // 초대 코드를 발급하지 못했다
    data object ShowInviteError : OnboardingUiEffect

    // 중복 외의 이유로 닉네임을 저장하지 못했다 (네트워크, 서버 오류 등)
    data object ShowNicknameError : OnboardingUiEffect

    // 가족 역할을 저장하지 못했다
    data object ShowRoleError : OnboardingUiEffect

    // 이름 중복 외의 이유로 가족 공간을 만들지 못했다 (네트워크, 서버 오류 등)
    data object ShowCreateFamilyError : OnboardingUiEffect

    // 가족코드로 가족에 참여하지 못했다 (안내 문구를 함께 보낸다)
    data class ShowJoinFamilyError(val message: String) : OnboardingUiEffect

    // 토스트로 알린다 (초대 링크로 참여한 결과 등)
    data class ShowMessage(val message: String) : OnboardingUiEffect
}
