package com.kiri.feature.record.presentation.schedule

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kiri.feature.record.BucketScheduleCreateUiState
import com.kiri.feature.record.presentation.sheet.RecordMember
import com.kiri.uikit.KiriTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// 서버 버킷의 일정 잡기 화면이 버킷 정보를 보여주고, 가족을 불러오지 못하면 다시 시도하게 하는지 확인한다
class BucketScheduleCreateScreenTest {
    @get:Rule val compose = createComposeRule()

    private val loaded = BucketScheduleCreateUiState(
        earliest = LocalDateTime.of(2026, 11, 5, 12, 0),
        title = "광장시장에서 시장음식 먹어보기",
        savedBy = "아빠",
        loading = false,
        members = listOf(RecordMember("2", "첫째"), RecordMember("3", "아빠")),
        draft = ScheduleRegisterDraft(LocalDate.of(2026, 11, 15), LocalTime.of(14, 0), setOf("2")),
    )

    @Test
    fun loadedStateShowsRegisterFormWithBucketTitleAndSaver() {
        compose.setContent { KiriTheme { BucketScheduleCreateScreen(loaded, {}, {}, {}, {}) } }

        compose.onNodeWithTag("schedule_register_screen").assertIsDisplayed()
        compose.onNodeWithText("일정 등록하기").assertIsDisplayed()
        compose.onNodeWithText("광장시장에서 시장음식 먹어보기").assertIsDisplayed()
        compose.onNodeWithText("아빠가 저장했어요!").assertIsDisplayed()
        compose.onNodeWithText("26.11.15(일) · 14:00분").assertIsDisplayed()
        compose.onNodeWithText("초대장 만들기").assertIsEnabled()
    }

    @Test
    fun familyLoadFailureShowsRetry() {
        var retries = 0
        compose.setContent {
            KiriTheme {
                BucketScheduleCreateScreen(
                    loaded.copy(loadError = "가족 정보를 불러오지 못했어요."),
                    {}, {}, {}, { retries++ },
                )
            }
        }

        compose.onNodeWithTag("bucket_schedule_status").assertIsDisplayed()
        compose.onNodeWithText("가족 정보를 불러오지 못했어요.").assertIsDisplayed()
        compose.onNodeWithText("다시 시도").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
