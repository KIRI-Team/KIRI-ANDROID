package com.kiri.domain.model.nickname

// 닉네임·가족명이 지켜야 하는 규칙 (온보딩, 마이페이지 닉네임 수정에서 함께 쓴다)
enum class NicknameRule {
    // 한글/영어/숫자 혼합 가능 (완성되지 않은 자모나 다른 문자는 불가)
    ALLOWED_LETTERS,

    // 최소 2자 ~ 최대 12자 이내
    LENGTH,

    // 특수 문자 입력 불가능 (공백·기호·이모지 포함)
    NO_SPECIAL_CHARACTERS,
}

object NicknameValidator {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 12

    // 가족명은 닉네임과 같은 규칙에 길이만 15자까지 허용한다
    const val FAMILY_NAME_MAX_LENGTH = 15

    private val allowedCharacters = Regex("^[가-힣a-zA-Z0-9]+$")

    fun isValid(nickname: String): Boolean = violations(nickname).isEmpty()

    fun familyNameViolations(familyName: String): Set<NicknameRule> = violations(familyName, FAMILY_NAME_MAX_LENGTH)

    fun violations(nickname: String, maxLength: Int = MAX_LENGTH): Set<NicknameRule> = buildSet {
        if (nickname.length !in MIN_LENGTH..maxLength) add(NicknameRule.LENGTH)
        if (nickname.any { !it.isLetterOrDigit() }) add(NicknameRule.NO_SPECIAL_CHARACTERS)
        // 글자/숫자이지만 허용 범위 밖인 문자 (예: ㄱ, 漢)
        if (nickname.any { it.isLetterOrDigit() && !allowedCharacters.matches(it.toString()) }) {
            add(NicknameRule.ALLOWED_LETTERS)
        }
    }
}
