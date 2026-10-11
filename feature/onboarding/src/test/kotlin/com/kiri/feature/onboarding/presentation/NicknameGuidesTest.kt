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
                "최소 2자 ~ 최대 10자 이내" to true,
                "특수 문자 입력 불가능" to true,
            ),
            guides.map { it.text to it.isError },
        )
    }
}
