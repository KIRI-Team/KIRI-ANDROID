package com.kiri.feature.onboarding.model

import com.kiri.feature.onboarding.NicknameRule
import com.kiri.feature.onboarding.OnboardingStep

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.INPUT_NICKNAME,
    val nickname: String = "",
    // 다음 버튼을 눌렀을 때 지키지 못한 규칙 (입력을 바꾸면 비운다)
    val nicknameViolations: Set<NicknameRule> = emptySet(),
    val selectedRole: FamilyRole? = null,
    val familyName: String = "",
    val familyNameViolations: Set<NicknameRule> = emptySet(),
    // TODO: 코드 생성 로직이 추가되면 생성된 코드로 채우기
    val familyCode: String = "",
    // 가족코드 입력 화면에서 사용자가 입력한 코드
    val inputFamilyCode: String = "",
)
