package com.kiri.feature.detail.presentation

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.domain.detail.Condition
import com.kiri.uikit.KiriTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DetailScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun detailShowsDataScrollsAndOpensCustom() {
        var opened = false
        compose.setContent { KiriTheme { MissionDetailScreen(DetailUiState(false, figmaDetailSample()), {}, { opened = true }) } }
        compose.onNodeWithText("한강 뚜벅뚜벅 축제 가보기").assertIsDisplayed()
        compose.onNodeWithTag("open_custom").performClick()
        assertTrue(opened)
        capture("detail-top")
        compose.onNodeWithText("한강 뚜벅뚜벅 축제 가보기 근처 놀거리추천").performScrollTo().assertIsDisplayed()
        capture("detail-bottom")
    }

    @Test fun customSupportsMultipleSelectionAndApply() {
        val state = mutableStateOf(DetailUiState(false, figmaDetailSample().copy(expectedSteps = 8000, title = "한강 뚜벅뚜벅 축제 방문")))
        var applied: Set<Condition>? = null
        compose.setContent { KiriTheme { ConditionCustomScreen(state.value, {}, { c ->
            val old = state.value
            state.value = old.copy(draft = if (c in old.draft) old.draft - c else old.draft + c)
        }, { applied = state.value.draft }) } }
        capture("custom-default")
        compose.onNodeWithTag("condition_LESS_WALKING").performClick().assertIsOn()
        compose.onNodeWithTag("condition_MORE_REST").performClick().assertIsOn()
        capture("custom-selected")
        compose.onNodeWithTag("condition_LESS_WALKING").performClick().assertIsOff()
        compose.onNodeWithTag("apply_custom").performClick()
        assertEquals(setOf(Condition.MORE_REST), applied)
    }

    @Test fun completedRecordActionIsVisibleButDisabled() {
        var navigated = false
        compose.setContent { KiriTheme { MissionDetailScreen(DetailUiState(false, figmaDetailSample(true)), {}, { navigated = true }) } }
        compose.onNodeWithTag("record_deferred").assertIsDisplayed().assertIsNotEnabled().performClick()
        assertFalse(navigated)
        capture("detail-completed")
    }

    @Test fun savingDisablesAllSelectionAndApply() {
        compose.setContent { KiriTheme { ConditionCustomScreen(DetailUiState(false, figmaDetailSample(), saving = true), {}, {}, {}) } }
        compose.onNodeWithTag("apply_custom").assertIsNotEnabled()
        Condition.entries.forEach { compose.onNodeWithTag("condition_${it.name}").assertIsNotEnabled() }
    }

    @Test fun loadFailureOffersRetryAndBack() {
        var retried = false; var backed = false
        compose.setContent { KiriTheme { MissionDetailScreen(DetailUiState(loading = false, error = "불러오기 실패"), { backed = true }, {}, { retried = true }) } }
        compose.onNodeWithText("다시 시도").performClick(); assertTrue(retried)
        compose.onNodeWithContentDescription("뒤로").performClick(); assertTrue(backed)
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let(::File) ?: requireNotNull(context.getExternalFilesDir(null))
        output.mkdirs()
        val file = File(output, "$name.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
