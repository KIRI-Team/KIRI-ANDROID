package com.kiri.data.repository

import com.kiri.domain.model.invite.InviteLink
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeInviteRepositoryTest {

    private val seoul = ZoneId.of("Asia/Seoul")
    private val clock = Clock.fixed(Instant.parse("2026-10-10T01:00:00Z"), seoul)
    private val repository = FakeInviteRepository(clock)

    @Test
    fun 영문_대문자와_숫자_6자리_코드와_그_코드의_링크를_발급한다() = runTest {
        val invite = repository.createInvite().getOrThrow()

        assertTrue(invite.code.matches(Regex("^[A-Z0-9]{6}$")))
        assertEquals(InviteLink.urlOf(invite.code), invite.url)
    }

    @Test
    fun 발급한_날부터_7일_동안_쓸_수_있다() = runTest {
        val invite = repository.createInvite().getOrThrow()

        assertEquals(LocalDate.of(2026, 10, 17), invite.expiresAt)
    }

    @Test
    fun 발급하기_전에는_지금_초대가_없다() = runTest {
        assertNull(repository.getCurrentInvite())
    }

    @Test
    fun 새로_발급하면_지금_초대가_새_초대로_바뀐다() = runTest {
        val first = repository.createInvite().getOrThrow()
        assertEquals(first, repository.getCurrentInvite())

        val second = repository.createInvite().getOrThrow()
        assertEquals(second, repository.getCurrentInvite())
    }
}
