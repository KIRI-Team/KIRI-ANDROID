package com.kiri.feature.record.presentation.schedule

import com.kiri.feature.record.presentation.bucket.withSubjectParticle
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleRegisterDraftTest {
    private val filled = ScheduleRegisterDraft(
        date = LocalDate.of(2026, 10, 7),
        time = LocalTime.of(16, 30),
        memberIds = setOf("mom"),
    )

    @Test fun submitNeedsDateTimeAndAtLeastOneMember() {
        assertTrue(filled.canSubmit)
        assertFalse(filled.copy(date = null).canSubmit)
        assertFalse(filled.copy(time = null).canSubmit)
        assertFalse(filled.copy(memberIds = emptySet()).canSubmit)
        assertFalse(ScheduleRegisterDraft().canSubmit)
    }

    @Test fun messageIsOptional() {
        assertTrue(filled.copy(message = "").canSubmit)
    }

    @Test fun messageIsCutAtHundredCharacters() {
        assertEquals(100, filled.withMessage("가".repeat(100)).message.length)
        assertEquals("가".repeat(100), filled.withMessage("가".repeat(101)).message)
        assertEquals("같이 가요", filled.withMessage("같이 가요").message)
    }

    @Test fun scheduleLabelUsesShortYearAndKoreanWeekday() {
        assertEquals("26.10.07(수) · 16:30분", scheduleLabel(LocalDate.of(2026, 10, 7), LocalTime.of(16, 30)))
        assertEquals("27.01.03(일) · 09:00분", scheduleLabel(LocalDate.of(2027, 1, 3), LocalTime.of(9, 0)))
    }

    @Test fun subjectParticleFollowsFinalConsonant() {
        assertEquals("딸이 저장한 미션이에요!", savedMissionLabel("딸"))
        assertEquals("아빠가 저장한 미션이에요!", savedMissionLabel("아빠"))
        assertEquals("아빠가 저장했어요!", savedByLabel("아빠"))
        assertEquals("엄마가 저장했어요!", savedByLabel("엄마"))
        assertEquals("할아버지 지훈이", withSubjectParticle("할아버지 지훈"))
        // 한글이 아니거나 비어 있으면 '가'로 둔다
        assertEquals("Kim가", withSubjectParticle("Kim"))
        assertEquals("가", withSubjectParticle(""))
    }

    @Test fun requestDateLabelShowsDateOnly() {
        assertEquals("26.06.23", requestDateLabel(LocalDate.of(2026, 6, 23)))
    }

    @Test fun receivedPromiseLabelNamesTheProposer() {
        assertEquals("엄마가 보낸 약속이에요!", receivedPromiseLabel("엄마"))
        assertEquals("지훈이 보낸 약속이에요!", receivedPromiseLabel("지훈"))
    }

    @Test fun scheduledPromiseLabelShowsMonthAndDayWithoutLeadingZero() {
        assertEquals("12월 3일에 예정된 약속이에요!", scheduledPromiseLabel(LocalDate.of(2026, 12, 3)))
    }

    @Test fun shareTextHasScheduleMembersAndOptionalMessage() {
        val date = LocalDate.of(2026, 10, 7)
        assertEquals(
            "[끼리] 일정 요청이 왔어요!\n활동: 한강 축제\n일정: 26.10.07(수) · 16:30분\n함께: 엄마, 아빠\n\n같이 가요",
            scheduleRequestShareText("한강 축제", date, LocalTime.of(16, 30), listOf("엄마", "아빠"), "같이 가요"),
        )
        // 한 줄을 비우면 문구 끝에 붙이지 않는다
        assertEquals(
            "[끼리] 일정 요청이 왔어요!\n활동: 한강 축제\n일정: 26.10.07(수) · 16:30분\n함께: 엄마",
            scheduleRequestShareText("한강 축제", date, LocalTime.of(16, 30), listOf("엄마"), " "),
        )
    }

    @Test fun memberCountLabelShowsCount() {
        assertEquals("인원 4명", memberCountLabel(4))
    }
}
