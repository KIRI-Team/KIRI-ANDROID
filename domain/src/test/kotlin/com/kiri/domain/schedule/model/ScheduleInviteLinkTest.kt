package com.kiri.domain.schedule.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleInviteLinkTest {
    @Test
    fun 카톡_버튼_실행_파라미터에_일정_id를_담는다() {
        assertEquals(mapOf("scheduleId" to "42"), ScheduleInviteLink.executionParams(42))
    }

    @Test
    fun 카카오톡이_연_링크에서_일정_id를_꺼낸다() {
        assertEquals(42L, ScheduleInviteLink.parseScheduleId("kakaoabc123", "kakaolink", "42"))
    }

    @Test
    fun 카카오_링크가_아니면_무시한다() {
        assertNull(ScheduleInviteLink.parseScheduleId("https", "kakaolink", "42"))
        assertNull(ScheduleInviteLink.parseScheduleId("kakaoabc123", "oauth", "42"))
        assertNull(ScheduleInviteLink.parseScheduleId(null, "kakaolink", "42"))
        assertNull(ScheduleInviteLink.parseScheduleId("kakaoabc123", null, "42"))
    }

    @Test
    fun 일정_id가_없거나_올바르지_않으면_무시한다() {
        assertNull(ScheduleInviteLink.parseScheduleId("kakaoabc123", "kakaolink", null))
        assertNull(ScheduleInviteLink.parseScheduleId("kakaoabc123", "kakaolink", ""))
        assertNull(ScheduleInviteLink.parseScheduleId("kakaoabc123", "kakaolink", "abc"))
        assertNull(ScheduleInviteLink.parseScheduleId("kakaoabc123", "kakaolink", "0"))
        assertNull(ScheduleInviteLink.parseScheduleId("kakaoabc123", "kakaolink", "-3"))
    }
}
