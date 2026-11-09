package com.kiri.domain.model.invite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InviteLinkTest {
    private val token = "eyJhbGciOiJIUzI1NiJ9.eyJmYW1pbHlJZCI6OX0.a-b_c123"

    @Test
    fun 실제_도메인으로_초대_링크를_만든다() {
        assertEquals("https://kiri.app/invite/$token", InviteLink.urlOf(token))
    }

    @Test
    fun 초대_링크에서_토큰을_꺼낸다() {
        assertEquals(token, InviteLink.parseToken("https", "kiri.app", "/invite/$token"))
        assertEquals(token, InviteLink.parseToken("https", "kiri.app", "/invite/$token/"))
    }

    @Test
    fun 다른_스킴이나_호스트의_링크는_무시한다() {
        assertNull(InviteLink.parseToken("http", InviteLink.HOST, "/invite/$token"))
        assertNull(InviteLink.parseToken("https", "evil.example.com", "/invite/$token"))
        assertNull(InviteLink.parseToken("https", "kiri.app.evil.com", "/invite/$token"))
    }

    @Test
    fun 초대_경로가_아니거나_토큰이_없으면_무시한다() {
        assertNull(InviteLink.parseToken("https", InviteLink.HOST, "/home"))
        assertNull(InviteLink.parseToken("https", InviteLink.HOST, "/invite/"))
        assertNull(InviteLink.parseToken("https", InviteLink.HOST, "/invite/ "))
        assertNull(InviteLink.parseToken("https", InviteLink.HOST, "/invitation/$token"))
        assertNull(InviteLink.parseToken("https", InviteLink.HOST, "/invite/AB/CD"))
        assertNull(InviteLink.parseToken("https", InviteLink.HOST, null))
    }
}
