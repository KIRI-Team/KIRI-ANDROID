package com.kiri.app.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kiri.app.HiltTestActivity
import com.kiri.uikit.KiriTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.*

// 기록 탭 버킷리스트(서버 데이터, UI 테스트에서는 FakeBucketItemModule)의 카드 → 일정 잡기·삭제하기 시트 흐름.
// 일정 잡기는 일정 등록 화면으로 가고, 삭제는 확인 창을 거쳐 지운다
@HiltAndroidTest
class RecordBucketScheduleNavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()

    @Before fun setup() = hilt.inject()

    private fun openBucketTab() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home_question_card").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithTag("home_tutorial_skip").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithTag("home_tutorial_skip").performClick()
        compose.onNodeWithTag("bottom_nav_record").performClick()
        compose.onNodeWithTag("record_tab_BUCKET").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("record_bucket_12").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun schedulableBucketOpensScheduleCreateScreen() {
        openBucketTab()
        compose.onNodeWithTag("record_bucket_12").performClick()
        compose.onNodeWithTag("record_item_option_sheet").assertExists()
        compose.onNodeWithText("광장시장에서 시장음식 먹어보기").assertExists()
        compose.onNodeWithTag("record_option_schedule").assertIsEnabled().performClick()

        // 가족 정보를 불러오는 동안·불러온 뒤 모두 일정 등록 화면이다
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("schedule_register_screen").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("bucket_schedule_status").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("record_item_option_sheet").assertDoesNotExist()
        compose.onNodeWithText("일정 등록하기").assertIsDisplayed()
        compose.onNodeWithTag("kiri_back_button").performClick()
        compose.onNodeWithTag("record_bucket_list").assertIsDisplayed()
    }

    @Test fun deletableBucketIsDeletedAfterConfirmation() {
        openBucketTab()
        compose.onNodeWithText("아빠 2").assertExists()
        compose.onNodeWithTag("record_bucket_12").performClick()
        compose.onNodeWithTag("record_option_delete").assertIsEnabled().performClick()

        compose.onNodeWithText("버킷을 삭제할까요?").assertIsDisplayed()
        compose.onNodeWithTag("record_bucket_delete_confirm").performClick()

        compose.waitUntil(5_000) { compose.onAllNodesWithTag("record_notice_toast").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("버킷리스트에서 삭제했어요.").assertIsDisplayed()
        compose.onNodeWithTag("record_bucket_12").assertDoesNotExist()
        compose.onNodeWithText("아빠 1").assertExists()
    }

    @Test fun cancellingDeleteKeepsBucket() {
        openBucketTab()
        compose.onNodeWithTag("record_bucket_12").performClick()
        compose.onNodeWithTag("record_option_delete").performClick()
        compose.onNodeWithText("취소").performClick()

        compose.onNodeWithText("버킷을 삭제할까요?").assertDoesNotExist()
        compose.onNodeWithTag("record_bucket_12").assertExists()
    }

    @Test fun completedBucketCannotBeScheduledOrDeleted() {
        openBucketTab()
        compose.onNodeWithTag("record_bucket_11").performScrollTo().performClick()
        compose.onNodeWithText("완료한 활동").assertExists()
        compose.onNodeWithTag("record_option_schedule").assertIsNotEnabled()
        compose.onNodeWithTag("record_option_delete").assertIsNotEnabled()
    }

    @Test fun dismissingOptionSheetStaysOnBucketList() {
        openBucketTab()
        compose.onNodeWithTag("record_bucket_12").performClick()
        compose.onNodeWithTag("record_option_close").performClick()
        compose.onNodeWithTag("record_item_option_sheet").assertDoesNotExist()
        compose.onNodeWithTag("record_bucket_list").assertIsDisplayed()
        compose.onNodeWithTag("schedule_register_screen").assertDoesNotExist()
    }
}
