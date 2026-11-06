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
    fun upcomingCardsShowServerSchedulesInOrderAndOpenTheirDetail() {
        val today = LocalDate.of(2026, 11, 15)
        fun card(id: Long, type: com.kiri.domain.model.schedule.UpcomingScheduleType, days: Long, title: String) =
            com.kiri.domain.model.schedule.UpcomingSchedule(
                id, today.plusDays(days), java.time.LocalTime.of(18, 0), type, days.toInt(), title, null,
            )
        val state = mutableStateOf(
            RecordUiState(
                today = today,
                upcomingSchedules = listOf(
                    card(120, com.kiri.domain.model.schedule.UpcomingScheduleType.RESPONSE_REQUIRED, 5, "광장시장 가기"),
                    card(118, com.kiri.domain.model.schedule.UpcomingScheduleType.UPCOMING, 2, "한강 축제 가기"),
                ),
            ),
        )
        val events = mutableListOf<RecordUiEvent>()
        compose.setContent { KiriTheme { RecordScreen(state.value, { events += it }) } }

        compose.onNodeWithTag("record_calendar").performScrollToKey("schedule-118")
        compose.onNodeWithText("응답 필요").assertExists()
        compose.onNodeWithText("약속 확정").assertExists()
        compose.onNodeWithText("일시 I 26.11.17 18:00 · D-2").assertExists()
        compose.onNodeWithTag("record_schedule_118").performClick()
        compose.runOnIdle {
            org.junit.Assert.assertEquals(listOf(RecordUiEvent.ServerScheduleClicked(118)), events)
        }

        state.value = state.value.copy(upcomingSchedules = emptyList(), upcomingFailed = true)
        compose.onNodeWithText("다가오는 약속을 불러오지 못했어요").performScrollTo().assertIsDisplayed()
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
    fun tappingADateOnlySelectsItAndDoesNotListThatDaysActivities() {
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
        val events = mutableListOf<RecordUiEvent>()
        compose.setContent {
            KiriTheme {
                RecordScreen(
                    RecordUiState(
                        today = date,
                        entries = entries,
                        calendarMarks = mapOf(date to RecordCalendarMark.PHOTO_FIRST),
                    ),
                    { events += it },
                )
            }
        }
        compose
            .onNodeWithTag("record_day_count_$date", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("+1")
        compose.onNodeWithTag("record_date_$date").performScrollTo().performClick()
        // 어느 활동을 열지는 ViewModel 이 정하므로 화면은 날짜 선택만 알린다
        compose.runOnIdle { org.junit.Assert.assertEquals(listOf(RecordUiEvent.DateSelected(date)), events) }
        compose.onNodeWithText("10월 13일의 활동").assertDoesNotExist()
        compose.onNodeWithText("오후 약속").assertDoesNotExist()
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
                bucketItems = com.kiri.data.bucket.FakeBucketItemRepository(),
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
        // 칩 개수는 서버가 준 가족 전체 기준 저장 개수다
        compose.onNodeWithText("엄마 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("아빠 2").performScrollTo().assertIsDisplayed()
        capture("record-bucket")
        compose.onNodeWithTag("record_saver_2").performClick().assertIsSelected()
        compose.onNodeWithTag("record_bucket_12").assertIsDisplayed()
        compose.onNodeWithTag("record_bucket_11").assertDoesNotExist()
        compose.onNodeWithTag("record_saver_1").performClick()
        compose.onNodeWithTag("record_bucket_11").assertIsDisplayed()
        compose.onNodeWithText("완료").assertIsDisplayed()
        compose.onNodeWithTag("record_bucket_12").assertDoesNotExist()
        compose.onNodeWithTag("record_tab_CALENDAR").performClick()
        compose.onNodeWithTag("record_date_2026-10-22").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("record_tab_BUCKET").performClick()
        compose.onNodeWithTag("record_saver_1").assertIsSelected()
    }

    @Test
    fun bucketListShowsLoadingAndRetryOnFailure() {
        val state = mutableStateOf(
            RecordUiState(today = LocalDate.of(2026, 10, 13), tab = RecordTab.BUCKET, bucketLoading = true),
        )
        val events = mutableListOf<RecordUiEvent>()
        compose.setContent { KiriTheme { RecordScreen(state.value, { events += it }) } }
        compose.onNodeWithTag("record_bucket_loading").assertExists()

        state.value = state.value.copy(bucketLoading = false, bucketError = "버킷리스트를 불러오지 못했어요.")
        compose.onNodeWithText("버킷리스트를 불러오지 못했어요.").assertIsDisplayed()
        compose.onNodeWithTag("record_bucket_retry").performClick()
        compose.runOnIdle {
            org.junit.Assert.assertEquals(listOf<RecordUiEvent>(RecordUiEvent.BucketListRetryClicked), events)
        }
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
