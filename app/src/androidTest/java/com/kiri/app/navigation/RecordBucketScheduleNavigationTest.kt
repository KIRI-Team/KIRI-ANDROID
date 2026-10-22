package com.kiri.app.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.kiri.app.HiltTestActivity
import com.kiri.domain.home.ScheduleStatus
import com.kiri.domain.record.BucketRepository
import com.kiri.domain.record.RecordRepository
import kotlinx.coroutines.runBlocking
import com.kiri.uikit.KiriTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import org.junit.*
import org.junit.Assert.assertTrue

@HiltAndroidTest
class RecordBucketScheduleNavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()
    @Inject lateinit var repository: RecordRepository
    @Inject lateinit var buckets: BucketRepository
    private lateinit var bucketId: String
    @Before fun setup() {
        hilt.inject()
        runBlocking { bucketId = buckets.save("place-river").id; repository.refresh() }
    }

    private fun openBucketTab() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home_question_card").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithTag("home_tutorial_skip").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithTag("home_tutorial_skip").performClick()
        compose.onNodeWithTag("bottom_nav_record").performClick()
        compose.waitUntil(10_000) { repository.records.value.entries.isNotEmpty() }
        compose.onNodeWithTag("record_tab_BUCKET").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("record_bucket_$bucketId").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun bucketCardOpensOptionSheetAndScheduleLeadsToRegister() {
        openBucketTab()
        compose.onNodeWithTag("record_bucket_$bucketId").performClick()
        compose.onNodeWithTag("record_item_option_sheet").assertExists()
        compose.onNodeWithTag("record_option_schedule").performClick()

        compose.waitUntil(10_000) { compose.onAllNodesWithTag("schedule_register_screen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("record_item_option_sheet").assertDoesNotExist()
        compose.onNodeWithText("한강 뚜벅뚜벅 축제 가보기").assertIsDisplayed()
        compose.onNodeWithText("한강 뚜벅뚜벅 축제 가보기").assertIsDisplayed()
        compose.onNodeWithTag("bottom_nav_record").assertIsDisplayed()

        compose.onNodeWithTag("kiri_back_button").performClick()
        compose.onNodeWithTag("record_bucket_list").assertIsDisplayed()
    }

    @Test fun appSendReturnsToBucketListWithToastAndPendingSchedule() {
        openBucketTab()
        compose.onNodeWithTag("record_bucket_$bucketId").performClick()
        compose.onNodeWithTag("record_option_schedule").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("schedule_register_screen").fetchSemanticsNodes().isNotEmpty() }

        // 내일 09:00, 엄마와 함께
        val tomorrow = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1)
        compose.onNodeWithTag("schedule_register_schedule_row").performClick()
        if (YearMonth.from(tomorrow) != YearMonth.now(ZoneId.of("Asia/Seoul"))) compose.onNodeWithTag("record_calendar_next").performClick()
        compose.onNodeWithTag("record_calendar_day_$tomorrow").performClick()
        compose.onNodeWithTag("record_time_09:00").performClick()
        compose.onNodeWithText("다음").performClick()
        compose.onNodeWithTag("schedule_register_member_row").performClick()
        compose.onNodeWithTag("record_member_mother").performClick()
        compose.onNodeWithText("확인").performClick()
        compose.onNode(hasText("초대장 만들기")).performClick()
        compose.onNodeWithText("요청 보내기").performClick()
        compose.onNodeWithTag("record_send_app").performClick()

        compose.waitUntil(10_000) { compose.onAllNodesWithTag("schedule_sent_toast").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("초대를 발송했습니다!").assertIsDisplayed()
        compose.onNodeWithTag("record_bucket_list").assertIsDisplayed()
        // 버킷은 일정과 연결된 상태로 보존하고, 중복 일정 대신 연결된 약속으로 이동한다.
        assertTrue(buckets.buckets.value.first { it.id == bucketId }.linkedScheduleIds.isNotEmpty())
        compose.onNodeWithTag("record_bucket_$bucketId").performClick()
        compose.onNodeWithTag("record_option_schedule").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("schedule_screen").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(repository.records.value.entries.any { it.date == tomorrow && it.status == ScheduleStatus.PENDING_RESPONSE })
    }

    @Test fun dismissingOptionSheetStaysOnBucketList() {
        openBucketTab()
        compose.onNodeWithTag("record_bucket_$bucketId").performClick()
        compose.onNodeWithTag("record_option_close").performClick()
        compose.onNodeWithTag("record_item_option_sheet").assertDoesNotExist()
        compose.onNodeWithTag("record_bucket_list").assertIsDisplayed()
        compose.onNodeWithTag("schedule_register_screen").assertDoesNotExist()
    }
}
