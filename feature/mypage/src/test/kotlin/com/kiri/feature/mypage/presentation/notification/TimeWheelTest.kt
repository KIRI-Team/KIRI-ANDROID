package com.kiri.feature.mypage.presentation.notification

import org.junit.Assert.assertEquals
import org.junit.Test

// 알림 시간 칸은 끝없이 돌아가고, 어디서 멈추든 가운데에 가장 가까운 숫자 하나를 고른다
class TimeWheelTest {

    @Test
    fun 반_넘게_지나간_숫자는_다음_숫자로_고른다() {
        assertEquals(10, nearestWheelIndex(firstIndex = 10, firstOffsetPx = 0, itemPx = 28))
        assertEquals(10, nearestWheelIndex(firstIndex = 10, firstOffsetPx = 13, itemPx = 28))
        assertEquals(11, nearestWheelIndex(firstIndex = 10, firstOffsetPx = 14, itemPx = 28))
        assertEquals(11, nearestWheelIndex(firstIndex = 10, firstOffsetPx = 27, itemPx = 28))
    }

    @Test
    fun 칸_크기를_아직_모르면_첫_숫자를_고른다() {
        assertEquals(10, nearestWheelIndex(firstIndex = 10, firstOffsetPx = 5, itemPx = 0))
    }

    @Test
    fun 끝을_넘기면_처음으로_돌아온다() {
        assertEquals(23, wheelValue(23, 24))
        assertEquals(0, wheelValue(24, 24))
        assertEquals(59, wheelValue(119, 60))
    }

    @Test
    fun 시작_위치는_목록_가운데쯤에서_현재_값을_가리킨다() {
        val start = wheelStartIndex(value = 9, count = 24)

        assertEquals(9, wheelValue(start, 24))
        // 위로 한참 넘겨도 목록 앞쪽 끝에 닿지 않는다
        assert(start > 24 * 100)
        assertEquals(0, wheelValue(wheelStartIndex(value = 0, count = 60), 60))
    }
}
