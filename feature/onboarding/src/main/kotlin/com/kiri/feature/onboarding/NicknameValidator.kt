package com.kiri.feature.onboarding

object NicknameValidator {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 10

    private val allowedCharacters = Regex("^[가-힣a-zA-Z0-9]+$")

    fun isValid(nickname: String): Boolean =
        nickname.length in MIN_LENGTH..MAX_LENGTH && allowedCharacters.matches(nickname)
}
