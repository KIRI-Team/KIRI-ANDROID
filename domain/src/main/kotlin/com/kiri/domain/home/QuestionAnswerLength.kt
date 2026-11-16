package com.kiri.domain.home

const val QUESTION_ANSWER_MAX_LENGTH = 500

// An extended grapheme cluster counts as one visible character (e.g. 👩🏽‍💻).
private val visibleCharacter = Regex("\\X")

fun String.limitQuestionAnswerLength(): String {
    val firstExtra = visibleCharacter.findAll(this).drop(QUESTION_ANSWER_MAX_LENGTH).firstOrNull()
    return if (firstExtra == null) this else substring(0, firstExtra.range.first)
}

fun String.hasValidQuestionAnswerLength(): Boolean =
    visibleCharacter.findAll(this).take(QUESTION_ANSWER_MAX_LENGTH + 1).count() <= QUESTION_ANSWER_MAX_LENGTH
