package com.kiri.feature.record.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerScheduleDetailRouteTest {
    @Test fun routeAddsOnlyTheFlagsThatAreSet() {
        assertEquals("record_graph/server-schedule/7", serverScheduleDetailRoute(7))
        assertEquals("record_graph/server-schedule/7?fromHome=true", serverScheduleDetailRoute(7, fromHome = true))
        // 홈의 응답 필요 카드는 응답 화면으로 연다
        assertEquals("record_graph/server-schedule/7?fromHome=true&respond=true", serverScheduleDetailRoute(7, fromHome = true, respond = true))
    }

    @Test fun calendarOpensTheScheduleDetail() {
        assertEquals("record_graph/server-schedule/7?date=2026-12-03", calendarScheduleDetailRoute(7, java.time.LocalDate.of(2026, 12, 3)))
    }
}
