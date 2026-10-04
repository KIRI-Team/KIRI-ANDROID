package com.kiri.feature.onboarding.model

import com.kiri.feature.onboarding.OnboardingStep

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.INPUT_NICKNAME,
    val nickname: String = "",
    val selectedRole: FamilyRole? = null,
)
