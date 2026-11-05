package com.kiri.app.navigation

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.app.HiltTestActivity
import com.kiri.domain.record.RecordRepository
import com.kiri.uikit.KiriTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import java.io.File
import org.junit.*
import org.junit.Assert.*

@HiltAndroidTest
class RecordCalendarNavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()
    @Inject lateinit var repository: RecordRepository
    @Before fun setup() { hilt.inject() }

    private fun start() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home_question_card").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithTag("home_tutorial_skip").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithTag("home_tutorial_skip").performClick()
        compose.onNodeWithTag("bottom_nav_record").performClick()
        compose.waitUntil(10_000) { repository.records.value.entries.isNotEmpty() }
    }

    private fun open(id: String) {
        val date = repository.records.value.entries.single { it.id == id }.date
        compose.onNodeWithTag("record_date_$date").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("한강 뚜벅뚜벅 축제 가보기").fetchSemanticsNodes().isNotEmpty() && compose.onAllNodesWithTag("record_calendar").fetchSemanticsNodes().isEmpty() }
    }

    private fun back() { compose.onNodeWithContentDescription("뒤로").performClick() }

    @Test fun clocksAndHeartOpenTheirOwnDetailAndOnlyTodayOffersCustomization() {
        start()
        open("record-upcoming")
        compose.onNodeWithText("끼리추천").assertIsDisplayed()
        compose.onNodeWithTag("open_custom").assertDoesNotExist()
        capture("calendar-upcoming")
        back()
        open("schedule-demo")
        compose.onNodeWithText("약속 확정").assertIsDisplayed()
        compose.onNodeWithTag("open_custom").assertIsDisplayed()
        capture("calendar-today")
        compose.onNodeWithTag("open_custom").performClick()
        compose.onNodeWithTag("condition_MORE_REST").performScrollTo().performClick()
        compose.onNodeWithTag("apply_custom").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("condition_custom_screen").fetchSemanticsNodes().isEmpty() }
        back()
        open("record-bucket")
        compose.onNodeWithText("버킷리스트 저장").assertIsDisplayed()
        compose.onNodeWithText("버킷리스트로 저장된 약속이에요!").assertIsDisplayed()
        compose.onNodeWithTag("open_custom").assertDoesNotExist()
        capture("calendar-bucket-detail")
    }

    @Test fun representativePhotoChangesAndOwnMemoryCanBeEditedAndDeleted() {
        start()
        open("record-memory-1")
        compose.onNodeWithTag("record_memory_screen").assertIsDisplayed()
        capture("calendar-memory")
        compose.onNodeWithTag("record_representative_photo").performScrollTo().performClick()
        compose.onNodeWithTag("change_record_photo").performClick()
        compose.onNodeWithTag("save_record_photo").assertIsNotEnabled()
        compose.onNodeWithTag("record_photo_pixel-1").performClick().assertIsSelected()
        capture("calendar-photo-selection")
        compose.onNodeWithTag("save_record_photo").assertIsEnabled().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("record_success_toast").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("사진 변경이 완료되었습니다!").assertIsDisplayed()
        assertEquals("pixel-1", repository.records.value.entries.single { it.id == "record-memory-1" }.representativePhotoId)
        capture("calendar-photo-saved")
        val memoryId = repository.records.value.memories.single { it.entryId == "record-memory-1" }.id
        compose.onNodeWithTag("record_memory_menu_$memoryId").performScrollTo().performClick()
        compose.onNodeWithTag("menu_edit_record").assertIsDisplayed()
        compose.onNodeWithTag("menu_delete_record").assertIsDisplayed()
        capture("calendar-memory-menu")
        compose.onNodeWithTag("menu_edit_record").performClick()
        compose.onNodeWithTag("memory_text").performScrollTo().performTextReplacement("가족과 함께한 새로운 기록")
        compose.onNodeWithTag("submit_memory").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("record_memory_screen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("가족과 함께한 새로운 기록").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("record_memory_menu_$memoryId").performScrollTo().performClick()
        compose.onNodeWithTag("menu_delete_record").performClick()
        compose.onNodeWithTag("confirm_delete_record").performClick()
        compose.waitUntil(10_000) { repository.records.value.memories.none { it.id == memoryId } }
        compose.onNodeWithText("아직 기록이 없어요!").assertIsDisplayed()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        // Dialog-window exit animations are outside Compose's test clock.
        android.os.SystemClock.sleep(300)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = instrumentation.targetContext.getExternalFilesDir(null)!!
        File(directory, "$name.png").outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
