package com.kiri.domain.model.nickname

import org.junit.Assert.assertEquals
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

    @Test
    fun 유효한_닉네임은_위반한_규칙이_없다() {
        assertEquals(emptySet<NicknameRule>(), NicknameValidator.violations("끼리Kiri7"))
    }

    @Test
    fun 한_글자면_길이_규칙만_위반한다() {
        assertEquals(setOf(NicknameRule.LENGTH), NicknameValidator.violations("주"))
    }

    @Test
    fun 특수문자나_공백이_있으면_특수문자_규칙을_위반한다() {
        assertEquals(setOf(NicknameRule.NO_SPECIAL_CHARACTERS), NicknameValidator.violations("주연!"))
        assertEquals(setOf(NicknameRule.NO_SPECIAL_CHARACTERS), NicknameValidator.violations("주 연주"))
    }

    @Test
    fun 완성되지_않은_자모나_다른_문자는_허용_문자_규칙을_위반한다() {
        assertEquals(setOf(NicknameRule.ALLOWED_LETTERS), NicknameValidator.violations("주여ㄴ"))
        assertEquals(setOf(NicknameRule.ALLOWED_LETTERS), NicknameValidator.violations("漢字"))
    }

    @Test
    fun 여러_규칙을_동시에_위반하면_모두_반환한다() {
        assertEquals(
            setOf(NicknameRule.LENGTH, NicknameRule.NO_SPECIAL_CHARACTERS),
            NicknameValidator.violations("!"),
        )
        assertEquals(
            NicknameRule.entries.toSet(),
            NicknameValidator.violations("ㄱ" + "!".repeat(10)),
        )
    }
}
