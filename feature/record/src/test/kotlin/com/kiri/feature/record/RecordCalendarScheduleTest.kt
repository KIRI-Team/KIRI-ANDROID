package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.record.FakeRecordRepository
import com.kiri.domain.model.schedule.CalendarScheduleStatus
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.MyScheduleStatus
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.feature.record.model.RecordUiEffect
import com.kiri.feature.record.presentation.calendar.scheduleLabel
import com.kiri.domain.repository.CalendarScheduleRepository
import com.kiri.feature.record.model.RecordCalendarMark
import com.kiri.feature.record.model.RecordUiEvent
import com.kiri.feature.record.model.mergeCalendarMarks
import com.kiri.feature.record.model.toCalendarMarks
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// 기록 캘린더가 서버의 월간 일정을 날짜별 약속 표시에 반영하고, 날짜를 누르면 일정 상세 화면으로 보내는지 확인한다
@OptIn(ExperimentalCoroutinesApi::class)
class RecordCalendarScheduleTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-13T03:00:00Z"), ZoneOffset.UTC)
    private val october = YearMonth.of(2026, 10)
    private val november = YearMonth.of(2026, 11)

    @Before
    fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun teardown() = Dispatchers.resetMain()

    // 요청한 달을 기록하고, 달마다 정해 둔 결과를 돌려준다
    private class RecordingCalendarSchedules : CalendarScheduleRepository {
        val requested = mutableListOf<YearMonth>()
        val results = mutableMapOf<YearMonth, Result<List<CalendarSchedule>>>()

        override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> {
            requested += month
            return results[month] ?: Result.success(emptyList())
        }

        val requestedDetails = mutableListOf<Long>()
        val details = mutableMapOf<Long, Result<ScheduleDetail>>()

        override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> {
            requestedDetails += scheduleId
            return details[scheduleId] ?: Result.failure(ScheduleNotFoundException())
        }

        override suspend fun updateSchedule(scheduleId: Long, update: com.kiri.domain.model.schedule.ScheduleUpdate): Result<Unit> =
            Result.failure(ScheduleNotFoundException())

        override suspend fun createSchedule(bucketItemId: Long, form: com.kiri.domain.model.schedule.ScheduleUpdate): Result<Long> =
            Result.failure(ScheduleNotFoundException())
    }

    private fun records() = FakeRecordRepository(
        FakeHomeRepository(clock, latencyMillis = 0),
        FakeMissionRepository(),
        clock = clock,
    )

    private fun viewModel(schedules: CalendarScheduleRepository) =
        RecordViewModel(SavedStateHandle(), records(), clock, schedules)

    // 기기에 저장된 활동이 없는 날을 골라 서버 일정 표시만 보이도록 한다
    private fun emptyDays(month: YearMonth): List<LocalDate> {
        val vm = viewModel(RecordingCalendarSchedules())
        repeat(month.monthValue - october.monthValue) { vm.onEvent(RecordUiEvent.NextMonth) }
        return (1..month.lengthOfMonth()).map(month::atDay).filter { it !in vm.uiState.value.calendarMarks }
    }

    private fun schedule(id: Long, date: LocalDate, status: CalendarScheduleStatus, my: MyScheduleStatus = MyScheduleStatus.ACCEPTED) =
        CalendarSchedule(id, date, LocalTime.of(10, 0), status, my)

    @Test
    fun 처음_열면_보고_있는_달의_일정을_불러와_표시한다() {
        val (confirmed, planned, cancelled) = emptyDays(october)
        val repo = RecordingCalendarSchedules()
        repo.results[october] = Result.success(
            listOf(
                schedule(1, confirmed, CalendarScheduleStatus.CONFIRMED),
                schedule(2, planned, CalendarScheduleStatus.PENDING_RESPONSE, MyScheduleStatus.NOT_INVITED),
                schedule(3, cancelled, CalendarScheduleStatus.CANCELLED),
            ),
        )

        val state = viewModel(repo).uiState.value

        assertEquals(listOf(october), repo.requested)
        assertEquals(listOf(1L, 2L, 3L), state.monthSchedules.map { it.id })
        assertEquals(RecordCalendarMark.CONFIRMED, state.calendarMarks[confirmed])
        assertEquals(RecordCalendarMark.PLANNED, state.calendarMarks[planned])
        assertEquals(RecordCalendarMark.CANCELLED, state.calendarMarks[cancelled])
    }

    @Test
    fun 달을_넘기면_그_달을_불러오고_이미_받은_달은_다시_요청하지_않는다() {
        val day = emptyDays(november).first()
        val repo = RecordingCalendarSchedules()
        repo.results[november] = Result.success(listOf(schedule(9, day, CalendarScheduleStatus.CONFIRMED)))
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.NextMonth)
        assertEquals(RecordCalendarMark.CONFIRMED, vm.uiState.value.calendarMarks[day])
        assertEquals(listOf(9L), vm.uiState.value.monthSchedules.map { it.id })

        vm.onEvent(RecordUiEvent.PreviousMonth)
        assertNull(vm.uiState.value.calendarMarks[day])
        vm.onEvent(RecordUiEvent.NextMonth)

        assertEquals(listOf(october, november), repo.requested)
        assertEquals(RecordCalendarMark.CONFIRMED, vm.uiState.value.calendarMarks[day])
    }

    @Test
    fun 불러오지_못하면_오류를_보여주고_다시_시도하면_새로_불러온다() {
        val day = emptyDays(october).first()
        val repo = RecordingCalendarSchedules()
        repo.results[october] = Result.failure(IOException())
        val vm = viewModel(repo)
        assertEquals("일정을 불러오지 못했어요.", vm.uiState.value.error)

        repo.results[october] = Result.success(listOf(schedule(1, day, CalendarScheduleStatus.CONFIRMED)))
        vm.onEvent(RecordUiEvent.RetryClicked)

        assertNull(vm.uiState.value.error)
        assertEquals(RecordCalendarMark.CONFIRMED, vm.uiState.value.calendarMarks[day])
        assertEquals(listOf(october, october), repo.requested)
    }

    @Test
    fun 같은_날에는_확정_예정_취소_순으로_하나만_표시한다() {
        val day = LocalDate.of(2026, 10, 3)
        val marks = { statuses: List<CalendarScheduleStatus> ->
            statuses.mapIndexed { i, status -> schedule(i.toLong(), day, status) }.toCalendarMarks()[day]
        }

        assertEquals(RecordCalendarMark.CONFIRMED, marks(listOf(CalendarScheduleStatus.CANCELLED, CalendarScheduleStatus.COMPLETED)))
        assertEquals(RecordCalendarMark.CONFIRMED, marks(listOf(CalendarScheduleStatus.PENDING_RESPONSE, CalendarScheduleStatus.CONFIRMED)))
        assertEquals(RecordCalendarMark.PLANNED, marks(listOf(CalendarScheduleStatus.CANCELLED, CalendarScheduleStatus.NOT_CONFIRMED)))
        assertEquals(RecordCalendarMark.CANCELLED, marks(listOf(CalendarScheduleStatus.CANCELLED, CalendarScheduleStatus.CANCELLED)))
    }

    @Test
    fun 기기_표시와_합칠_때_사진은_유지하고_취소는_다른_표시에_밀린다() {
        val photo = LocalDate.of(2026, 10, 1)
        val bucket = LocalDate.of(2026, 10, 2)
        val question = LocalDate.of(2026, 10, 3)
        val serverOnly = LocalDate.of(2026, 10, 4)
        val local = mapOf(
            photo to RecordCalendarMark.PHOTO_FIRST,
            bucket to RecordCalendarMark.BUCKET,
            question to RecordCalendarMark.QUESTION,
        )
        val server = mapOf(
            photo to RecordCalendarMark.CONFIRMED,
            bucket to RecordCalendarMark.PLANNED,
            question to RecordCalendarMark.CANCELLED,
            serverOnly to RecordCalendarMark.CANCELLED,
        )

        assertEquals(
            mapOf(
                photo to RecordCalendarMark.PHOTO_FIRST,
                bucket to RecordCalendarMark.PLANNED,
                question to RecordCalendarMark.QUESTION,
                serverOnly to RecordCalendarMark.CANCELLED,
            ),
            mergeCalendarMarks(local, server),
        )
    }

    private val today = LocalDate.of(2026, 10, 13)

    // 효과가 오지 않으면 null
    private suspend fun RecordViewModel.nextEffect() = withTimeoutOrNull(100) { effect.first() }

    @Test
    fun 열_수_있는_서버_일정이_하나뿐인_날을_누르면_상세_화면으로_간다() = runTest {
        val day = emptyDays(october).first()
        val repo = RecordingCalendarSchedules()
        repo.results[october] = Result.success(
            listOf(schedule(1, day, CalendarScheduleStatus.CONFIRMED), schedule(2, day, CalendarScheduleStatus.CANCELLED)),
        )
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.DateSelected(day))

        assertEquals(RecordUiEffect.OpenServerSchedule(1, day), vm.nextEffect())
        assertEquals(day, vm.uiState.value.selectedDate)
        // 상세는 상세 화면에서 불러오므로 캘린더에서는 요청하지 않는다
        assertEquals(emptyList<Long>(), repo.requestedDetails)
    }

    @Test
    fun 서버_일정이_여러_개거나_취소만_있는_날은_바로_이동하지_않고_목록에서_고르게_한다() = runTest {
        val (multiple, cancelledOnly) = emptyDays(october)
        val repo = RecordingCalendarSchedules()
        repo.results[october] = Result.success(
            listOf(
                schedule(1, multiple, CalendarScheduleStatus.CONFIRMED),
                schedule(2, multiple, CalendarScheduleStatus.PENDING_RESPONSE),
                schedule(3, cancelledOnly, CalendarScheduleStatus.CANCELLED),
            ),
        )
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.DateSelected(cancelledOnly))
        assertNull(vm.nextEffect())
        vm.onEvent(RecordUiEvent.ServerScheduleClicked(3))
        assertNull(vm.nextEffect())

        vm.onEvent(RecordUiEvent.DateSelected(multiple))
        assertNull(vm.nextEffect())
        vm.onEvent(RecordUiEvent.ServerScheduleClicked(2))
        assertEquals(RecordUiEffect.OpenServerSchedule(2, multiple), vm.nextEffect())
    }

    @Test
    fun 기기에_저장된_활동이_있는_날은_서버_일정이_하나여도_목록에서_고르게_한다() = runTest {
        val vm0 = viewModel(RecordingCalendarSchedules())
        val busy = vm0.uiState.value.entries.first { YearMonth.from(it.date) == october }.date
        val repo = RecordingCalendarSchedules()
        repo.results[october] = Result.success(listOf(schedule(1, busy, CalendarScheduleStatus.CONFIRMED)))
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.DateSelected(busy))

        assertNull(vm.nextEffect())
    }

    @Test
    fun 고른_날에_없는_일정은_눌러도_이동하지_않는다() = runTest {
        val (day, other) = emptyDays(october)
        val repo = RecordingCalendarSchedules()
        repo.results[october] = Result.success(listOf(schedule(1, day, CalendarScheduleStatus.CONFIRMED)))
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.DateSelected(other))
        vm.onEvent(RecordUiEvent.ServerScheduleClicked(1))

        assertNull(vm.nextEffect())
    }

    @Test
    fun 캘린더_배지는_상세_상태를_먼저_쓰고_당일은_확정으로_보여준다() {
        assertEquals("약속 확정" to true, scheduleLabel(CalendarScheduleStatus.CONFIRMED, ScheduleDisplayStatus.TODAY))
        assertEquals("약속 완료" to false, scheduleLabel(CalendarScheduleStatus.CONFIRMED, ScheduleDisplayStatus.COMPLETED))
        assertEquals("약속 예정" to false, scheduleLabel(CalendarScheduleStatus.NOT_CONFIRMED, null))
        assertEquals("약속 확정" to true, scheduleLabel(CalendarScheduleStatus.CONFIRMED, null))
        assertEquals("약속 취소" to false, scheduleLabel(CalendarScheduleStatus.CANCELLED, null))
    }
}
