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
        compose.setContent { KiriTheme { ServerScheduleActions(ServerScheduleDetailUiState(canRespond = true), { answers += it }, {}) } }
        compose.onNodeWithTag("server_schedule_accept").performClick()
        compose.onNodeWithTag("server_schedule_decline").performClick()
        assertEquals(listOf(true, false), answers)
    }
    @Test fun savingDisablesAllServerActions() {
        compose.setContent { KiriTheme { ServerScheduleActions(ServerScheduleDetailUiState(canRespond = true, canCancel = true, canReview = true, detail = DetailUiState(saving = true)), {}, {}) } }
        listOf("server_schedule_accept", "server_schedule_decline", "server_schedule_cancel", "server_schedule_review").forEach { compose.onNodeWithTag(it).assertIsNotEnabled() }
    }
    @Test fun reviewButtonOpensEditorCallback() {
        var opened = false
        compose.setContent { KiriTheme { ServerScheduleActions(ServerScheduleDetailUiState(canReview = true), {}, {}, { opened = true }) } }
        compose.onNodeWithTag("server_schedule_review").performClick()
        assertTrue(opened)
    }
}
