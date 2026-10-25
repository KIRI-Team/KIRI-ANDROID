package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.feature.record.model.*
import com.kiri.feature.record.presentation.sheet.recordCalendarWeeks
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.record.FakeRecordRepository
import com.kiri.data.mission.FakeMissionRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModelTest {
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }
    private fun repository() = FakeRecordRepository(FakeHomeRepository(Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC), latencyMillis = 0), FakeMissionRepository())
    private fun handle() = SavedStateHandle(mapOf("recordSeedDate" to "2026-10-13"))

    @Test fun monthGridIncludesEveryDayAndAlignsMondayFirstAcrossLeapYears() {
        for (month in listOf(YearMonth.of(2024, 2), YearMonth.of(2026, 2), YearMonth.of(2026, 8))) {
            val weeks = recordCalendarWeeks(month)
            assertTrue(weeks.all { it.size == 7 })
            assertEquals((1..month.lengthOfMonth()).map(month::atDay), weeks.flatten().filterNotNull().filter { it.isInMonth }.map { it.date })
            weeks.flatten().forEachIndexed { index, day -> if (day != null) assertEquals(day.date.dayOfWeek.value - 1, index % 7) }
        }
        assertEquals(6, recordCalendarWeeks(YearMonth.of(2026, 8)).size)
    }

    @Test fun yearBoundaryAndShortMonthClampSelectedDayWithoutLosingTabOrSaver() {
        val handle = SavedStateHandle(mapOf("recordSeedDate" to "2026-12-31"))
        val vm = RecordViewModel(handle, repository())
        vm.onEvent(RecordUiEvent.TabSelected(RecordTab.BUCKET))
        vm.onEvent(RecordUiEvent.SaverSelected("father"))
        vm.onEvent(RecordUiEvent.NextMonth)
        assertEquals(YearMonth.of(2027, 1), vm.uiState.value.month)
        assertEquals(LocalDate.of(2027, 1, 31), vm.uiState.value.selectedDate)
        vm.onEvent(RecordUiEvent.NextMonth)
        assertEquals(LocalDate.of(2027, 2, 28), vm.uiState.value.selectedDate)
        assertEquals(RecordTab.BUCKET, vm.uiState.value.tab)
        assertEquals("father", vm.uiState.value.selectedSaverId)
    }

    @Test fun savedUiChoicesRestoreAndUnknownSaversOrOutOfMonthDatesAreIgnored() {
        val handle = handle()
        val vm = RecordViewModel(handle, repository())
        vm.onEvent(RecordUiEvent.DateSelected(LocalDate.of(2026, 10, 22)))
        vm.onEvent(RecordUiEvent.SaverSelected("father"))
        vm.onEvent(RecordUiEvent.TabSelected(RecordTab.BUCKET))
        val restored = RecordViewModel(handle, repository())
        assertEquals(vm.uiState.value, restored.uiState.value)
        restored.onEvent(RecordUiEvent.SaverSelected("unknown"))
        restored.onEvent(RecordUiEvent.DateSelected(LocalDate.of(2026, 11, 1)))
        assertEquals(vm.uiState.value, restored.uiState.value)
        assertEquals(1, restored.uiState.value.visibleBuckets.size)
        restored.onEvent(RecordUiEvent.SaverSelected(null))
        assertEquals(1, restored.uiState.value.visibleBuckets.size)
    }

    @Test fun requestedScheduleKeepsPlannedCalendarMarkerButIsExcludedFromConfirmedAppointments() = runTest {
        val repo = repository()
        val vm = RecordViewModel(handle(), repo)
        val date = LocalDate.of(2026, 10, 20)
        repo.requestSchedule(com.kiri.domain.record.ScheduleRequest("record-bucket", date, java.time.LocalTime.of(14, 0),
            setOf("mother"), "", com.kiri.domain.record.ScheduleRequestChannel.APP))

        val state = vm.uiState.value
        assertTrue(state.buckets.none { it.id == "record-bucket" })
        assertTrue(state.schedules.none { it.date == date })
        assertEquals(RecordCalendarMark.PLANNED, state.calendarMarks[date])
        // 원래 있던 확정 일정은 그대로 확정이다
        assertTrue(state.schedules.filter { it.date != date }.all { it.status == RecordScheduleStatus.CONFIRMED })
    }

    @Test fun cardEffectsCarryTheirItemIdsAndDoNotMutateTheFixtures() = runTest {
        val vm = RecordViewModel(handle(), repository())
        val original = vm.uiState.value
        vm.onEvent(RecordUiEvent.ScheduleClicked("schedule-demo"))
        assertEquals(RecordUiEffect.OpenSchedule("schedule-demo"), vm.effect.first())
        vm.onEvent(RecordUiEvent.BucketClicked("record-bucket"))
        assertEquals(RecordUiEffect.OpenBucket("record-bucket"), vm.effect.first())
        vm.onEvent(RecordUiEvent.CalendarEntryClicked("record-memory-1"))
        assertEquals(RecordUiEffect.OpenEntry("record-memory-1"), vm.effect.first())
        assertEquals(original, vm.uiState.value)
    }
}
