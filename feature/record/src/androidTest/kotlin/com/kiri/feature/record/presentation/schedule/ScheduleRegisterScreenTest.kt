package com.kiri.feature.record.presentation.schedule

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.kiri.domain.record.ScheduleRequestChannel
import com.kiri.feature.record.presentation.sheet.RecordMember
import com.kiri.uikit.KiriTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScheduleRegisterScreenTest {
    @get:Rule val compose = createComposeRule()

    // 이번 달 안의 날짜여야 시트를 열었을 때 바로 보인다. 지난 날짜 제한은 걸지 않는다(earliest = null)
    private val pickDate = YearMonth.from(LocalDate.now()).atDay(15)
    private var draft by mutableStateOf(ScheduleRegisterDraft())
    private var sending by mutableStateOf(false)
    private var error by mutableStateOf<String?>(null)
    private val sent = mutableListOf<ScheduleRequestChannel>()

    private fun setFlow() {
        compose.setContent {
            KiriTheme {
                ScheduleRegisterFlow(
                    savedBy = "딸",
                    title = "한강 뚜벅뚜벅 축제 가보기",
                    description = "설명",
                    members = listOf(RecordMember("mom", "엄마"), RecordMember("dad", "아빠")),
                    draft = draft,
                    onDraftChange = { draft = it },
                    onBackClick = {},
                    onDetailClick = {},
                    onSend = { sent += it },
                    sending = sending,
                    error = error,
                    times = listOf(LocalTime.of(9, 0), LocalTime.of(16, 30)),
                )
            }
        }
    }

    private fun fillScheduleAndMembers(vararg memberIds: String) {
        compose.onNodeWithTag("schedule_register_schedule_row").performClick()
        compose.onNodeWithTag("record_calendar_day_$pickDate").performClick()
        compose.onNodeWithTag("record_time_09:00").performClick()
        compose.onNodeWithText("다음").performClick()
        compose.onNodeWithTag("schedule_register_member_row").performClick()
        memberIds.forEach { compose.onNodeWithTag("record_member_$it").performClick() }
        compose.onNodeWithText("확인").performClick()
    }

    @Test fun scheduleRowOpensScheduleSheetAndAppliesPickOnNext() {
        setFlow()
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
        setFlow()
        compose.onNodeWithTag("schedule_register_member_row").performClick()
        compose.onNodeWithTag("record_member_select_sheet").assertExists()

        compose.onNodeWithTag("record_member_mom").performClick()
        compose.onNodeWithTag("record_member_dad").performClick()
        compose.onNodeWithText("확인").performClick()

        compose.onNodeWithTag("record_member_select_sheet").assertDoesNotExist()
        compose.onNodeWithTag("schedule_register_member_row").assertTextContains("인원 2명")
    }

    @Test fun inviteOpensRequestAndEachSendMethodIsPassedOn() {
        setFlow()
        val invite = compose.onNode(hasText("초대장 만들기"))
        invite.assertIsNotEnabled()
        fillScheduleAndMembers("mom")
        compose.onNodeWithTag("schedule_register_message").performTextInput("같이 가요")
        compose.onNodeWithText("5/100").assertExists()

        invite.assertIsEnabled().performClick()

        // 초대장 만들기를 누르면 요청알림 미리보기로 넘어가고, 고른 내용이 그대로 보인다
        compose.onNodeWithTag("schedule_request_screen").assertExists()
        compose.onNodeWithText("딸이 저장한 미션이에요!").assertExists()
        compose.onNodeWithText(requestDateLabel(pickDate)).assertExists()
        compose.onNodeWithText("엄마").assertExists()
        compose.onNodeWithText("같이 가요").assertExists()

        compose.onNodeWithText("요청 보내기").performClick()
        compose.onNodeWithTag("record_send_app").performClick()
        compose.onNodeWithTag("record_send_method_sheet").assertDoesNotExist()
        compose.onNodeWithText("요청 보내기").performClick()
        compose.onNodeWithTag("record_send_kakao").performClick()
        assertEquals(listOf(ScheduleRequestChannel.APP, ScheduleRequestChannel.KAKAO), sent)
    }

    @Test fun requestShowsSendingAndErrorWithoutLeaving() {
        setFlow()
        fillScheduleAndMembers("mom")
        compose.onNode(hasText("초대장 만들기")).performClick()

        sending = true
        compose.onNodeWithText("보내는 중…").assertIsNotEnabled()
        compose.onNodeWithTag("schedule_request_cancel").assertIsNotEnabled()

        sending = false
        error = "요청을 보내지 못했어요. 다시 시도해주세요."
        compose.onNodeWithTag("schedule_request_error").assertTextContains("요청을 보내지 못했어요. 다시 시도해주세요.")
        compose.onNodeWithText("요청 보내기").assertIsEnabled()
        compose.onNodeWithTag("schedule_request_screen").assertExists()
    }

    @Test fun cancelOnRequestReturnsToRegisterKeepingInput() {
        setFlow()
        fillScheduleAndMembers("mom", "dad")
        compose.onNode(hasText("초대장 만들기")).performClick()
        compose.onNodeWithText("엄마, 아빠").assertExists()

        compose.onNodeWithTag("schedule_request_cancel").performClick()

        compose.onNodeWithTag("schedule_request_screen").assertDoesNotExist()
        compose.onNodeWithTag("schedule_register_member_row").assertTextContains("인원 2명")
    }
}
