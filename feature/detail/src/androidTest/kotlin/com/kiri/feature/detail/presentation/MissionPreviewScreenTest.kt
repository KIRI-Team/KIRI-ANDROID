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

    @Test fun placeDetailOmitsSaveAction() {
        compose.setContent {
            KiriTheme {
                MissionPreviewScreen(
                    MissionPreviewUiState(loading = false, preview = MissionPreview(figmaDetailSample())),
                    {}, {}, {}, showSaveAction = false,
                )
            }
        }
        compose.onNodeWithTag("detail_header").assertIsDisplayed()
        compose.onNodeWithTag("preview_save").assertDoesNotExist()
    }

    @Test fun bucketSaveButtonStaysInsideTopBox() {
        compose.setContent { KiriTheme { MissionPreviewScreen(MissionPreviewUiState(loading = false, preview = MissionPreview(figmaDetailSample()),
            error = "버킷에 저장하지 못했어요."), {}, {}, {}) } }

        val header = compose.onNodeWithTag("detail_header").fetchSemanticsNode().boundsInRoot
        val save = compose.onNodeWithTag("preview_save").fetchSemanticsNode().boundsInRoot
        val error = compose.onNodeWithText("버킷에 저장하지 못했어요.").fetchSemanticsNode().boundsInRoot
        // 버튼과 오류 문구가 상단 박스(흰 배경) 안쪽에 있어야 한다
        org.junit.Assert.assertTrue("save=$save header=$header", save.top >= header.top && save.bottom <= header.bottom)
        org.junit.Assert.assertTrue("error=$error header=$header", error.bottom <= header.bottom)
        org.junit.Assert.assertTrue(save.left >= header.left && save.right <= header.right)
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = instrumentation.targetContext.getExternalFilesDir(null)!!
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
