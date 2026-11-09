package com.kiri.feature.place.presentation

import com.kiri.feature.place.model.*

import androidx.compose.ui.unit.dp
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
    @Test fun completedCardShowsResaveAndUnfinishedCardDisablesSaving() {
        val mission = PlaceMission("repeat", "다시 참여할 활동", "장소", "서울", PlaceCategory.OUTDOOR,
            previouslyCompleted = true)
        val state = mutableStateOf(PlaceUiState(loading = false, items = listOf(mission), totalCount = 1))
        var saves = 0
        compose.setContent { KiriTheme { PlaceScreen(state.value) {
            if (it is PlaceUiEvent.SaveClicked) {
                saves++
                state.value = state.value.copy(items = listOf(mission.copy(saved = true)))
            }
        } } }
        compose.onNodeWithContentDescription("다시 버킷에 저장").assertIsDisplayed()
        capture("place-resave")
        compose.onNodeWithTag("place_save_repeat").performClick()
        assertEquals(1, saves)
        compose.onNodeWithTag("place_save_repeat").assertIsNotEnabled()
        compose.onNodeWithContentDescription("저장됨").assertIsDisplayed()
    }
    @Test fun searchCategorySaveCancelConfirmAndToastShortcut() {
        val mission = PlaceMission("one", "광장시장에서 시장음식 먹어보기", "광장시장", "종로구", PlaceCategory.FOOD, rating = "4.6", tags = listOf("대중교통 편리", "혼잡도 높음"), pick = true)
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
        compose.onNodeWithText("활동 48개").assertIsDisplayed()
        compose.onNodeWithText("끼리추천").assertIsDisplayed()
        compose.onNodeWithText("4.6").assertIsDisplayed()
        capture("place-default")
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
    @Test fun missingMetadataDoesNotCreatePlaceholderBadgesOrExtraSaveButton() {
        val mission = PlaceMission("empty", "전통주갤러리 관람하기", "전통주갤러리", "", PlaceCategory.CULTURE)
        compose.setContent { KiriTheme { PlaceScreen(PlaceUiState(loading = false, items = listOf(mission)), {}) } }
        compose.onNodeWithText("정보 없음").assertDoesNotExist()
        compose.onNodeWithText("평가 전").assertDoesNotExist()
        compose.onNodeWithText("버킷에 저장").assertDoesNotExist()
        compose.onNodeWithContentDescription("버킷에 저장").assertIsDisplayed()
        capture("place-empty-metadata")
    }

    @Test fun photoLoadsWithRatingOverlayAndBookmarkStillSaves() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val photo = File(context.cacheDir, "mission-card-test.png")
        photo.outputStream().use { output ->
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.BLUE)
                compress(Bitmap.CompressFormat.PNG, 100, output)
                recycle()
            }
        }
        val mission = PlaceMission("photo", "사진 있는 활동", "장소", "", PlaceCategory.CULTURE,
            rating = "4.6", tags = listOf("무료"), imageUrl = android.net.Uri.fromFile(photo).toString())
        var saves = 0
        compose.setContent { KiriTheme { PlaceScreen(PlaceUiState(loading = false, items = listOf(mission))) {
            if (it is PlaceUiEvent.SaveClicked) saves++
        } } }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("사진 있는 활동 사진").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("place_image_photo").assertWidthIsEqualTo(91.dp).assertHeightIsEqualTo(91.dp)
        compose.onNodeWithText("4.6").assertIsDisplayed()
        compose.onNodeWithText("무료").assertIsDisplayed()
        compose.onNodeWithTag("place_save_photo").performClick()
        assertEquals(1, saves)
        capture("place-photo")
    }

    private fun capture(name: String) {
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use { (if (name == "place-save-dialog") compose.onNodeWithTag("place_save_dialog") else compose.onRoot()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
