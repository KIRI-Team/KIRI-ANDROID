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
import com.kiri.domain.model.schedule.UpcomingSchedule
import com.kiri.domain.model.schedule.UpcomingScheduleType
import com.kiri.domain.model.schedule.UpcomingSchedules
import com.kiri.domain.repository.UpcomingScheduleRepository
import com.kiri.feature.record.model.RecordUiEffect
import com.kiri.feature.record.presentation.calendar.scheduleLabel
import com.kiri.domain.repository.CalendarScheduleRepository
import com.kiri.feature.record.model.RecordCalendarMark
import com.kiri.feature.record.model.RecordUiEvent
import com.kiri.feature.record.model.mergeCalendarMarks
import com.kiri.feature.record.model.toCalendarMarks
import com.kiri.feature.record.model.toCalendarPhotos
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
    fun 리뷰_대표_이미지가_있는_완료_일정은_그_사진을_날짜_배경으로_쓴다() {
        val (withImage, withoutImage, confirmed) = emptyDays(october)
        val repo = RecordingCalendarSchedules()
        repo.results[october] = Result.success(
            listOf(
                schedule(1, withImage, CalendarScheduleStatus.COMPLETED).copy(reviewImageUrl = "https://cdn/review/1.jpg"),
                schedule(2, withoutImage, CalendarScheduleStatus.COMPLETED),
                schedule(3, confirmed, CalendarScheduleStatus.CONFIRMED),
            ),
        )

        val state = viewModel(repo).uiState.value

        // 사진 기록처럼 날짜 위에 사진을 깔고 날짜 숫자를 보여준다
        assertEquals(RecordCalendarMark.PHOTO_FIRST, state.calendarMarks[withImage])
        assertEquals(com.kiri.domain.record.RecordPhoto("schedule-1", "https://cdn/review/1.jpg"), state.calendarPhotos[withImage])
        // 이미지가 없는 완료 일정과 확정 일정은 지금처럼 확정 표시만 한다
        assertEquals(RecordCalendarMark.CONFIRMED, state.calendarMarks[withoutImage])
        assertEquals(null, state.calendarPhotos[withoutImage])
        assertEquals(null, state.calendarPhotos[confirmed])
    }

    @Test
    fun 한_날에_사진_있는_완료_일정이_여럿이면_이른_시간_일정의_사진을_쓴다() {
        val day = october.atDay(1)
        val photos = listOf(
            CalendarSchedule(5, day, LocalTime.of(15, 0), CalendarScheduleStatus.COMPLETED, MyScheduleStatus.ACCEPTED, "https://cdn/late.jpg"),
            CalendarSchedule(6, day, LocalTime.of(9, 0), CalendarScheduleStatus.COMPLETED, MyScheduleStatus.ACCEPTED, "https://cdn/early.jpg"),
        ).toCalendarPhotos()

        assertEquals("https://cdn/early.jpg", photos[day]?.source)
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
        assertEquals(RecordCalendarMark.PLANNED, marks(listOf(CalendarScheduleStatus.CANCELLED, CalendarScheduleStatus.PENDING_RESPONSE)))
        assertEquals(RecordCalendarMark.CANCELLED, marks(listOf(CalendarScheduleStatus.CANCELLED, CalendarScheduleStatus.CANCELLED)))
        // 미확정은 열 수 없어 취소처럼 흐린 시계로 보여준다
        assertEquals(RecordCalendarMark.CANCELLED, marks(listOf(CalendarScheduleStatus.NOT_CONFIRMED)))
        assertEquals(RecordCalendarMark.CANCELLED, marks(listOf(CalendarScheduleStatus.CANCELLED, CalendarScheduleStatus.NOT_CONFIRMED)))
        assertEquals(RecordCalendarMark.PLANNED, marks(listOf(CalendarScheduleStatus.NOT_CONFIRMED, CalendarScheduleStatus.PENDING_RESPONSE)))
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
    fun 서버_일정이_여러_개면_가장_이른_일정으로_가고_취소만_있는_날은_이동하지_않는다() = runTest {
        val (multiple, cancelledOnly) = emptyDays(october)
        val repo = RecordingCalendarSchedules()
        repo.results[october] = Result.success(
            listOf(
                schedule(1, multiple, CalendarScheduleStatus.CONFIRMED).copy(time = LocalTime.of(15, 0)),
                schedule(2, multiple, CalendarScheduleStatus.PENDING_RESPONSE).copy(time = LocalTime.of(9, 0)),
                schedule(4, multiple, CalendarScheduleStatus.CONFIRMED).copy(time = null),
                schedule(3, cancelledOnly, CalendarScheduleStatus.CANCELLED),
            ),
        )
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.DateSelected(cancelledOnly))
        assertNull(vm.nextEffect())

        vm.onEvent(RecordUiEvent.DateSelected(multiple))
        assertEquals(RecordUiEffect.OpenServerSchedule(2, multiple), vm.nextEffect())
    }

    @Test
    fun 미확정_일정은_눌러도_열지_않는다() = runTest {
        val (unconfirmedOnly, mixed) = emptyDays(october)
        val repo = RecordingCalendarSchedules()
        repo.results[october] = Result.success(
            listOf(
                schedule(1, unconfirmedOnly, CalendarScheduleStatus.NOT_CONFIRMED),
                // 같은 날 더 이른 미확정 일정이 있어도 열 수 있는 일정으로 간다
                schedule(2, mixed, CalendarScheduleStatus.NOT_CONFIRMED).copy(time = LocalTime.of(9, 0)),
                schedule(3, mixed, CalendarScheduleStatus.CONFIRMED).copy(time = LocalTime.of(15, 0)),
            ),
        )
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.DateSelected(unconfirmedOnly))
        assertNull(vm.nextEffect())

        vm.onEvent(RecordUiEvent.DateSelected(mixed))
        assertEquals(RecordUiEffect.OpenServerSchedule(3, mixed), vm.nextEffect())
    }

    @Test
    fun 기기에_저장된_활동이_있는_날은_시간이_더_이른_활동을_연다() = runTest {
        val vm0 = viewModel(RecordingCalendarSchedules())
        val busy = vm0.uiState.value.entries.first { YearMonth.from(it.date) == october }.date
        val repo = RecordingCalendarSchedules()
        // 시간을 모르는 서버 일정은 기기에 저장된 활동보다 뒤로 미룬다
        repo.results[october] = Result.success(listOf(schedule(1, busy, CalendarScheduleStatus.CONFIRMED).copy(time = null)))
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.DateSelected(busy))

        assertEquals(RecordUiEffect.OpenEntry(vm.uiState.value.selectedEntries.first().id), vm.nextEffect())
    }

    @Test
    fun 다른_달의_날짜는_골라도_이동하지_않는다() = runTest {
        val day = emptyDays(october).first()
        val repo = RecordingCalendarSchedules()
        repo.results[october] = Result.success(listOf(schedule(1, day, CalendarScheduleStatus.CONFIRMED)))
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.DateSelected(november.atDay(1)))

        assertNull(vm.nextEffect())
        assertEquals(today, vm.uiState.value.selectedDate)
    }

    // 다가오는 일정 조회 결과를 정해 두고, 몇 번 불렸는지 센다
    private class FakeUpcoming(var result: Result<UpcomingSchedules>) : UpcomingScheduleRepository {
        var calls = 0

        override suspend fun getUpcomingSchedules(): Result<UpcomingSchedules> {
            calls++
            return result
        }
    }

    private fun upcomingCard(id: Long, type: UpcomingScheduleType, date: LocalDate) =
        UpcomingSchedule(id, date, LocalTime.of(18, 0), type, 3, "약속 $id", MyScheduleStatus.PENDING)

    @Test
    fun 다가오는_약속은_응답_필요_카드를_먼저_보여주고_누르면_상세로_간다() = runTest {
        val required = upcomingCard(120, UpcomingScheduleType.RESPONSE_REQUIRED, today.plusDays(5))
        val upcoming = upcomingCard(118, UpcomingScheduleType.UPCOMING, today.plusDays(2))
        val vm = RecordViewModel(
            SavedStateHandle(), records(), clock, RecordingCalendarSchedules(),
            upcoming = FakeUpcoming(Result.success(UpcomingSchedules(required, upcoming))),
        )

        assertEquals(listOf(required, upcoming), vm.uiState.value.upcomingSchedules)
        assertEquals(false, vm.uiState.value.upcomingFailed)

        vm.onEvent(RecordUiEvent.ServerScheduleClicked(118))
        assertEquals(RecordUiEffect.OpenServerSchedule(118, today.plusDays(2)), vm.nextEffect())
        // 카드에 없는 일정은 열지 않는다
        vm.onEvent(RecordUiEvent.ServerScheduleClicked(999))
        assertNull(vm.nextEffect())
    }

    @Test
    fun 다가오는_약속을_불러오지_못하면_실패로_두고_다시_시도하면_새로_불러온다() = runTest {
        val repo = FakeUpcoming(Result.failure(IOException()))
        val vm = RecordViewModel(SavedStateHandle(), records(), clock, RecordingCalendarSchedules(), upcoming = repo)

        assertEquals(true, vm.uiState.value.upcomingFailed)
        assertEquals(emptyList<UpcomingSchedule>(), vm.uiState.value.upcomingSchedules)

        val card = upcomingCard(1, UpcomingScheduleType.TODAY, today)
        repo.result = Result.success(UpcomingSchedules(upcoming = card))
        vm.onEvent(RecordUiEvent.RetryClicked)

        assertEquals(2, repo.calls)
        assertEquals(false, vm.uiState.value.upcomingFailed)
        assertEquals(listOf(card), vm.uiState.value.upcomingSchedules)
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
