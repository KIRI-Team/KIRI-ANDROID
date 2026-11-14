package com.kiri.feature.record.presentation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kiri.feature.record.ServerScheduleDetailUiState
import com.kiri.feature.detail.model.DetailUiState
import com.kiri.uikit.KiriTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ServerScheduleActionsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun responseButtonsSendOnlyAcceptAndDecline() {
        val answers = mutableListOf<Boolean>()
        compose.setContent { KiriTheme { ServerScheduleActions(ServerScheduleDetailUiState(canRespond = true), { answers += it }) } }
        compose.onNodeWithTag("server_schedule_accept").performClick()
        compose.onNodeWithTag("server_schedule_decline").performClick()
        assertEquals(listOf(true, false), answers)
    }
    @Test fun responseButtonsAreHiddenWhenOpenedFromTheRecordTab() {
        compose.setContent { KiriTheme { ServerScheduleActions(ServerScheduleDetailUiState(canRespond = true), {}, showRespond = false) } }
        compose.onNodeWithTag("server_schedule_accept").assertDoesNotExist()
        compose.onNodeWithTag("server_schedule_decline").assertDoesNotExist()
    }
    @Test fun savingDisablesAllServerActions() {
        compose.setContent { KiriTheme { ServerScheduleActions(ServerScheduleDetailUiState(canRespond = true, canCancel = true, canReview = true, detail = DetailUiState(saving = true)), {}) } }
        listOf("server_schedule_accept", "server_schedule_decline").forEach { compose.onNodeWithTag(it).assertIsNotEnabled() }
        // 일정 취소는 일정 취소하기 버튼으로, 기록은 오늘의 추억 기록하러가기로 옮겨 아래쪽 임시 버튼은 없다
        compose.onNodeWithTag("server_schedule_cancel").assertDoesNotExist()
        compose.onNodeWithTag("server_schedule_review").assertDoesNotExist()
    }
}
