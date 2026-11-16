package com.kiri.app.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// 서버 메시지 형식이 정해지기 전이라 notification 영역과 data 의 title·body 를 모두 받는다
class PushContentTest {

    @Test
    fun notification_영역의_제목과_본문을_먼저_쓴다() {
        val content = pushContentOf("일정 초대", "엄마가 초대했어요", mapOf("title" to "다른 제목", "body" to "다른 본문"))

        assertEquals(PushContent("일정 초대", "엄마가 초대했어요"), content)
    }

    @Test
    fun notification_영역이_없으면_data의_title과_body를_쓴다() {
        val content = pushContentOf(null, " ", mapOf("title" to "콕 찔렀어요", "body" to "아빠가 콕 찔렀어요"))

        assertEquals(PushContent("콕 찔렀어요", "아빠가 콕 찔렀어요"), content)
    }

    @Test
    fun 제목만_있어도_보여준다() {
        assertEquals(PushContent("오늘의 질문", null), pushContentOf("오늘의 질문", null, emptyMap()))
    }

    @Test
    fun 보여줄_내용이_없으면_null이다() {
        assertNull(pushContentOf(null, "", mapOf("scheduleId" to "118")))
    }
}
