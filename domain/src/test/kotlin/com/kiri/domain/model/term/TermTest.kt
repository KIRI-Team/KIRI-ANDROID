package com.kiri.domain.model.term

import org.junit.Assert.assertEquals
import org.junit.Test

class TermTest {

    @Test
    fun 필수_약관의_동의_제목에는_필수_표시가_붙는다() {
        val term = Term(id = 1, title = "서비스 이용약관", content = "", required = true)

        assertEquals("[필수] 서비스 이용약관", term.agreementTitle)
    }

    @Test
    fun 선택_약관의_동의_제목에는_선택_표시가_붙는다() {
        val term = Term(id = 3, title = "마케팅 정보 수신 동의", content = "", required = false)

        assertEquals("[선택] 마케팅 정보 수신 동의", term.agreementTitle)
    }
}
