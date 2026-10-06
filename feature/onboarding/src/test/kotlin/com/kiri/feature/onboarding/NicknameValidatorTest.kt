package com.kiri.feature.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NicknameValidatorTest {

    @Test
    fun 한글_영어_숫자를_혼합한_닉네임은_유효하다() {
        assertTrue(NicknameValidator.isValid("주연주"))
        assertTrue(NicknameValidator.isValid("kiri12"))
        assertTrue(NicknameValidator.isValid("끼리Kiri7"))
    }

    @Test
    fun 길이가_2자_이상_10자_이하이면_유효하다() {
        assertTrue(NicknameValidator.isValid("ab"))
        assertTrue(NicknameValidator.isValid("가".repeat(10)))
    }

    @Test
    fun 길이가_2자_미만이거나_10자_초과이면_유효하지_않다() {
        assertFalse(NicknameValidator.isValid(""))
        assertFalse(NicknameValidator.isValid("a"))
        assertFalse(NicknameValidator.isValid("a".repeat(11)))
    }

    @Test
    fun 특수문자나_공백이_포함되면_유효하지_않다() {
        assertFalse(NicknameValidator.isValid("주연!"))
        assertFalse(NicknameValidator.isValid("kiri_1"))
        assertFalse(NicknameValidator.isValid("주 연주"))
    }

    @Test
    fun 완성되지_않은_한글_자모는_유효하지_않다() {
        assertFalse(NicknameValidator.isValid("ㄱㄴ"))
        assertFalse(NicknameValidator.isValid("주여ㄴ"))
    }
}
