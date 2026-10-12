package com.kiri.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiBaseUrlTest {

    @Test
    fun 끝에_슬래시가_없으면_붙이고_있으면_그대로_둔다() {
        assertEquals("https://api.kiri.com/", apiBaseUrl("https://api.kiri.com"))
        assertEquals("https://api.kiri.com/v1/", apiBaseUrl(" https://api.kiri.com/v1/ "))
    }

    @Test(expected = IllegalStateException::class)
    fun 설정하지_않았으면_멈춘다() {
        apiBaseUrl("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun http_로_시작하지_않으면_멈춘다() {
        apiBaseUrl("api.kiri.com")
    }
}
