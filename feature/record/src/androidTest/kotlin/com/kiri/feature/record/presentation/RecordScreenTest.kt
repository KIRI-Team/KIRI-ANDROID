package com.kiri.feature.record.presentation

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.record.FakeRecordRepository
import com.kiri.feature.record.RecordViewModel
import com.kiri.feature.record.model.*
import com.kiri.uikit.KiriTheme
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test

class RecordScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun selectedDayListsServerSchedulesAndOnlyOpenableOnesCanBeTapped() {
        val date = LocalDate.of(2026, 10, 13)
        fun schedule(id: Long, status: com.kiri.domain.model.schedule.CalendarScheduleStatus) =
            com.kiri.domain.model.schedule.CalendarSchedule(id, date, java.time.LocalTime.of(14, 50), status, null)
        val events = mutableListOf<RecordUiEvent>()
        compose.setContent {
            KiriTheme {
                RecordScreen(
                    RecordUiState(
                        today = date,
                        monthSchedules =
                            listOf(
                                schedule(1, com.kiri.domain.model.schedule.CalendarScheduleStatus.CONFIRMED),
                                schedule(2, com.kiri.domain.model.schedule.CalendarScheduleStatus.CANCELLED),
                            ),
                    ),
                    { events += it },
                )
            }
        }
        compose.onNodeWithTag("record_calendar").performScrollToKey("server-schedule-2")
        compose.onNodeWithText("약속 취소").assertExists()
        compose.onNodeWithTag("record_server_schedule_2").assertIsNotEnabled()
        compose.onNodeWithTag("record_calendar").performScrollToKey("server-schedule-1")
        compose.onNodeWithTag("record_server_schedule_1").performClick()
        compose.runOnIdle {
            org.junit.Assert.assertEquals(listOf(RecordUiEvent.ServerScheduleClicked(1)), events)
        }
    }

    @Test
    fun cancelledScheduleDayIsShownDimmedWithItsOwnDescription() {
        val date = LocalDate.of(2026, 10, 13)
        compose.setContent {
            KiriTheme {
                RecordScreen(
                    RecordUiState(today = date, calendarMarks = mapOf(date to RecordCalendarMark.CANCELLED)),
                    {},
                )
            }
        }
        compose.onNodeWithTag("record_cancelled_$date", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("2026년 10월 13일, 취소된 약속").assertExists()
    }

    @Test
    fun multipleActivitiesOnOneDateRequireAnExplicitActivityChoice() {
        val date = LocalDate.of(2026, 10, 13)
        val entries =
            listOf(
                com.kiri.domain.record.CalendarEntry(
                    "photo",
                    "river",
                    date,
                    com.kiri.domain.record.CalendarEntryKind.MEMORY,
                    "오전 활동",
                    java.time.LocalTime.of(10, 0),
                ),
                com.kiri.domain.record.CalendarEntry(
                    "schedule",
                    "river",
                    date,
                    com.kiri.domain.record.CalendarEntryKind.SCHEDULE,
                    "오후 약속",
                    java.time.LocalTime.of(15, 0),
                ),
            )
        val state =
            mutableStateOf(
                RecordUiState(
                    today = date,
                    entries = entries,
                    calendarMarks = mapOf(date to RecordCalendarMark.PHOTO_FIRST),
                )
            )
        var opened: String? = null
        compose.setContent {
            KiriTheme {
                RecordScreen(
                    state.value,
                    { event ->
                        when (event) {
                            is RecordUiEvent.DateSelected ->
                                state.value = state.value.copy(selectedDate = event.date)
                            is RecordUiEvent.CalendarEntryClicked -> opened = event.id
                            else -> Unit
                        }
                    },
                )
            }
        }
        compose.onNodeWithTag("record_date_$date").performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertNull(opened) }
        compose
            .onNodeWithTag("record_day_count_$date", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("+1")
        compose.onNodeWithTag("record_day_schedule").performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals("schedule", opened) }
    }

    private fun start() {
        val repo =
            FakeRecordRepository(
                FakeHomeRepository(
                    Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC),
                    latencyMillis = 0,
                ),
                FakeMissionRepository(),
                clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC),
            )
        val vm =
            RecordViewModel(
                SavedStateHandle(),
                repo,
                Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC),
            )
        compose.setContent { KiriTheme { RecordRoute(viewModel = vm) } }
    }

    @Test
    fun calendarStartsSelectedAndMonthDateAndBucketFilterControlsWork() {
        start()
        compose.onNodeWithTag("record_tab_CALENDAR").assertIsSelected()
        compose.onNodeWithTag("record_month").assertTextEquals("2026년 10월")
        capture("record-calendar")
        compose
            .onNodeWithTag("record_date_2026-10-22")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
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

    @Test
    fun sixWeekMonthAndEmptyListsRemainScrollableAndDoNotClipControls() {
        val state = mutableStateOf(RecordUiState(today = LocalDate.of(2026, 8, 31)))
        compose.setContent {
            KiriTheme {
                RecordScreen(
                    state.value,
                    { event ->
                        if (event is RecordUiEvent.TabSelected)
                            state.value = state.value.copy(tab = event.tab)
                    },
                )
            }
        }
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
            root.findAccessibilityNodeInfosByText("OK").forEach {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            Thread.sleep(300)
            compose.waitForIdle()
        }
        val directory =
            InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
                ?: InstrumentationRegistry.getInstrumentation()
                    .targetContext
                    .getExternalFilesDir(null)!!
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
