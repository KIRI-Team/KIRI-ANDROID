package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.detail.Condition
import com.kiri.domain.detail.DetailParticipant
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.CancelledScheduleException
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleParticipant
import com.kiri.domain.repository.CalendarScheduleRepository
import com.kiri.feature.detail.model.DetailUiEvent
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 캘린더에서 연 서버 일정 상세 화면이 일정 상세 API 결과로 채워지는지 확인한다
@OptIn(ExperimentalCoroutinesApi::class)
class ServerScheduleDetailViewModelTest {

    @Before
    fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun teardown() = Dispatchers.resetMain()

    private class FakeSchedules(var result: Result<ScheduleDetail>) : CalendarScheduleRepository {
        val requested = mutableListOf<Long>()

        override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> =
            Result.success(emptyList())

        override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> {
            requested += scheduleId
            return result
        }
    }

    private val detail = ScheduleDetail(
        date = LocalDate.of(2026, 11, 17),
        time = LocalTime.of(14, 50),
        status = ScheduleDisplayStatus.TODAY,
        participants = listOf(ScheduleParticipant("엄마", true), ScheduleParticipant("첫째", false)),
    )

    private fun viewModel(repo: CalendarScheduleRepository, id: Long? = 7) =
        ServerScheduleDetailViewModel(
            SavedStateHandle(id?.let { mapOf(ServerScheduleDetailViewModel.SCHEDULE_ID_ARG to it) }.orEmpty()),
            repo,
        )

    @Test
    fun 열면_해당_일정의_상세를_불러와_날짜_시간_참여자를_채운다() {
        val repo = FakeSchedules(Result.success(detail))

        val state = viewModel(repo).uiState.value
        val filled = state.detail.detail!!

        assertEquals(listOf(7L), repo.requested)
        assertFalse(state.detail.loading)
        assertEquals(detail, state.schedule)
        assertEquals("11월 17일 약속", filled.title)
        assertEquals(LocalDate.of(2026, 11, 17), filled.date)
        assertEquals("오후 2시 50분", filled.startTime)
        assertEquals(listOf(DetailParticipant("엄마", true), DetailParticipant("첫째", false)), filled.participants)
        // API 가 주지 않는 정보는 지어내지 않는다
        assertNull(filled.place)
        assertTrue(filled.activities.isEmpty())
        assertTrue(filled.nearby.isEmpty())
    }

    @Test
    fun 시간을_모르면_비워_두고_완료된_일정은_완료로_표시한다() {
        val repo = FakeSchedules(Result.success(detail.copy(time = null, status = ScheduleDisplayStatus.COMPLETED)))

        val filled = viewModel(repo).uiState.value.detail.detail!!

        assertNull(filled.startTime)
        assertTrue(filled.completed)
    }

    @Test
    fun 불러오지_못하면_오류를_보여주고_다시_시도하면_새로_불러온다() {
        val repo = FakeSchedules(Result.failure(IOException()))
        val vm = viewModel(repo)
        assertEquals("약속 정보를 불러오지 못했어요.", vm.uiState.value.detail.error)
        assertNull(vm.uiState.value.detail.detail)

        repo.result = Result.success(detail)
        vm.onEvent(DetailUiEvent.RetryClicked)

        assertNull(vm.uiState.value.detail.error)
        assertEquals(detail, vm.uiState.value.schedule)
        assertEquals(listOf(7L, 7L), repo.requested)
    }

    @Test
    fun 취소된_일정은_취소_문구를_보여준다() {
        val vm = viewModel(FakeSchedules(Result.failure(CancelledScheduleException())))

        assertEquals("취소된 약속이에요.", vm.uiState.value.detail.error)
    }

    @Test
    fun 일정_id_가_없으면_요청하지_않고_찾을_수_없다고_보여준다() {
        val repo = FakeSchedules(Result.success(detail))

        val state = viewModel(repo, id = null).uiState.value

        assertTrue(repo.requested.isEmpty())
        assertEquals("찾을 수 없는 약속이에요.", state.detail.error)
    }

    @Test
    fun 뒤로가기를_누르면_닫기_효과를_보낸다() = runTest {
        val vm = viewModel(FakeSchedules(Result.success(detail)))

        vm.onEvent(DetailUiEvent.BackClicked)

        assertEquals(ServerScheduleDetailEffect.Back, vm.effect.first())
    }

    @Test
    fun 약속_당일이면_컨디션_커스텀_화면을_열고_조건을_고를_수_있다() = runTest {
        val vm = viewModel(FakeSchedules(Result.success(detail)))
        assertTrue(vm.uiState.value.canCustomize)

        vm.onEvent(DetailUiEvent.CustomClicked)
        assertEquals(ServerScheduleDetailEffect.OpenCustom, vm.effect.first())

        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.MORE_REST))
        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.LESS_WALKING))
        vm.onEvent(DetailUiEvent.ConditionToggled(Condition.MORE_REST))
        assertEquals(setOf(Condition.LESS_WALKING), vm.uiState.value.detail.draft)

        // 적용 API 가 없어 적용하기는 아무 것도 바꾸지 않는다
        vm.onEvent(DetailUiEvent.ApplyClicked)
        assertFalse(vm.uiState.value.detail.saving)
        assertTrue(vm.uiState.value.detail.detail!!.conditions.isEmpty())

        vm.onEvent(DetailUiEvent.CustomCancelled)
        assertEquals(ServerScheduleDetailEffect.Back, vm.effect.first())

        // 다시 들어가면 고른 조건을 비우고 시작한다
        vm.onEvent(DetailUiEvent.CustomClicked)
        assertEquals(ServerScheduleDetailEffect.OpenCustom, vm.effect.first())
        assertTrue(vm.uiState.value.detail.draft.isEmpty())
    }

    @Test
    fun 당일이_아니거나_불러오지_못했으면_컨디션_커스텀을_열지_않는다() = runTest {
        ScheduleDisplayStatus.entries.filter { it != ScheduleDisplayStatus.TODAY }.forEach { status ->
            val vm = viewModel(FakeSchedules(Result.success(detail.copy(status = status))))
            assertFalse(vm.uiState.value.canCustomize)
            vm.onEvent(DetailUiEvent.CustomClicked)
            assertNull(withTimeoutOrNull(100) { vm.effect.first() })
        }
        val failed = viewModel(FakeSchedules(Result.failure(IOException())))
        assertFalse(failed.uiState.value.canCustomize)
        failed.onEvent(DetailUiEvent.CustomClicked)
        assertNull(withTimeoutOrNull(100) { failed.effect.first() })
    }

    @Test
    fun 배지는_캘린더와_같이_당일을_확정으로_보여준다() {
        assertEquals("약속 확정", ScheduleDisplayStatus.TODAY.badgeLabel())
        assertEquals("약속 확정", ScheduleDisplayStatus.CONFIRMED.badgeLabel())
        assertEquals("약속 예정", ScheduleDisplayStatus.SCHEDULED.badgeLabel())
        assertEquals("약속 완료", ScheduleDisplayStatus.COMPLETED.badgeLabel())
        assertEquals("약속", null.badgeLabel())
    }
}
