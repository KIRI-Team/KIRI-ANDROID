package com.kiri.feature.onboarding.presentation

import com.kiri.domain.model.nickname.NicknameRule
import org.junit.Assert.assertEquals
import org.junit.Test

class NicknameGuidesTest {

    @Test
    fun 위반한_규칙이_없으면_모든_안내가_기본_색이다() {
        assertEquals(listOf(false, false, false), nicknameGuides(emptySet()).map { it.isError })
    }

    @Test
    fun 위반한_규칙의_안내만_오류로_표시한다() {
        val guides = nicknameGuides(setOf(NicknameRule.LENGTH, NicknameRule.NO_SPECIAL_CHARACTERS))

        assertEquals(
            listOf(
                "한글/영어/숫자 혼합 가능" to false,
                "최소 2자 ~ 최대 12자 이내" to true,
                "특수 문자 입력 불가능" to true,
            ),
            guides.map { it.text to it.isError },
        )
    }

    @Test
    fun 가족명_안내는_최대_15자로_표시한다() {
        val guides = familyNameGuides(setOf(NicknameRule.LENGTH))

        assertEquals("최소 2자 ~ 최대 15자 이내" to true, guides[1].let { it.text to it.isError })
        assertEquals(3, guides.size)
    }

    @Test
    fun 가족명이_중복이면_가족_이름_중복_안내를_오류로_덧붙인다() {
        val guides = familyNameGuides(emptySet(), isDuplicated = true)

        assertEquals(4, guides.size)
        assertEquals("이미 존재하는 가족 이름입니다." to true, guides.last().let { it.text to it.isError })
    }

    @Test
    fun 중복이면_규칙_안내_아래에_중복_안내를_오류로_덧붙인다() {
        val guides = nicknameGuides(emptySet(), isDuplicated = true)

        assertEquals(4, guides.size)
        assertEquals("이미 사용 중인 닉네임입니다." to true, guides.last().let { it.text to it.isError })
    }
}
