package com.kiri.feature.record.presentation

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.feature.record.RecordViewModel
import com.kiri.data.record.FakeRecordRepository
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.mission.FakeMissionRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import com.kiri.feature.record.model.*
import com.kiri.uikit.KiriTheme
import java.io.File
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class RecordScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun multipleActivitiesOnOneDateRequireAnExplicitActivityChoice() {
        val date = LocalDate.of(2026, 10, 13)
        val entries = listOf(
            com.kiri.domain.record.CalendarEntry("photo", "river", date, com.kiri.domain.record.CalendarEntryKind.MEMORY, "오전 활동", java.time.LocalTime.of(10, 0)),
            com.kiri.domain.record.CalendarEntry("schedule", "river", date, com.kiri.domain.record.CalendarEntryKind.SCHEDULE, "오후 약속", java.time.LocalTime.of(15, 0)),
        )
        val state = mutableStateOf(RecordUiState(today = date, entries = entries, calendarMarks = mapOf(date to RecordCalendarMark.PHOTO_FIRST)))
        var opened: String? = null
        compose.setContent { KiriTheme { RecordScreen(state.value, { event -> when (event) {
            is RecordUiEvent.DateSelected -> state.value = state.value.copy(selectedDate = event.date)
            is RecordUiEvent.CalendarEntryClicked -> opened = event.id
            else -> Unit
        } }) } }
        compose.onNodeWithTag("record_date_$date").performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertNull(opened) }
        compose.onNodeWithTag("record_day_count_$date", useUnmergedTree = true).assertIsDisplayed().assertTextEquals("+1")
        compose.onNodeWithTag("record_day_schedule").performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals("schedule", opened) }
    }
    private fun start() {
        val repo = FakeRecordRepository(FakeHomeRepository(Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC), latencyMillis = 0), FakeMissionRepository())
        val vm = RecordViewModel(SavedStateHandle(mapOf("recordSeedDate" to "2026-10-13")), repo)
        compose.setContent { KiriTheme { RecordRoute(viewModel = vm) } }
    }

    @Test fun calendarStartsSelectedAndMonthDateAndBucketFilterControlsWork() {
        start()
        compose.onNodeWithTag("record_tab_CALENDAR").assertIsSelected()
        compose.onNodeWithTag("record_month").assertTextEquals("2026년 10월")
        capture("record-calendar")
        compose.onNodeWithTag("record_date_2026-10-22").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("record_previous_month").performScrollTo().performClick()
        compose.onNodeWithTag("record_month").assertTextEquals("2026년 9월")
        compose.onNodeWithTag("record_next_month").performClick()
        compose.onNodeWithTag("record_date_2026-10-22").assertIsSelected()
        compose.onNodeWithTag("record_tab_BUCKET").performClick()
        compose.onNodeWithTag("record_bucket_list").assertIsDisplayed()
        compose.onNodeWithText("마마 0").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("빠빠 1").performScrollTo().assertIsDisplayed()
        capture("record-bucket")
        compose.onNodeWithTag("record_saver_father").performClick().assertIsSelected()
        compose.onNodeWithTag("record_bucket_record-bucket").assertIsDisplayed()
        compose.onNodeWithTag("record_saver_mother").performClick()
        compose.onNodeWithText("저장한 활동이 없어요").assertIsDisplayed()
        compose.onNodeWithTag("record_bucket_record-bucket").assertDoesNotExist()
        compose.onNodeWithTag("record_tab_CALENDAR").performClick()
        compose.onNodeWithTag("record_date_2026-10-22").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("record_tab_BUCKET").performClick()
        compose.onNodeWithTag("record_saver_mother").assertIsSelected()
    }

    @Test fun sixWeekMonthAndEmptyListsRemainScrollableAndDoNotClipControls() {
        val state = mutableStateOf(RecordUiState(today = LocalDate.of(2026, 8, 31)))
        compose.setContent { KiriTheme { RecordScreen(state.value, { event ->
            if (event is RecordUiEvent.TabSelected) state.value = state.value.copy(tab = event.tab)
        }) } }
        compose.onNodeWithTag("record_date_2026-08-31").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("다가오는 약속이 없어요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("record_tab_BUCKET").assertIsDisplayed()
        compose.onNodeWithTag("record_tab_BUCKET").performClick()
        compose.onNodeWithText("저장한 활동이 없어요").assertIsDisplayed()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val root = automation.rootInActiveWindow
        if (root?.packageName?.toString() == "android") {
            root.findAccessibilityNodeInfosByText("OK").forEach { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
            Thread.sleep(300)
            compose.waitForIdle()
        }
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
