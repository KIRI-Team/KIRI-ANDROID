package com.kiri.domain.home

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

// 가족 정보로 방장·참여자·둘러보기를 나누는지 확인한다
class FamilyAccessTest {
    private fun family(name: String?, me: String = "13", owner: String? = "16") = FamilyHome(
        LocalDate.of(2026, 11, 11), me, name, emptyList(), null, emptyMap(), emptySet(), null,
        serverBacked = true, ownerId = owner,
    )

    @Test
    fun 소속된_가족이_없으면_둘러보기다() {
        assertEquals(FamilyAccess.BROWSING, family(name = null).access)
    }

    @Test
    fun 가족을_만든_사람은_방장이고_나머지는_참여자다() {
        assertEquals(FamilyAccess.OWNER, family("끼리네", me = "16").access)
        assertEquals(FamilyAccess.MEMBER, family("끼리네", me = "13").access)
    }

    @Test
    fun 방장을_모르면_참여자로_본다() {
        assertEquals(FamilyAccess.MEMBER, family("끼리네", owner = null).access)
    }
}
