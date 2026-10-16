package com.kiri.feature.place.presentation

import android.graphics.Bitmap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.feature.place.model.PlaceUiState
import com.kiri.uikit.KiriTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

class PlaceFilterSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun applyPersistsSelectionsButDismissDiscardsDraftAndResetRestoresDefaults() {
        compose.setContent { KiriTheme { PlaceScreen(PlaceUiState(loading = false)) {} } }
        compose.onNodeWithTag("place_filter_region").performClick()
        compose.onNodeWithTag("place_filter_sheet").assertIsDisplayed()
        compose.onNodeWithTag("filter_region_경기").performClick()
        compose.onNodeWithTag("filter_distance_3km 이내").performClick()
        compose.onNodeWithTag("filter_apply").performClick()
        compose.onNodeWithTag("place_filter_sheet").assertDoesNotExist()
        compose.onNodeWithTag("place_filter_date").performClick()
        compose.onNodeWithTag("filter_region_경기").assertIsSelected()
        compose.onNodeWithTag("filter_distance_3km 이내").assertIsSelected()
        compose.onNodeWithTag("filter_region_인천").performClick()
        compose.onNodeWithTag("place_filter_close").performClick()
        compose.onNodeWithTag("place_filter_region").performClick()
        compose.onNodeWithTag("filter_region_경기").assertIsSelected()
        compose.onNodeWithTag("filter_reset").performClick()
        compose.onNodeWithTag("filter_region_서울특별시").assertIsSelected()
        compose.onNodeWithTag("filter_distance_1km 이내").assertIsSelected()
        compose.onNodeWithTag("filter_apply").performClick()
        compose.onNodeWithTag("place_filter_region").performClick()
        compose.onNodeWithTag("filter_region_서울특별시").assertIsSelected()
        capture("place-filter-sheet")
        compose.onNodeWithTag("filter_price_min").performScrollTo().assertTextEquals("0원")
        compose.onNodeWithTag("filter_price_max").assertTextEquals("1,000,000원")
        capture("place-filter-price")
    }

    @Test fun datePickerCancelsWithoutChangingTheDateAndPriceRangeCanBeAdjusted() {
        compose.setContent { KiriTheme { PlaceScreen(PlaceUiState(loading = false)) {} } }
        compose.onNodeWithTag("place_filter_date").performClick()
        compose.onNodeWithTag("filter_date").performScrollTo().performClick()
        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithText("날짜·시간 선택").assertIsDisplayed()
        compose.onNodeWithTag("filter_price").performScrollTo().performTouchInput {
            swipe(centerLeft + androidx.compose.ui.geometry.Offset(10f, 0f), center)
        }
        compose.onNodeWithTag("filter_price_min").assert(!hasText("0원"))
        compose.onNodeWithTag("filter_apply").assertIsDisplayed()
        capture("place-filter-selected-price")
    }

    @Test fun appliedAndOpenDraftSelectionsSurviveStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { KiriTheme { PlaceScreen(PlaceUiState(loading = false)) {} } }
        compose.onNodeWithTag("place_filter_region").performClick()
        compose.onNodeWithTag("filter_region_경기").performClick()
        compose.onNodeWithTag("filter_apply").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("place_filter_region").performClick()
        compose.onNodeWithTag("filter_region_경기").assertIsSelected()
        compose.onNodeWithTag("filter_distance_10km 이내").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("filter_distance_10km 이내").assertIsSelected()
        compose.onNodeWithTag("place_filter_close").performClick()
        compose.onNodeWithTag("place_filter_region").performClick()
        compose.onNodeWithTag("filter_distance_1km 이내").assertIsSelected()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val root = automation.rootInActiveWindow
        // Android 16 warns about the library test APK's target SDK. It is not part of the UI.
        if (root?.packageName?.toString() == "android") {
            root.findAccessibilityNodeInfosByText("OK").forEach { it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
            Thread.sleep(300)
            compose.waitForIdle()
        }
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            // Dialog bounds are offset from the activity; capture the actual display for visual QA.
            automation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
