package com.kiri.app.mypage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.mypage.model.license.OpenLicense
import com.kiri.feature.mypage.model.license.OpenLicenseUiEvent
import com.kiri.feature.mypage.model.license.OpenLicenseUiState
import com.kiri.feature.mypage.presentation.license.OpenLicenseScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenLicenseScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<OpenLicenseUiEvent>()

    private fun setContent(state: OpenLicenseUiState) {
        composeTestRule.setContent {
            KiriTheme { OpenLicenseScreen(state = state, onEvent = { events += it }) }
        }
    }

    @Test
    fun 라이브러리_이름과_버전이_보이고_누르면_이벤트를_전달한다() {
        val first = OpenLicense.Libraries.first()
        setContent(OpenLicenseUiState())

        composeTestRule.onNodeWithText("오픈라이선스").assertIsDisplayed()
        composeTestRule.onNodeWithText(first.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(first.description).assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_license_item_0").performClick()
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        assertEquals(listOf(OpenLicenseUiEvent.LicenseClicked(first), OpenLicenseUiEvent.BackClicked), events)
    }

    @Test
    fun 팝업에_라이선스_전문이_보이고_닫기를_누르면_닫기를_전달한다() {
        val license = OpenLicense.Libraries.first()
        setContent(OpenLicenseUiState(viewingLicense = license))

        composeTestRule.onNodeWithTag("open_license_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("${license.version} · Apache License 2.0").assertIsDisplayed()
        composeTestRule.onNodeWithText("Licensed under the Apache License", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("open_license_dialog_close").performClick()

        assertEquals(listOf(OpenLicenseUiEvent.LicenseDialogClosed), events)
    }
}
