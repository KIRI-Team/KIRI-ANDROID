package com.kiri.feature.mypage.presentation.nameedit

import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.domain.model.nickname.NicknameValidator
import org.junit.Assert.assertEquals
import org.junit.Test

class NameErrorsTest {

    @Test
    fun 닉네임_길이_오류는_최대_12자로_안내한다() {
        assertEquals(listOf("최소 2자 ~ 최대 12자 이내로 입력해주세요"), nameErrors(setOf(NicknameRule.LENGTH)))
    }

    @Test
    fun 가족명_길이_오류는_최대_15자로_안내한다() {
        assertEquals(
            listOf("최소 2자 ~ 최대 15자 이내로 입력해주세요"),
            nameErrors(setOf(NicknameRule.LENGTH), NicknameValidator.FAMILY_NAME_MAX_LENGTH),
        )
    }
}
