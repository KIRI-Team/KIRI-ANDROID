package com.kiri.feature.detail.model

import com.kiri.domain.detail.Condition
import com.kiri.domain.detail.MissionDetail
import com.kiri.domain.detail.NearbyPlace
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

// 서버가 정렬한 근처 놀거리는 앱에서 다시 정렬하지 않는다
class DetailUiStateNearbyTest {
    private val expensiveFirst = listOf(
        NearbyPlace("a", "비싼 곳", "", "", costWon = 30000),
        NearbyPlace("b", "무료", "", "", costWon = 0),
    )

    private fun detail() = MissionDetail(
        id = "1", title = "t", familyName = "", date = LocalDate.of(2026, 11, 8), startTime = null, place = null,
        expectedSteps = null, durationMinutes = null, participants = emptyList(), completed = false,
        activities = emptyList(), essentials = emptyList(), nearby = expensiveFirst,
        conditions = setOf(Condition.LOWER_BUDGET),
    )

    @Test
    fun 서버가_정렬한_목록은_조건이_있어도_순서를_그대로_둔다() {
        val state = DetailUiState(detail = detail(), nearbyRankedByServer = true)

        assertEquals(listOf("a", "b"), state.nearby.map { it.id })
    }

    @Test
    fun 앱이_가진_목록은_지금처럼_조건으로_다시_정렬한다() {
        val state = DetailUiState(detail = detail())

        assertEquals(listOf("b", "a"), state.nearby.map { it.id })
    }
}
