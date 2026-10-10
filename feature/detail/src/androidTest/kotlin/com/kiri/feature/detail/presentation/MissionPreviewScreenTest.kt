package com.kiri.feature.detail.presentation

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.domain.detail.MissionPreview
import com.kiri.domain.mission.MissionReview
import com.kiri.feature.detail.model.MissionPreviewUiState
import com.kiri.uikit.KiriTheme
import java.io.File
import org.junit.Rule
import org.junit.Test

class MissionPreviewScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun recommendationAndReviewTabsShareHeaderAndCanSwitch() {
        val state = mutableStateOf(MissionPreviewUiState(loading = false, preview = MissionPreview(
            figmaDetailSample(), List(3) { MissionReview("review-$it", "USER)@#@#@", "아빠가 자주 가는 식당에 처음 가봤다. 좋아하는 메뉴를 같이 먹으니 아빠를 조금 더 알게 된 기분이었다.", 10) }, curated = true,
        )))
        compose.setContent { KiriTheme { MissionPreviewScreen(state.value, {}, {}, { state.value = state.value.copy(reviewsSelected = it) }) } }
        compose.onNodeWithTag("preview_activity_tab").assertIsSelected()
        compose.onNodeWithTag("open_custom").assertDoesNotExist()
        capture("preview-activity-top")
        compose.onNodeWithText("한강 뚜벅뚜벅 축제 가보기 근처 놀거리추천").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("mission_preview_screen").performTouchInput { swipeUp() }
        capture("preview-activity-nearby")
        compose.onNodeWithTag("preview_review_tab").performScrollTo().performClick()
        compose.onNodeWithTag("preview_review_tab").assertIsSelected()
        compose.onNodeWithTag("mission_review_review-0").assertIsDisplayed()
        compose.onNodeWithText("한강 뚜벅뚜벅 축제 가보기").performScrollTo()
        capture("preview-review")
        compose.onNodeWithTag("preview_activity_tab").performScrollTo().performClick()
        compose.onNodeWithTag("preview_activity_tab").assertIsSelected()
        compose.onNodeWithTag("mission_review_review-0").assertDoesNotExist()
    }

    @Test fun emptyReviewsAndLoadingFailureAreVisibleAndRetryable() {
        var retries = 0
        val state = mutableStateOf(MissionPreviewUiState(loading = false, error = "연결 오류"))
        compose.setContent { KiriTheme { MissionPreviewScreen(state.value, {}, { retries++ }, {}) } }
        compose.onNodeWithText("다시 시도").performClick()
        org.junit.Assert.assertEquals(1, retries)
        compose.runOnIdle { state.value = MissionPreviewUiState(loading = false, preview = MissionPreview(figmaDetailSample()), reviewsSelected = true) }
        compose.onNodeWithText("아직 등록된 리뷰가 없어요.").performScrollTo().assertIsDisplayed()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = instrumentation.targetContext.getExternalFilesDir(null)!!
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
