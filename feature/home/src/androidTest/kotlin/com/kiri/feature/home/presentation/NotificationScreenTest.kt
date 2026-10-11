package com.kiri.feature.home.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.kiri.domain.notification.*
import com.kiri.feature.home.NotificationUiState
import com.kiri.uikit.KiriTheme
import java.time.Instant
import org.junit.*
import org.junit.Assert.*

class NotificationScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun filtersAndReadAllRetainRowsAndShowEmptyState() {
        val notification = FamilyNotification("one", "끼리네", "[일정] 초대장이 도착했어요!", "일정을 확인해주세요", Instant.now(), NotificationCategory.SCHEDULE, NotificationTarget.SCHEDULE, "schedule")
        val state = mutableStateOf(NotificationUiState(loading = false, items = listOf(notification)))
        compose.setContent { KiriTheme { NotificationScreen(state.value, {}, { state.value = state.value.copy(selected = it) },
            { state.value = state.value.copy(items = state.value.items.map { it.copy(read = true) }) }, {}, {}) } }
        compose.onNodeWithText("알림함 +1").assertIsDisplayed()
        compose.onNodeWithTag("notifications_read_all").performClick()
        compose.onNodeWithTag("notification_one").assertIsDisplayed()
        compose.onNodeWithTag("notifications_read_all").assertIsNotEnabled()
        compose.onNodeWithTag("notification_filter_POKE").performClick().assertIsSelected()
        compose.onNodeWithTag("notifications_empty").assertIsDisplayed()
        compose.onNodeWithTag("notification_filter_ALL").performClick()
        compose.onNodeWithTag("notification_one").assertIsDisplayed()
    }
    @Test fun failureOffersRetryAndNoRows() {
        var retried = false
        compose.setContent { KiriTheme { NotificationScreen(NotificationUiState(loading = false, error = "알림을 불러오지 못했어요"), {}, {}, {}, {}, { retried = true }) } }
        compose.onNodeWithTag("notifications_retry").performClick()
        assertTrue(retried)
        compose.onNodeWithTag("notification_list").assertDoesNotExist()
    }
}
