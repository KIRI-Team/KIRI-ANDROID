package com.kiri.feature.onboarding.presentation

import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.feature.onboarding.presentation.component.OnboardingInputGuide

// 닉네임·가족명 입력 아래 안내 문구. 지키지 못한 규칙은 오류로 표시한다
private val nicknameRuleTexts = listOf(
    NicknameRule.ALLOWED_LETTERS to "한글/영어/숫자 혼합 가능",
    NicknameRule.LENGTH to "최소 2자 ~ 최대 10자 이내",
    NicknameRule.NO_SPECIAL_CHARACTERS to "특수 문자 입력 불가능",
)

internal fun nicknameGuides(violations: Set<NicknameRule>): List<OnboardingInputGuide> =
    nicknameRuleTexts.map { (rule, text) -> OnboardingInputGuide(text = text, isError = rule in violations) }
