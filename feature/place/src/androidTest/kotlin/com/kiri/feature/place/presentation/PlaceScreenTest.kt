package com.kiri.feature.place.presentation

import com.kiri.feature.place.model.*

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.domain.place.*
import com.kiri.uikit.KiriTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class PlaceScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun searchCategorySaveCancelConfirmAndToastShortcut() {
        val mission = PlaceMission("one", "광장시장에서 시장음식 먹어보기", "광장시장", "종로구", PlaceCategory.FOOD, pick = true)
        val state = mutableStateOf(PlaceUiState(items = listOf(mission), totalCount = 48, loading = false))
        var keyword = ""
        compose.setContent { KiriTheme { PlaceScreen(state.value) { event ->
            when (event) {
                is PlaceUiEvent.SearchChanged -> { keyword = event.keyword; state.value = state.value.copy(query = state.value.query.copy(keyword = event.keyword)) }
                is PlaceUiEvent.CategorySelected -> state.value = state.value.copy(query = state.value.query.copy(category = event.category))
                is PlaceUiEvent.SaveClicked -> state.value = state.value.copy(pending = event.mission)
                PlaceUiEvent.SaveConfirmed -> state.value = state.value.copy(pending = null, items = listOf(mission.copy(saved = true)), savedItems = listOf(mission.copy(saved = true)), toast = true)
                PlaceUiEvent.SaveDismissed -> state.value = state.value.copy(pending = null)
                PlaceUiEvent.NoticeDismissed -> state.value = state.value.copy(toast = false)
                PlaceUiEvent.SavedClicked -> state.value = state.value.copy(showingSaved = true, toast = false)
                PlaceUiEvent.SavedBackClicked -> state.value = state.value.copy(showingSaved = false)
                else -> Unit
            }
        } } }
        compose.onNodeWithText("활동 48개").assertIsDisplayed(); capture("place-default")
        compose.onNodeWithTag("place_search").performTextInput("광장")
        assertEquals("광장", keyword)
        compose.onNodeWithTag("place_category_CULTURE").performClick()
        compose.runOnIdle { assertEquals(PlaceCategory.CULTURE, state.value.query.category) }
        compose.onNodeWithTag("place_save_one").performClick(); capture("place-save-dialog")
        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithTag("place_save_dialog").assertDoesNotExist()
        compose.onNodeWithTag("place_save_one").performClick()
        compose.onNodeWithTag("place_confirm_save").performClick()
        compose.onNodeWithContentDescription("저장됨").assertIsDisplayed(); capture("place-saved-toast")
        compose.onNodeWithText("바로가기").performClick()
        compose.onNodeWithText("저장한 활동 1개").assertIsDisplayed()
    }
    private fun capture(name: String) {
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { (if (name == "place-save-dialog") compose.onNodeWithTag("place_save_dialog") else compose.onRoot()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
