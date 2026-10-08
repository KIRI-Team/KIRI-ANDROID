package com.kiri.feature.onboarding.model

import com.kiri.feature.onboarding.OnboardingStep

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.INPUT_NICKNAME,
    val nickname: String = "",
    val selectedRole: FamilyRole? = null,
    val familyName: String = "",
    // 서버 연결 전에는 온보딩의 공통 임시 코드 사용
    val familyCode: String = "",
    // 가족코드 입력 화면에서 사용자가 입력한 코드
    val inputFamilyCode: String = "",
)
