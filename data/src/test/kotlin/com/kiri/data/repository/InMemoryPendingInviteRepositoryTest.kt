package com.kiri.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryPendingInviteRepositoryTest {

    @Test
    fun 보관한_코드는_한_번만_꺼낼_수_있다() {
        val repository = InMemoryPendingInviteRepository()

        repository.save("AB12CD")

        assertEquals("AB12CD", repository.consume())
        assertNull(repository.consume())
    }

    @Test
    fun 새_코드를_받으면_이전_코드를_덮어쓴다() {
        val repository = InMemoryPendingInviteRepository()

        repository.save("AB12CD")
        repository.save("ZZ99ZZ")

        assertEquals("ZZ99ZZ", repository.consume())
    }
}
