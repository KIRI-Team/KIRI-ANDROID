package com.kiri.feature.onboarding.presentation

import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.domain.model.nickname.NicknameValidator
import com.kiri.feature.onboarding.presentation.component.OnboardingInputGuide

// 닉네임·가족명 입력 아래 안내 문구. 지키지 못한 규칙은 오류로 표시한다. 둘은 최대 길이만 다르다
private fun ruleTexts(maxLength: Int) = listOf(
    NicknameRule.ALLOWED_LETTERS to "한글/영어/숫자 혼합 가능",
    NicknameRule.LENGTH to "최소 ${NicknameValidator.MIN_LENGTH}자 ~ 최대 ${maxLength}자 이내",
    NicknameRule.NO_SPECIAL_CHARACTERS to "특수 문자 입력 불가능",
)

private fun guides(violations: Set<NicknameRule>, maxLength: Int): List<OnboardingInputGuide> =
    ruleTexts(maxLength).map { (rule, text) -> OnboardingInputGuide(text = text, isError = rule in violations) }

// 서버가 중복이라고 답했으면 규칙 안내 아래에 중복 안내를 오류로 덧붙인다
internal fun nicknameGuides(violations: Set<NicknameRule>, isDuplicated: Boolean = false): List<OnboardingInputGuide> =
    guides(violations, NicknameValidator.MAX_LENGTH) +
        listOfNotNull(OnboardingInputGuide(text = DUPLICATED_TEXT, isError = true).takeIf { isDuplicated })

internal fun familyNameGuides(violations: Set<NicknameRule>): List<OnboardingInputGuide> =
    guides(violations, NicknameValidator.FAMILY_NAME_MAX_LENGTH)

private const val DUPLICATED_TEXT = "이미 사용 중인 닉네임입니다."
