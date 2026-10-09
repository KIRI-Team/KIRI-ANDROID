package com.kiri.domain.model.invite

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InviteLinkTest {

    @Test
    fun 코드로_초대_링크를_만든다() {
        assertEquals("https://${InviteLink.HOST}/invite/AB12CD", InviteLink.urlOf("AB12CD"))
    }

    @Test
    fun 초대_링크에서_코드를_꺼낸다() {
        assertEquals("AB12CD", InviteLink.parseCode("https", InviteLink.HOST, "/invite/AB12CD"))
        assertEquals("AB12CD", InviteLink.parseCode("https", InviteLink.HOST, "/invite/AB12CD/"))
    }

    @Test
    fun 다른_스킴이나_호스트의_링크는_무시한다() {
        assertNull(InviteLink.parseCode("http", InviteLink.HOST, "/invite/AB12CD"))
        assertNull(InviteLink.parseCode("https", "evil.example.com", "/invite/AB12CD"))
    }

    @Test
    fun 초대_경로가_아니거나_코드가_없으면_무시한다() {
        assertNull(InviteLink.parseCode("https", InviteLink.HOST, "/home"))
        assertNull(InviteLink.parseCode("https", InviteLink.HOST, "/invite/"))
        assertNull(InviteLink.parseCode("https", InviteLink.HOST, "/invitation/AB12CD"))
        assertNull(InviteLink.parseCode("https", InviteLink.HOST, "/invite/AB/CD"))
        assertNull(InviteLink.parseCode("https", InviteLink.HOST, null))
    }
}
