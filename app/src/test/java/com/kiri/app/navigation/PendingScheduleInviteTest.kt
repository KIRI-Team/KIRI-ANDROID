package com.kiri.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingScheduleInviteTest {
    @Test
    fun 받은_일정을_연_뒤에는_다시_열지_않는다() {
        val pending = PendingScheduleInvite()

        pending.save(42)
        assertEquals(42L, pending.scheduleId.value)

        pending.consume(42)
        assertNull(pending.scheduleId.value)
    }

    @Test
    fun 여는_사이_새_초대장이_오면_새_일정은_남긴다() {
        val pending = PendingScheduleInvite()

        pending.save(42)
        pending.save(7)
        pending.consume(42)

        assertEquals(7L, pending.scheduleId.value)
    }
}
