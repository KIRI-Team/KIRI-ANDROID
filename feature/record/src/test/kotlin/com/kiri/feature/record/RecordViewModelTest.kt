package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.feature.record.model.*
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RecordViewModelTest {
    private fun handle() = SavedStateHandle(mapOf("recordSeedDate" to "2026-10-13"))

    @Test fun monthGridIncludesEveryDayAndAlignsMondayFirstAcrossLeapYears() {
        for (month in listOf(YearMonth.of(2024, 2), YearMonth.of(2026, 2), YearMonth.of(2026, 8))) {
            val weeks = calendarWeeks(month)
            assertTrue(weeks.all { it.size == 7 })
            assertEquals((1..month.lengthOfMonth()).map(month::atDay), weeks.flatten().filterNotNull())
            weeks.flatten().forEachIndexed { index, date -> if (date != null) assertEquals(date.dayOfWeek.value - 1, index % 7) }
        }
        assertEquals(6, calendarWeeks(YearMonth.of(2026, 8)).size)
    }

    @Test fun yearBoundaryAndShortMonthClampSelectedDayWithoutLosingTabOrSaver() {
        val handle = SavedStateHandle(mapOf("recordSeedDate" to "2026-12-31"))
        val vm = RecordViewModel(handle)
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
        val vm = RecordViewModel(handle)
        vm.onEvent(RecordUiEvent.DateSelected(LocalDate.of(2026, 10, 22)))
        vm.onEvent(RecordUiEvent.SaverSelected("father"))
        vm.onEvent(RecordUiEvent.TabSelected(RecordTab.BUCKET))
        val restored = RecordViewModel(handle)
        assertEquals(vm.uiState.value, restored.uiState.value)
        restored.onEvent(RecordUiEvent.SaverSelected("unknown"))
        restored.onEvent(RecordUiEvent.DateSelected(LocalDate.of(2026, 11, 1)))
        assertEquals(vm.uiState.value, restored.uiState.value)
        assertEquals(2, restored.uiState.value.visibleBuckets.size)
        restored.onEvent(RecordUiEvent.SaverSelected(null))
        assertEquals(4, restored.uiState.value.visibleBuckets.size)
    }

    @Test fun cardEffectsCarryTheirItemIdsAndDoNotMutateTheFixtures() = runTest {
        val vm = RecordViewModel(handle())
        val original = vm.uiState.value
        vm.onEvent(RecordUiEvent.ScheduleClicked("schedule-1"))
        assertEquals(RecordUiEffect.OpenSchedule("schedule-1"), vm.effect.first())
        vm.onEvent(RecordUiEvent.BucketClicked("bucket-2"))
        assertEquals(RecordUiEffect.OpenBucket("bucket-2"), vm.effect.first())
        assertEquals(original, vm.uiState.value)
    }
}
