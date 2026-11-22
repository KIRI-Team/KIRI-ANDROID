package com.kiri.feature.record.presentation

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.domain.record.QuestionHistoryRepository
import com.kiri.domain.record.RecordedQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import com.kiri.feature.record.RecordViewModel
import com.kiri.feature.record.model.*
import com.kiri.uikit.KiriTheme
import java.io.File
import java.time.Clock
import java.time.Instant
import com.kiri.domain.model.bucket.BucketItemStatus
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
        compose.onNodeWithText("약속 예정").assertExists()
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
        val today = LocalDate.of(2026, 10, 13)
        val date = LocalDate.of(2026, 10, 14)
        compose.setContent {
            KiriTheme {
                RecordScreen(
                    RecordUiState(today = today, calendarMarks = mapOf(date to RecordCalendarMark.CANCELLED)),
                    {},
                )
            }
        }
        compose.onNodeWithTag("record_cancelled_$date", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("2026년 10월 14일, 취소·미확정 약속").assertExists()
        // 오늘 날짜만 오늘로 읽어준다 (초록 배경도 오늘에만 쓴다)
        compose.onNodeWithContentDescription("2026년 10월 13일, 오늘").assertExists()
    }

    @Test
    fun tappingADateOnlySelectsItAndDoesNotListThatDaysActivities() {
        val date = LocalDate.of(2026, 10, 13)
        val events = mutableListOf<RecordUiEvent>()
        compose.setContent {
            KiriTheme {
                RecordScreen(
                    RecordUiState(
                        today = date,
                        calendarMarks = mapOf(date to RecordCalendarMark.PHOTO_FIRST),
                    ),
                    { events += it },
                )
            }
        }
        compose.onNodeWithTag("record_date_$date").performScrollTo().performClick()
        // 어느 활동을 열지는 ViewModel 이 정하므로 화면은 날짜 선택만 알린다
        compose.runOnIdle { org.junit.Assert.assertEquals(listOf(RecordUiEvent.DateSelected(date)), events) }
        compose.onNodeWithText("10월 13일의 활동").assertDoesNotExist()
        compose.onNodeWithText("오후 약속").assertDoesNotExist()
    }

    // 완료 후 리뷰 사진이 있는 날은 사진 위에 날짜 숫자를 보여준다 (Figma 10693:9281 "완료후 리뷰있는 약속")
    @Test
    fun reviewPhotoDayShowsThePhotoWithTheDayNumber() {
        val date = LocalDate.of(2026, 10, 13)
        compose.setContent {
            KiriTheme {
                RecordScreen(
                    RecordUiState(
                        today = date.plusDays(5),
                        calendarMarks = mapOf(date to RecordCalendarMark.PHOTO_FIRST),
                        calendarPhotos = mapOf(date to com.kiri.domain.record.RecordPhoto("schedule-1", "demo:group")),
                    ),
                    {},
                )
            }
        }
        compose.onNodeWithTag("record_date_$date").performScrollTo()
        compose.onNodeWithTag("record_day_photo_$date", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("record_day_number_$date", useUnmergedTree = true).assertTextEquals("13")
    }

    private fun start() {
        // 모아 둔 지난 질문이 없는 기록
        val repo = object : QuestionHistoryRepository {
            override val questions = MutableStateFlow<Map<LocalDate, RecordedQuestion>>(emptyMap())
            override suspend fun refresh() {}
        }
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
        compose.onNodeWithText("완료된 일정").assertIsDisplayed()
        compose.onNodeWithTag("record_bucket_12").assertDoesNotExist()
        compose.onNodeWithTag("record_tab_CALENDAR").performClick()
        compose.onNodeWithTag("record_date_2026-10-22").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("record_tab_BUCKET").performClick()
        compose.onNodeWithTag("record_saver_1").assertIsSelected()
    }

    // 같은 미션을 여러 가족이 저장하면 카드 한 장에 "엄마, 아빠가 저장했어요!" 로 합쳐 보여준다
    @Test
    fun sameMissionSavedByTwoMembersShowsOneCardWithBothNames() {
        val fake = com.kiri.data.bucket.FakeBucketItemRepository
        val mom = fake.item(1, 1, "엄마", title = "한강 축제 가보기").copy(missionId = 100)
        val dad = fake.item(2, 2, "아빠", title = "한강 축제 가보기", deletable = true).copy(missionId = 100)
        val events = mutableListOf<RecordUiEvent>()
        compose.setContent {
            KiriTheme {
                RecordScreen(RecordUiState(today = LocalDate.of(2026, 10, 13), tab = RecordTab.BUCKET, bucketItems = listOf(dad, mom)), { events += it })
            }
        }

        compose.onNodeWithText("엄마, 아빠가 저장했어요!").assertIsDisplayed()
        compose.onNodeWithText("엄마가 저장했어요!").assertDoesNotExist()
        compose.onNodeWithTag("record_bucket_1").assertDoesNotExist()
        // 대표는 내가 저장한(지울 수 있는) 아빠 항목이다
        compose.onNodeWithTag("record_bucket_2").performClick()
        compose.runOnIdle { org.junit.Assert.assertEquals(listOf<RecordUiEvent>(RecordUiEvent.BucketClicked("2")), events) }
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
