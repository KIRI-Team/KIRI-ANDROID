package com.kiri.feature.record.presentation.sheet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kiri.uikit.KiriTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RecordSheetsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun optionSheetRoutesEachAction() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            KiriTheme {
                RecordItemOptionSheetContent(
                    title = "광장시장에서 시장음식 먹어보기",
                    subtitle = "아빠가 저장했어요!",
                    onScheduleClick = { clicks += "schedule" },
                    onDeleteClick = { clicks += "delete" },
                    onCloseClick = { clicks += "close" },
                )
            }
        }
        compose.onNodeWithTag("record_option_schedule").performClick()
        compose.onNodeWithTag("record_option_delete").performClick()
        compose.onNodeWithTag("record_option_close").performClick()
        assertEquals(listOf("schedule", "delete", "close"), clicks)
    }

    @Test fun unavailableBucketActionsAreDisabled() {
        compose.setContent { KiriTheme {
            RecordItemOptionSheetContent("완료한 활동", "다른 가족이 저장했어요", {}, {}, {},
                scheduleEnabled = false, deleteEnabled = false)
        } }
        compose.onNodeWithTag("record_option_schedule").assertIsNotEnabled()
        compose.onNodeWithTag("record_option_delete").assertIsNotEnabled()
        compose.onNodeWithTag("record_option_close").assertIsEnabled()
    }

    @Test fun sendMethodSheetRoutesEachAction() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            KiriTheme {
                RecordSendMethodSheetContent(
                    onAppClick = { clicks += "app" },
                    onKakaoClick = { clicks += "kakao" },
                    onCloseClick = { clicks += "close" },
                )
            }
        }
        compose.onNodeWithText("앱으로 보내기").assertExists()
        compose.onNodeWithText("카카오톡으로 보내기").assertExists()
        compose.onNodeWithTag("record_send_app").performClick()
        compose.onNodeWithTag("record_send_kakao").performClick()
        compose.onNodeWithTag("record_send_close").performClick()
        assertEquals(listOf("app", "kakao", "close"), clicks)
    }

    @Test fun scheduleSheetNeedsDateAndTimeAndMovesBetweenMonths() {
        var month by mutableStateOf(YearMonth.of(2026, 12))
        var date by mutableStateOf<LocalDate?>(null)
        var time by mutableStateOf<LocalTime?>(null)
        var next = false
        compose.setContent {
            KiriTheme {
                RecordScheduleSheetContent(
                    month = month,
                    selectedDate = date,
                    selectedTime = time,
                    times = listOf(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                    onMonthChange = { month = it },
                    onDateSelect = { date = it },
                    onTimeSelect = { time = it },
                    onNextClick = { next = true },
                )
            }
        }
        compose.onNodeWithText("다음").assertIsNotEnabled()
        compose.onNodeWithTag("record_calendar_day_2026-12-23").performClick().assertIsSelected()
        compose.onNodeWithText("다음").assertIsNotEnabled()
        compose.onNodeWithTag("record_time_09:30").performClick().assertIsSelected()
        compose.onNodeWithTag("record_time_09:00").assertIsNotSelected()
        compose.onNodeWithText("다음").assertIsEnabled().performClick()
        assertTrue(next)

        // 다음 달 날짜 칸은 눌러도 선택이 바뀌지 않는다
        compose.onNodeWithTag("record_calendar_day_2027-01-01").performClick()
        assertEquals(LocalDate.of(2026, 12, 23), date)

        compose.onNodeWithTag("record_calendar_next").performClick()
        compose.onNodeWithTag("record_calendar_month").assertTextEquals("2027년 1월")
        compose.onNodeWithTag("record_calendar_prev").performClick()
        compose.onNodeWithTag("record_calendar_month").assertTextEquals("2026년 12월")
    }

    @Test fun scheduleSheetBlocksPastDatesAndTimes() {
        var date by mutableStateOf<LocalDate?>(null)
        var time by mutableStateOf<LocalTime?>(null)
        compose.setContent {
            KiriTheme {
                RecordScheduleSheetContent(
                    month = YearMonth.of(2026, 10),
                    selectedDate = date,
                    selectedTime = time,
                    times = listOf(LocalTime.of(9, 0), LocalTime.of(16, 30)),
                    onMonthChange = {},
                    onDateSelect = { date = it },
                    onTimeSelect = { time = it },
                    onNextClick = {},
                    earliest = java.time.LocalDateTime.of(2026, 10, 18, 12, 0),
                )
            }
        }
        // 어제는 눌러도 고를 수 없다
        compose.onNodeWithTag("record_calendar_day_2026-10-17").performClick()
        assertEquals(null, date)
        // 내일은 이른 시간도 고를 수 있다
        compose.onNodeWithTag("record_calendar_day_2026-10-19").performClick()
        compose.onNodeWithTag("record_time_09:00").performClick().assertIsSelected()
        compose.onNodeWithText("다음").assertIsEnabled()
        // 오늘로 바꾸면 이미 지난 09:00 은 고른 것으로 보지 않는다
        compose.onNodeWithTag("record_calendar_day_2026-10-18").performClick()
        compose.onNodeWithTag("record_time_09:00").assertIsNotEnabled().assertIsNotSelected()
        compose.onNodeWithText("다음").assertIsNotEnabled()
        compose.onNodeWithTag("record_time_16:30").performClick().assertIsSelected()
        compose.onNodeWithText("다음").assertIsEnabled()
    }

    @Test fun scheduleSheetBlocksDatesThatAlreadyHaveASchedule() {
        var date by mutableStateOf<LocalDate?>(LocalDate.of(2026, 12, 23))
        var time by mutableStateOf<LocalTime?>(LocalTime.of(9, 0))
        var blocked by mutableStateOf(emptySet<LocalDate>())
        compose.setContent {
            KiriTheme {
                RecordScheduleSheetContent(
                    month = YearMonth.of(2026, 12),
                    selectedDate = date,
                    selectedTime = time,
                    times = listOf(LocalTime.of(9, 0)),
                    onMonthChange = {},
                    onDateSelect = { date = it },
                    onTimeSelect = { time = it },
                    onNextClick = {},
                    blockedDates = blocked,
                )
            }
        }
        compose.onNodeWithText("다음").assertIsEnabled()
        // 고른 날에 다른 일정이 있는 것으로 뒤늦게 확인되면 다음으로 넘어갈 수 없다
        blocked = setOf(LocalDate.of(2026, 12, 23), LocalDate.of(2026, 12, 24))
        compose.onNodeWithText("다음").assertIsNotEnabled()
        // 하루에 일정은 하나만 정할 수 있어 일정이 있는 날은 눌러도 고를 수 없다
        compose.onNodeWithTag("record_calendar_day_2026-12-24").performClick()
        assertEquals(LocalDate.of(2026, 12, 23), date)
        compose.onNodeWithTag("record_calendar_day_2026-12-25").performClick()
        assertEquals(LocalDate.of(2026, 12, 25), date)
        compose.onNodeWithText("다음").assertIsEnabled()
    }

    @Test fun memberSheetTogglesSelectionAndConfirms() {
        var selected by mutableStateOf(emptySet<String>())
        var confirmed = false
        compose.setContent {
            KiriTheme {
                RecordMemberSelectSheetContent(
                    members = (1..6).map { RecordMember(it.toString(), "구성원$it") },
                    selectedIds = selected,
                    onMemberToggle = { selected = if (it.id in selected) selected - it.id else selected + it.id },
                    onConfirmClick = { confirmed = true },
                )
            }
        }
        compose.onNodeWithTag("record_member_1").performClick().assertIsOn()
        compose.onNodeWithTag("record_member_4").performClick().assertIsOn()
        compose.onNodeWithTag("record_member_1").performClick().assertIsOff()
        assertEquals(setOf("4"), selected)
        compose.onNodeWithText("확인").performClick()
        assertTrue(confirmed)
    }
}
