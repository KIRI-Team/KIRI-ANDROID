package com.kiri.domain.model.schedule

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 일정 상세의 응답 상태로 요청알림·변경알림 화면을 고르는 규칙을 확인한다
class ScheduleDetailTest {
    private val detail = ScheduleDetail(LocalDate.of(2026, 11, 17), null, ScheduleDisplayStatus.SCHEDULED, emptyList())

    @Test
    fun 미응답이고_응답할_수_있을_때만_처음_받은_초대다() {
        assertTrue(detail.copy(myStatus = MyScheduleStatus.PENDING, canRespond = true).isFirstInvitation)
        // 응답 마감 등으로 서버가 막으면 요청알림이 아니다
        assertFalse(detail.copy(myStatus = MyScheduleStatus.PENDING, canRespond = false).isFirstInvitation)
        assertFalse(detail.copy(myStatus = MyScheduleStatus.RECONFIRM_REQUIRED, canRespond = true).isFirstInvitation)
    }

    @Test
    fun 재확인_필요면_변경알림이다() {
        assertTrue(detail.copy(myStatus = MyScheduleStatus.RECONFIRM_REQUIRED).needsReconfirm)
        assertFalse(detail.copy(myStatus = MyScheduleStatus.ACCEPTED).needsReconfirm)
        assertFalse(detail.needsReconfirm)
    }

    @Test
    fun 참여자는_수락한_상태만_수락으로_본다() {
        assertEquals(
            listOf(true, false, false, false, false),
            listOf(
                MyScheduleStatus.ACCEPTED, MyScheduleStatus.PENDING, MyScheduleStatus.DECLINED,
                MyScheduleStatus.RECONFIRM_REQUIRED, null,
            ).map { ScheduleParticipant("엄마", it).accepted },
        )
    }
}
