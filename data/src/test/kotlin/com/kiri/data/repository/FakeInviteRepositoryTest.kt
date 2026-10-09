package com.kiri.data.repository

import com.kiri.domain.model.invite.InviteLink
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeInviteRepositoryTest {

    @Test
    fun 영문_대문자와_숫자_6자리_코드와_그_코드의_링크를_발급한다() = runTest {
        val invite = FakeInviteRepository().createInvite().getOrThrow()

        assertTrue(invite.code.matches(Regex("^[A-Z0-9]{6}$")))
        assertEquals(InviteLink.urlOf(invite.code), invite.url)
    }
}
