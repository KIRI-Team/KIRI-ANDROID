package com.kiri.feature.record.presentation.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// 완료 후 리뷰가 있는 날짜 칸의 숫자 색: 사진 가운데가 밝으면 검정, 어두우면 흰색 (Figma 추억기록날짜_black / _white)
class ReviewPhotoCellTest {
    private fun gray(v: Int) = (0xFF shl 24) or (v shl 16) or (v shl 8) or v

    @Test
    fun 흰색과_검정의_평균_휘도는_1과_0이다() {
        assertEquals(1.0, averageLuminance(intArrayOf(gray(255))), 1e-6)
        assertEquals(0.0, averageLuminance(intArrayOf(gray(0))), 1e-6)
        assertEquals(0.5, averageLuminance(intArrayOf(gray(255), gray(0))), 1e-6)
        assertEquals(0.0, averageLuminance(IntArray(0)), 1e-6)
    }

    @Test
    fun 초록은_파랑보다_밝게_센다() {
        val green = averageLuminance(intArrayOf(0xFF00FF00.toInt()))
        val blue = averageLuminance(intArrayOf(0xFF0000FF.toInt()))
        assertTrue(green > blue)
    }

    @Test
    fun 밝은_사진은_검정_숫자_어두운_사진은_흰_숫자다() {
        assertTrue(prefersDarkDayNumber(averageLuminance(intArrayOf(gray(255)))))
        assertTrue(prefersDarkDayNumber(averageLuminance(intArrayOf(gray(200)))))
        assertFalse(prefersDarkDayNumber(averageLuminance(intArrayOf(gray(0)))))
        assertFalse(prefersDarkDayNumber(averageLuminance(intArrayOf(gray(60)))))
    }

    @Test
    fun 두_글자색의_대비가_같아지는_휘도_근처에서_갈린다() {
        // 검정(#1A1A1A)·흰(#D9D9D8) 숫자의 대비가 같아지는 휘도는 약 0.16 이다
        assertFalse(prefersDarkDayNumber(0.15))
        assertTrue(prefersDarkDayNumber(0.18))
    }
}
