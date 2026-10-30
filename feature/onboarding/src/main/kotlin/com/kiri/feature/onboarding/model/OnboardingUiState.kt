package com.kiri.feature.onboarding.model

import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.feature.onboarding.OnboardingStep

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.INPUT_NICKNAME,
    val nickname: String = "",
    // 다음 버튼을 눌렀을 때 지키지 못한 규칙 (입력을 바꾸면 비운다)
    val nicknameViolations: Set<NicknameRule> = emptySet(),
    // 서버가 이미 사용 중인 닉네임이라고 답했다 (입력을 바꾸면 비운다)
    val isNicknameDuplicated: Boolean = false,
    // 닉네임이나 역할을 서버에 저장하는 중
    val isSubmitting: Boolean = false,
    val selectedRole: FamilyRole? = null,
    val familyName: String = "",
    val familyNameViolations: Set<NicknameRule> = emptySet(),
    // 서버가 이미 존재하는 가족 이름이라고 답했다 (입력을 바꾸면 비운다)
    val isFamilyNameDuplicated: Boolean = false,
    // 코드 생성 로딩 단계에서 가족 공간을 만들며 발급된 초대 코드
    val familyCode: String = "",
    // 가족코드 입력 화면에서 사용자가 입력한 코드
    val inputFamilyCode: String = "",
)
