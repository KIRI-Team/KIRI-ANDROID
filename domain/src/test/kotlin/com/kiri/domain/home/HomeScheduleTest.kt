package com.kiri.domain.home

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeScheduleTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val now = Instant.parse("2026-10-03T00:00:00Z")
    private fun schedule(id: String, local: String, status: ScheduleStatus, final: Boolean = true, deadline: Instant? = null) = HomeSchedule(id, id, java.time.LocalDateTime.parse(local).atZone(zone).toInstant(), status, final, deadline)

    @Test fun responseRequiredWinsOverConfirmedUpcoming() {
        val result = selectHomeSchedule(listOf(schedule("upcoming", "2026-10-05T10:00", ScheduleStatus.CONFIRMED), schedule("answer", "2026-10-06T10:00", ScheduleStatus.PENDING_RESPONSE, deadline = now.plusSeconds(1000))), now, zone)
        assertEquals("answer", result?.schedule?.id)
        assertEquals(HomeScheduleCard.Kind.RESPONSE_REQUIRED, result?.kind)
    }

    @Test fun expiredResponseIsExcludedAndD7IsIncluded() {
        val result = selectHomeSchedule(listOf(schedule("expired", "2026-10-04T10:00", ScheduleStatus.PENDING_RESPONSE, deadline = now.minusSeconds(1)), schedule("d7", "2026-10-10T10:00", ScheduleStatus.CONFIRMED)), now, zone)
        assertEquals("d7", result?.schedule?.id)
    }

    @Test fun onlyFinalParticipantsAndTodayAreEligible() {
        val result = selectHomeSchedule(listOf(schedule("other", "2026-10-03T10:00", ScheduleStatus.CONFIRMED, final = false), schedule("today", "2026-10-03T18:00", ScheduleStatus.CONFIRMED)), now, zone)
        assertEquals(HomeScheduleCard.Kind.TODAY, result?.kind)
        assertEquals("today", result?.schedule?.id)
    }

    @Test fun cancelledCompletedAndNotConfirmedAreExcluded() {
        assertNull(selectHomeSchedule(listOf(schedule("cancelled", "2026-10-04T10:00", ScheduleStatus.CANCELLED), schedule("done", "2026-10-04T10:00", ScheduleStatus.COMPLETED), schedule("pending", "2026-10-04T10:00", ScheduleStatus.NOT_CONFIRMED)), now, zone))
    }
}
