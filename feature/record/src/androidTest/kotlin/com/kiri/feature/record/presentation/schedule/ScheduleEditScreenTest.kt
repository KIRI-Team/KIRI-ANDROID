package com.kiri.feature.record.presentation.schedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.kiri.feature.record.ScheduleEditUiState
import com.kiri.feature.record.presentation.sheet.RecordMember
import com.kiri.uikit.KiriTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// 일정 등록 화면의 변경 모드가 기존 값을 보여주고 변경하기로 저장을 요청하는지 확인한다
class ScheduleEditScreenTest {
    @get:Rule val compose = createComposeRule()

    private val loaded = ScheduleEditUiState(
        earliest = LocalDateTime.of(2026, 11, 4, 12, 0),
        loading = false,
        title = "11월 17일 약속",
        subtitle = "약속 확정",
        members = listOf(RecordMember("2", "첫째"), RecordMember("3", "아빠")),
        draft = ScheduleRegisterDraft(LocalDate.of(2026, 11, 17), LocalTime.of(14, 50), setOf("2")),
    )

    @Test
    fun editModeShowsExistingValuesAndSavesWithChangeButton() {
        var saves = 0
        compose.setContent { KiriTheme { ScheduleEditScreen(loaded, {}, {}, { saves++ }, {}) } }

        compose.onNodeWithText("일정 변경하기").assertIsDisplayed()
        compose.onNodeWithText("일정 등록하기").assertDoesNotExist()
        compose.onNodeWithText("약속 확정").assertIsDisplayed()
        compose.onNodeWithText("11월 17일 약속").assertIsDisplayed()
        compose.onNodeWithText("26.11.17(화) · 14:50분").assertIsDisplayed()
        compose.onNodeWithText("인원 1명").assertIsDisplayed()
        compose.onNodeWithText("초대장 만들기").assertDoesNotExist()

        compose.onNodeWithText("변경하기").performClick()
        compose.runOnIdle { assertEquals(1, saves) }
    }

    @Test
    fun savingDisablesButtonAndErrorIsShown() {
        compose.setContent {
            KiriTheme { ScheduleEditScreen(loaded.copy(saving = true, error = "지금은 변경할 수 없는 약속이에요."), {}, {}, {}, {}) }
        }

        compose.onNodeWithText("변경 중…").assertIsNotEnabled()
        compose.onNodeWithTag("schedule_form_error").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("지금은 변경할 수 없는 약속이에요.").assertExists()
    }

    @Test
    fun loadFailureShowsRetry() {
        var retries = 0
        compose.setContent {
            KiriTheme {
                ScheduleEditScreen(
                    ScheduleEditUiState(earliest = loaded.earliest, loading = false, loadError = "약속 정보를 불러오지 못했어요."),
                    {}, {}, {}, { retries++ },
                )
            }
        }

        compose.onNodeWithText("일정 변경하기").assertIsDisplayed()
        compose.onNodeWithText("약속 정보를 불러오지 못했어요.").assertIsDisplayed()
        compose.onNodeWithText("다시 시도").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
