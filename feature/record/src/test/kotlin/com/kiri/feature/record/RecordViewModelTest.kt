package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.question.model.RecordedQuestion
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
        val repo = FakeQuestionHistory()
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

    private fun repository() = FakeQuestionHistory()

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
        val vm = RecordViewModel(handle, repository(), testClock(handle), bucketItems = com.kiri.data.bucket.FakeBucketItemRepository())
        vm.onEvent(RecordUiEvent.TabSelected(RecordTab.BUCKET))
        vm.onEvent(RecordUiEvent.SaverSelected("2"))
        vm.onEvent(RecordUiEvent.NextMonth)
        assertEquals(YearMonth.of(2027, 1), vm.uiState.value.month)
        assertEquals(LocalDate.of(2027, 1, 31), vm.uiState.value.selectedDate)
        vm.onEvent(RecordUiEvent.NextMonth)
        assertEquals(LocalDate.of(2027, 2, 28), vm.uiState.value.selectedDate)
        assertEquals(RecordTab.BUCKET, vm.uiState.value.tab)
        assertEquals("2", vm.uiState.value.selectedSaverId)
    }

    @Test
    fun savedUiChoicesRestoreAndUnknownSaversOrOutOfMonthDatesAreIgnored() {
        val handle = handle()
        val buckets = com.kiri.data.bucket.FakeBucketItemRepository()
        val vm = RecordViewModel(handle, repository(), testClock(handle), bucketItems = buckets)
        vm.onEvent(RecordUiEvent.DateSelected(LocalDate.of(2026, 10, 22)))
        vm.onEvent(RecordUiEvent.SaverSelected("2"))
        vm.onEvent(RecordUiEvent.TabSelected(RecordTab.BUCKET))
        val restored = RecordViewModel(handle, repository(), testClock(handle), bucketItems = buckets)
        assertEquals(vm.uiState.value, restored.uiState.value)
        restored.onEvent(RecordUiEvent.SaverSelected("unknown"))
        restored.onEvent(RecordUiEvent.DateSelected(LocalDate.of(2026, 11, 1)))
        assertEquals(vm.uiState.value, restored.uiState.value)
        // 서버가 아빠(2) 것만 걸러 준다
        assertEquals(listOf(12L, 10L), restored.uiState.value.bucketItems.map { it.id })
        restored.onEvent(RecordUiEvent.SaverSelected(null))
        assertEquals(3, restored.uiState.value.bucketItems.size)
    }

    @Test
    fun 가족_모두_답한_지난_질문만_캘린더에_질문으로_표시한다() {
        fun question(date: LocalDate, everyone: Boolean) =
            RecordedQuestion(date, "질문", mapOf("1" to "답"), mapOf("1" to "엄마"), everyone, "q-$date")
        val answered = LocalDate.of(2026, 10, 5)
        val partial = LocalDate.of(2026, 10, 6)
        val history = FakeQuestionHistory(mapOf(answered to question(answered, true), partial to question(partial, false)))

        val vm = RecordViewModel(handle(), history, testClock(handle()))

        assertEquals(RecordCalendarMark.QUESTION, vm.uiState.value.calendarMarks[answered])
        assertNull(vm.uiState.value.calendarMarks[partial])
        assertEquals(setOf(answered, partial), vm.uiState.value.questions.keys)
    }

    @Test
    fun 새로_고치면_질문_기록도_새로_받는다() {
        val history = FakeQuestionHistory()
        val vm = RecordViewModel(handle(), history, testClock(handle()))
        val before = history.refreshCount

        vm.onEvent(RecordUiEvent.RetryClicked)

        assertEquals(before + 1, history.refreshCount)
    }

    @Test
    fun cardEffectsCarryTheirItemIdsAndDoNotMutateTheFixtures() = runTest {
        val vm = RecordViewModel(handle(), repository(), testClock(handle()), bucketItems = com.kiri.data.bucket.FakeBucketItemRepository())
        val original = vm.uiState.value
        vm.onEvent(RecordUiEvent.BucketClicked("12"))
        assertEquals(RecordUiEffect.OpenBucket("12"), vm.effect.first())
        assertEquals(original, vm.uiState.value)
    }
}
