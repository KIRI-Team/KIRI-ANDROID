package com.kiri.domain.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class KiriTimeTest {
    @Test
    fun dayBoundaryUsesKoreaRegardlessOfDeviceZone() {
        val instant = Instant.parse("2026-10-25T15:00:00Z")
        for (zone in listOf("UTC", "America/Los_Angeles", "Pacific/Auckland")) {
            val clock = Clock.fixed(instant, ZoneId.of(zone))
            assertEquals(LocalDate.of(2026, 10, 26), clock.kiriToday())
            assertEquals(0, clock.kiriNow().hour)
            assertEquals(
                LocalDate.of(2026, 10, 25),
                Clock.fixed(instant.minusNanos(1), ZoneId.of(zone)).kiriToday(),
            )
        }
    }
}
