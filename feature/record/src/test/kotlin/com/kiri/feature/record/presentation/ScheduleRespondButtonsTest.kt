package com.kiri.feature.record.presentation

import com.kiri.domain.model.schedule.MyScheduleStatus
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.feature.record.ServerScheduleDetailUiState
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 홈 상단 카드로 연 상세 아래 거절·수락 버튼을 보여줄지
class ScheduleRespondButtonsTest {
    private fun state(status: ScheduleDisplayStatus?, mine: MyScheduleStatus = MyScheduleStatus.PENDING, canRespond: Boolean = true) =
        ServerScheduleDetailUiState(
            schedule = ScheduleDetail(LocalDate.of(2026, 11, 20), LocalTime.of(10, 0), status, emptyList(), myStatus = mine, canRespond = canRespond),
            canRespond = canRespond,
        )

    @Test
    fun 확정된_약속은_서버가_응답을_허락해도_거절_수락을_숨긴다() {
        listOf(ScheduleDisplayStatus.CONFIRMED, ScheduleDisplayStatus.TODAY, ScheduleDisplayStatus.COMPLETED).forEach {
            assertFalse(it.name, state(it).showsRespondButtons)
        }
    }

    @Test
    fun 확정_전_약속은_서버가_허락할_때만_보여준다() {
        assertTrue(state(ScheduleDisplayStatus.SCHEDULED).showsRespondButtons)
        assertFalse(state(ScheduleDisplayStatus.SCHEDULED, canRespond = false).showsRespondButtons)
    }

    @Test
    fun 확정된_약속이_바뀌어_재확인이_필요하면_보여준다() {
        assertTrue(state(ScheduleDisplayStatus.CONFIRMED, mine = MyScheduleStatus.RECONFIRM_REQUIRED).showsRespondButtons)
    }
}
