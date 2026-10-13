package com.kiri.feature.record.presentation

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.feature.record.RecordViewModel
import com.kiri.feature.record.model.*
import com.kiri.uikit.KiriTheme
import java.io.File
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class RecordScreenTest {
    @get:Rule val compose = createComposeRule()
    private fun start() {
        val vm = RecordViewModel(SavedStateHandle(mapOf("recordSeedDate" to "2026-10-13")))
        compose.setContent { KiriTheme { RecordRoute(viewModel = vm) } }
    }

    @Test fun calendarStartsSelectedAndMonthDateAndBucketFilterControlsWork() {
        start()
        compose.onNodeWithTag("record_tab_CALENDAR").assertIsSelected()
        compose.onNodeWithTag("record_month").assertTextEquals("2026년 10월")
        capture("record-calendar")
        compose.onNodeWithTag("record_date_2026-10-22").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("record_previous_month").performScrollTo().performClick()
        compose.onNodeWithTag("record_month").assertTextEquals("2026년 9월")
        compose.onNodeWithTag("record_next_month").performClick()
        compose.onNodeWithTag("record_date_2026-10-22").assertIsSelected()
        compose.onNodeWithTag("record_tab_BUCKET").performClick()
        compose.onNodeWithTag("record_bucket_list").assertIsDisplayed()
        compose.onNodeWithText("엄마 1").assertIsDisplayed()
        compose.onNodeWithText("아빠 2").assertIsDisplayed()
        capture("record-bucket")
        compose.onNodeWithTag("record_saver_father").performClick().assertIsSelected()
        compose.onNodeWithTag("record_bucket_bucket-1").assertIsDisplayed()
        compose.onNodeWithTag("record_bucket_bucket-2").assertIsDisplayed()
        compose.onNodeWithTag("record_bucket_bucket-3").assertDoesNotExist()
        compose.onNodeWithTag("record_saver_mother").performClick()
        compose.onNodeWithTag("record_bucket_bucket-3").assertIsDisplayed()
        compose.onNodeWithTag("record_bucket_bucket-1").assertDoesNotExist()
        compose.onNodeWithTag("record_tab_CALENDAR").performClick()
        compose.onNodeWithTag("record_date_2026-10-22").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("record_tab_BUCKET").performClick()
        compose.onNodeWithTag("record_saver_mother").assertIsSelected()
    }

    @Test fun sixWeekMonthAndEmptyListsRemainScrollableAndDoNotClipControls() {
        val state = mutableStateOf(RecordUiState(today = LocalDate.of(2026, 8, 31)))
        compose.setContent { KiriTheme { RecordScreen(state.value, { event ->
            if (event is RecordUiEvent.TabSelected) state.value = state.value.copy(tab = event.tab)
        }) } }
        compose.onNodeWithTag("record_date_2026-08-31").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("다가오는 약속이 없어요").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("record_tab_BUCKET").assertIsDisplayed()
        compose.onNodeWithTag("record_tab_BUCKET").performClick()
        compose.onNodeWithText("저장한 활동이 없어요").assertIsDisplayed()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val root = automation.rootInActiveWindow
        if (root?.packageName?.toString() == "android") {
            root.findAccessibilityNodeInfosByText("OK").forEach { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
            Thread.sleep(300)
            compose.waitForIdle()
        }
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
