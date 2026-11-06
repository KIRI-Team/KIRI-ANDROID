package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.CalendarScheduleStatus
import com.kiri.domain.model.schedule.InvalidScheduleUpdateException
import com.kiri.domain.model.schedule.NotScheduleProposerException
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleNotEditableException
import com.kiri.domain.model.schedule.ScheduleParticipant
import com.kiri.domain.model.schedule.ScheduleUpdate
import com.kiri.domain.repository.CalendarScheduleRepository
import com.kiri.domain.usecase.schedule.GetScheduleEditAccessUseCase
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 제안자가 일정 등록 화면의 변경 모드로 기존 일정을 바꾸는 흐름을 확인한다
@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleEditViewModelTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-04T03:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun teardown() = Dispatchers.resetMain()

    private class FakeSchedules(
        var detail: Result<ScheduleDetail>,
        var update: Result<Unit> = Result.success(Unit),
    ) : CalendarScheduleRepository {
        val updates = mutableListOf<Pair<Long, ScheduleUpdate>>()
        var monthly: List<CalendarSchedule> = emptyList()

        override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> =
            Result.success(monthly.filter { YearMonth.from(it.date) == month })

        override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> = detail

        override suspend fun createSchedule(bucketItemId: Long, form: ScheduleUpdate): Result<Long> = Result.success(1)

        override suspend fun updateSchedule(scheduleId: Long, update: ScheduleUpdate): Result<Unit> {
            updates += scheduleId to update
            return this.update
        }
    }

    // 나(1, 엄마)가 제안했고 첫째(2)를 초대한 일정. 아빠(3)는 초대하지 않았다
    private val detail = ScheduleDetail(
        date = LocalDate.of(2026, 11, 17),
        time = LocalTime.of(14, 50),
        status = ScheduleDisplayStatus.CONFIRMED,
        participants = listOf(ScheduleParticipant("엄마", true), ScheduleParticipant("첫째", false)),
        missionId = 42,
    )

    private val family = FakeFamily("엄마", listOf("2" to "첫째", "3" to "아빠"))

    private fun viewModel(repo: CalendarScheduleRepository, home: FakeFamily = family) =
        ScheduleEditViewModel(
            SavedStateHandle(mapOf(ScheduleEditViewModel.SCHEDULE_ID_ARG to 100L)),
            GetScheduleEditAccessUseCase(repo, home),
            repo,
            clock,
        )

    @Test
    fun 열면_기존_날짜_시간과_초대한_가족을_미리_채우고_나는_고를_수_없다() {
        val state = viewModel(FakeSchedules(Result.success(detail))).uiState.value

        assertFalse(state.loading)
        assertNull(state.loadError)
        assertEquals("11월 17일 약속", state.title)
        assertEquals("약속 확정", state.subtitle)
        assertEquals(listOf("2", "3"), state.members.map { it.id })
        assertEquals(LocalDate.of(2026, 11, 17), state.draft.date)
        assertEquals(LocalTime.of(14, 50), state.draft.time)
        assertEquals(setOf("2"), state.draft.memberIds)
        assertEquals("", state.draft.message)
    }

    @Test
    fun 바꾼_값으로_일정_수정을_요청하고_성공하면_저장_효과를_보낸다() = runTest {
        val repo = FakeSchedules(Result.success(detail))
        val vm = viewModel(repo)

        vm.onDraftChange(
            vm.uiState.value.draft.copy(
                date = LocalDate.of(2026, 11, 18),
                time = LocalTime.of(15, 0),
                memberIds = setOf("2", "3"),
                message = "날짜 바꿨어요",
            ),
        )
        vm.save()

        assertEquals(ScheduleEditEffect.Saved, vm.effect.first())
        assertEquals(
            listOf(100L to ScheduleUpdate(LocalDate.of(2026, 11, 18), LocalTime.of(15, 0), setOf(2L, 3L), "날짜 바꿨어요")),
            repo.updates,
        )
        assertFalse(vm.uiState.value.saving)
    }

    @Test
    fun 초대할_수_없는_구성원은_고를_수_없고_아무도_없으면_저장하지_않는다() {
        val repo = FakeSchedules(Result.success(detail))
        val vm = viewModel(repo)

        vm.onDraftChange(vm.uiState.value.draft.copy(memberIds = setOf("1", "99")))
        assertTrue(vm.uiState.value.draft.memberIds.isEmpty())

        vm.save()

        assertTrue(repo.updates.isEmpty())
    }

    @Test
    fun 한_줄이_비어_있으면_보내지_않는다() {
        val repo = FakeSchedules(Result.success(detail))
        val vm = viewModel(repo)

        vm.onDraftChange(vm.uiState.value.draft.copy(message = "  "))
        vm.save()

        assertNull(repo.updates.single().second.message)
    }

    @Test
    fun 수정에_실패하면_이유에_맞는_문구를_보여준다() {
        listOf(
            NotScheduleProposerException() to "일정 제안자만 수정할 수 있어요.",
            ScheduleNotEditableException() to "지금은 변경할 수 없는 약속이에요.",
            InvalidScheduleUpdateException("잘못된 요청입니다.") to "날짜·시간과 함께할 가족을 다시 확인해주세요.",
            IOException() to "약속을 변경하지 못했어요. 다시 시도해주세요.",
        ).forEach { (error, message) ->
            val vm = viewModel(FakeSchedules(Result.success(detail), update = Result.failure(error)))

            vm.save()

            assertEquals(message, vm.uiState.value.error)
            assertFalse(vm.uiState.value.saving)
        }
    }

    @Test
    fun 일정이나_가족을_불러오지_못하면_오류를_보여주고_다시_시도할_수_있다() {
        val repo = FakeSchedules(Result.failure(IOException()))
        val vm = viewModel(repo)
        assertEquals("약속 정보를 불러오지 못했어요.", vm.uiState.value.loadError)

        repo.detail = Result.success(detail)
        vm.retry()
        assertNull(vm.uiState.value.loadError)
        assertEquals(setOf("2"), vm.uiState.value.draft.memberIds)

        val noFamily = viewModel(FakeSchedules(Result.success(detail)), FakeFamily(null))
        assertEquals("가족 정보를 불러오지 못했어요.", noFamily.uiState.value.loadError)
    }

    @Test
    fun 당일_완료된_일정이나_내가_제안하지_않은_일정은_변경할_수_없다고_보여준다() {
        listOf(
            detail.copy(status = ScheduleDisplayStatus.TODAY) to "지금은 변경할 수 없는 약속이에요.",
            detail.copy(status = ScheduleDisplayStatus.COMPLETED) to "지금은 변경할 수 없는 약속이에요.",
            detail.copy(participants = detail.participants.reversed()) to "일정 제안자만 수정할 수 있어요.",
        ).forEach { (schedule, message) ->
            val vm = viewModel(FakeSchedules(Result.success(schedule)))

            assertEquals(message, vm.uiState.value.loadError)
        }
    }

    @Test
    fun 다른_일정이_있는_날로는_바꿀_수_없고_바꾸는_일정_자신의_날은_고를_수_있다() = runTest {
        val repo = FakeSchedules(Result.success(detail))
        repo.monthly = listOf(
            // 바꾸는 일정 자신(100)
            CalendarSchedule(100, LocalDate.of(2026, 11, 17), LocalTime.of(14, 50), CalendarScheduleStatus.CONFIRMED, null),
            CalendarSchedule(7, LocalDate.of(2026, 11, 18), LocalTime.of(10, 0), CalendarScheduleStatus.PENDING_RESPONSE, null),
        )
        val vm = viewModel(repo)

        vm.onMonthShown(YearMonth.of(2026, 11))
        assertEquals(setOf(LocalDate.of(2026, 11, 18)), vm.uiState.value.occupiedDates)

        vm.onDraftChange(vm.uiState.value.draft.copy(date = LocalDate.of(2026, 11, 18)))
        vm.save()
        assertTrue(repo.updates.isEmpty())
        assertEquals("이미 약속이 있는 날이에요. 다른 날짜를 골라주세요.", vm.uiState.value.error)
        assertFalse(vm.uiState.value.saving)

        // 시간만 바꾸면 원래 날짜 그대로 저장한다
        vm.onDraftChange(vm.uiState.value.draft.copy(date = LocalDate.of(2026, 11, 17), time = LocalTime.of(16, 0)))
        vm.save()
        assertEquals(ScheduleEditEffect.Saved, vm.effect.first())
        assertEquals(LocalDate.of(2026, 11, 17), repo.updates.single().second.date)
    }
}
