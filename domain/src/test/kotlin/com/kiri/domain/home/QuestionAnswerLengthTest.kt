package com.kiri.domain.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionAnswerLengthTest {
    @Test
    fun emojiAndCombiningMarksCountAsVisibleCharacters() {
        val answer = "가".repeat(498) + "👩🏽‍💻" + "e\u0301"
        assertTrue(answer.hasValidQuestionAnswerLength())
        assertFalse((answer + "🇰🇷").hasValidQuestionAnswerLength())
        assertEquals(answer, (answer + "🇰🇷").limitQuestionAnswerLength())
    }
}
