package com.kiri.feature.record.presentation.schedule

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kiri.feature.record.presentation.sheet.RecordMember
import com.kiri.uikit.KiriTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test

class ScheduleRegisterScreenTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.now()
    // 이번 달 안의 날짜여야 시트를 열었을 때 바로 보인다
    private val pickDate = YearMonth.from(today).atDay(15)

    private fun setScreen() {
        compose.setContent {
            KiriTheme {
                ScheduleRegisterRoute(
                    savedBy = "딸",
                    title = "한강 뚜벅뚜벅 축제 가보기",
                    description = "설명",
                    members = listOf(RecordMember("mom", "엄마"), RecordMember("dad", "아빠")),
                    times = listOf(LocalTime.of(9, 0), LocalTime.of(16, 30)),
                    onBackClick = {},
                    onDetailClick = {},
                )
            }
        }
    }

    @Test fun scheduleRowOpensScheduleSheetAndAppliesPickOnNext() {
        setScreen()
        compose.onNodeWithTag("schedule_register_schedule_row").performClick()
        compose.onNodeWithTag("record_schedule_sheet").assertExists()

        compose.onNodeWithTag("record_calendar_day_$pickDate").performClick()
        compose.onNodeWithTag("record_time_16:30").performClick()
        compose.onNodeWithText("다음").performClick()

        compose.onNodeWithTag("record_schedule_sheet").assertDoesNotExist()
        compose.onNodeWithTag("schedule_register_schedule_row")
            .assertTextContains(scheduleLabel(pickDate, LocalTime.of(16, 30)))
    }

    @Test fun memberRowOpensMemberSheetAndAppliesPickOnConfirm() {
        setScreen()
        compose.onNodeWithTag("schedule_register_member_row").performClick()
        compose.onNodeWithTag("record_member_select_sheet").assertExists()

        compose.onNodeWithTag("record_member_mom").performClick()
        compose.onNodeWithTag("record_member_dad").performClick()
        compose.onNodeWithText("확인").performClick()

        compose.onNodeWithTag("record_member_select_sheet").assertDoesNotExist()
        compose.onNodeWithTag("schedule_register_member_row").assertTextContains("인원 2명")
    }

    @Test fun inviteButtonEnablesOnlyAfterScheduleAndMembersAndOpensRequest() {
        setScreen()
        val invite = compose.onNode(hasText("초대장 만들기"))
        invite.assertIsNotEnabled()

        compose.onNodeWithTag("schedule_register_schedule_row").performClick()
        compose.onNodeWithTag("record_calendar_day_$pickDate").performClick()
        compose.onNodeWithTag("record_time_09:00").performClick()
        compose.onNodeWithText("다음").performClick()
        invite.assertIsNotEnabled()

        compose.onNodeWithTag("schedule_register_member_row").performClick()
        compose.onNodeWithTag("record_member_mom").performClick()
        compose.onNodeWithText("확인").performClick()

        compose.onNodeWithTag("schedule_register_message").performTextInput("같이 가요")
        compose.onNodeWithText("5/100").assertExists()

        invite.assertIsEnabled().performClick()

        // 초대장 만들기를 누르면 요청알림 미리보기로 넘어가고, 고른 내용이 그대로 보인다
        compose.onNodeWithTag("schedule_request_screen").assertExists()
        compose.onNodeWithText("딸이 저장한 미션이에요!").assertExists()
        compose.onNodeWithText(requestDateLabel(pickDate)).assertExists()
        compose.onNodeWithText("엄마").assertExists()
        compose.onNodeWithText("같이 가요").assertExists()

        // 요청 보내기를 누르면 발송 방법 시트가 뜬다. 앱으로 보내기는 아직 이동할 곳이 없어 시트만 닫힌다
        compose.onNodeWithText("요청 보내기").performClick()
        compose.onNodeWithTag("record_send_method_sheet").assertExists()
        compose.onNodeWithTag("record_send_kakao").assertExists()
        compose.onNodeWithTag("record_send_app").performClick()
        compose.onNodeWithTag("record_send_method_sheet").assertDoesNotExist()
        compose.onNodeWithTag("schedule_request_screen").assertExists()
    }

    @Test fun cancelOnRequestReturnsToRegisterKeepingInput() {
        setScreen()
        compose.onNodeWithTag("schedule_register_schedule_row").performClick()
        compose.onNodeWithTag("record_calendar_day_$pickDate").performClick()
        compose.onNodeWithTag("record_time_09:00").performClick()
        compose.onNodeWithText("다음").performClick()
        compose.onNodeWithTag("schedule_register_member_row").performClick()
        compose.onNodeWithTag("record_member_mom").performClick()
        compose.onNodeWithTag("record_member_dad").performClick()
        compose.onNodeWithText("확인").performClick()
        compose.onNode(hasText("초대장 만들기")).performClick()
        compose.onNodeWithText("엄마, 아빠").assertExists()

        compose.onNodeWithTag("schedule_request_cancel").performClick()

        compose.onNodeWithTag("schedule_request_screen").assertDoesNotExist()
        compose.onNodeWithTag("schedule_register_member_row").assertTextContains("인원 2명")
    }
}
