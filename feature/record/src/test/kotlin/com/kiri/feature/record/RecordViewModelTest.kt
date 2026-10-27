package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.record.FakeRecordRepository
import com.kiri.feature.record.model.*
import com.kiri.feature.record.presentation.sheet.recordCalendarWeeks
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModelTest {
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refreshingAcrossKoreanMidnightUpdatesTodayWithoutOverwritingBrowsedDate() = runTest {
        var instant = Instant.parse("2026-10-25T14:59:59Z")
        val clock =
            object : Clock() {
                override fun getZone(): java.time.ZoneId = ZoneOffset.UTC

                override fun instant(): Instant = instant

                override fun withZone(zone: java.time.ZoneId): Clock = Clock.fixed(instant, zone)
            }
        val repo =
            FakeRecordRepository(
                FakeHomeRepository(clock, latencyMillis = 0),
                FakeMissionRepository(),
                clock = clock,
            )
        // An older saved seed must not freeze the device's definition of today.
        val vm =
            RecordViewModel(SavedStateHandle(mapOf("recordSeedDate" to "2020-01-01")), repo, clock)
        assertEquals(LocalDate.of(2026, 10, 25), vm.uiState.value.today)
        instant = instant.plusSeconds(2)
        vm.onEvent(RecordUiEvent.RetryClicked)
        assertEquals(LocalDate.of(2026, 10, 26), vm.uiState.value.today)
        assertEquals(LocalDate.of(2026, 10, 26), vm.uiState.value.selectedDate)
        vm.onEvent(RecordUiEvent.DateSelected(LocalDate.of(2026, 10, 10)))
        instant = instant.plusSeconds(86400)
        vm.onEvent(RecordUiEvent.RetryClicked)
        assertEquals(LocalDate.of(2026, 10, 27), vm.uiState.value.today)
        assertEquals(LocalDate.of(2026, 10, 10), vm.uiState.value.selectedDate)
    }

    private fun repository() =
        FakeRecordRepository(
            FakeHomeRepository(
                Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC),
                latencyMillis = 0,
            ),
            FakeMissionRepository(),
            clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC),
        )

    private fun testClock(handle: SavedStateHandle): Clock =
        Clock.fixed(
            LocalDate.parse(handle.get<String>("recordSeedDate") ?: "2026-10-13")
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant(),
            ZoneOffset.UTC,
        )

    private fun handle() = SavedStateHandle(mapOf("recordSeedDate" to "2026-10-13"))

    @Test
    fun monthGridIncludesEveryDayAndAlignsMondayFirstAcrossLeapYears() {
        for (month in listOf(YearMonth.of(2024, 2), YearMonth.of(2026, 2), YearMonth.of(2026, 8))) {
            val weeks = recordCalendarWeeks(month)
            assertTrue(weeks.all { it.size == 7 })
            assertEquals(
                (1..month.lengthOfMonth()).map(month::atDay),
                weeks.flatten().filterNotNull().filter { it.isInMonth }.map { it.date },
            )
            weeks.flatten().forEachIndexed { index, day ->
                if (day != null) assertEquals(day.date.dayOfWeek.value - 1, index % 7)
            }
        }
        assertEquals(6, recordCalendarWeeks(YearMonth.of(2026, 8)).size)
    }

    @Test
    fun yearBoundaryAndShortMonthClampSelectedDayWithoutLosingTabOrSaver() {
        val handle = SavedStateHandle(mapOf("recordSeedDate" to "2026-12-31"))
        val vm = RecordViewModel(handle, repository(), testClock(handle))
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

    @Test
    fun savedUiChoicesRestoreAndUnknownSaversOrOutOfMonthDatesAreIgnored() {
        val handle = handle()
        val vm = RecordViewModel(handle, repository(), testClock(handle))
        vm.onEvent(RecordUiEvent.DateSelected(LocalDate.of(2026, 10, 22)))
        vm.onEvent(RecordUiEvent.SaverSelected("father"))
        vm.onEvent(RecordUiEvent.TabSelected(RecordTab.BUCKET))
        val restored = RecordViewModel(handle, repository(), testClock(handle))
        assertEquals(vm.uiState.value, restored.uiState.value)
        restored.onEvent(RecordUiEvent.SaverSelected("unknown"))
        restored.onEvent(RecordUiEvent.DateSelected(LocalDate.of(2026, 11, 1)))
        assertEquals(vm.uiState.value, restored.uiState.value)
        assertEquals(1, restored.uiState.value.visibleBuckets.size)
        restored.onEvent(RecordUiEvent.SaverSelected(null))
        assertEquals(1, restored.uiState.value.visibleBuckets.size)
    }

    @Test
    fun requestedScheduleKeepsPlannedCalendarMarkerButIsExcludedFromConfirmedAppointments() =
        runTest {
            val repo = repository()
            val vm = RecordViewModel(handle(), repo, testClock(handle()))
            val date = LocalDate.of(2026, 10, 20)
            repo.requestSchedule(
                com.kiri.domain.record.ScheduleRequest(
                    "record-bucket",
                    date,
                    java.time.LocalTime.of(14, 0),
                    setOf("mother"),
                    "",
                    com.kiri.domain.record.ScheduleRequestChannel.APP,
                )
            )

            val state = vm.uiState.value
            assertTrue(state.buckets.none { it.id == "record-bucket" })
            assertTrue(state.schedules.none { it.date == date })
            assertEquals(RecordCalendarMark.PLANNED, state.calendarMarks[date])
            // 원래 있던 확정 일정은 그대로 확정이다
            assertTrue(
                state.schedules
                    .filter { it.date != date }
                    .all { it.status == RecordScheduleStatus.CONFIRMED }
            )
        }

    @Test
    fun cardEffectsCarryTheirItemIdsAndDoNotMutateTheFixtures() = runTest {
        val vm = RecordViewModel(handle(), repository(), testClock(handle()))
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
