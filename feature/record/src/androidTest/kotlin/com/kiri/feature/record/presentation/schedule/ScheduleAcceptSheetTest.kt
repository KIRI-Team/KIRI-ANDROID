package com.kiri.feature.record.presentation.schedule

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kiri.uikit.KiriTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// 약속 확정하기를 누르면 뜨는 확인 시트 (Figma 9898:18767)
class ScheduleAcceptSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsTheScheduleAndSendsConfirmOrDismiss() {
        var confirmed = 0
        var dismissed = 0
        compose.setContent {
            KiriTheme {
                ScheduleAcceptSheetContent(
                    title = "한강 뚜벅뚜벅 축제 가보기",
                    date = LocalDate.of(2026, 6, 23),
                    memberNames = listOf("엄마", "아빠", "나"),
                    onConfirm = { confirmed++ },
                    onDismiss = { dismissed++ },
                )
            }
        }

        compose.onNodeWithText("캘린더에 일정을 추가하시겠습니까?").assertExists()
        compose.onNodeWithText("26.06.23").assertExists()
        compose.onNodeWithText("엄마, 아빠, 나").assertExists()
        compose.onNodeWithText("초대된 가족 구성원에게도 알림이 발송됩니다").assertExists()
        compose.onNodeWithTag("schedule_accept_cancel").performClick()
        compose.onNodeWithTag("schedule_accept_close").performClick()
        compose.onNodeWithTag("schedule_accept_confirm").performClick()
        assertEquals(listOf(1, 2), listOf(confirmed, dismissed))
    }
}
