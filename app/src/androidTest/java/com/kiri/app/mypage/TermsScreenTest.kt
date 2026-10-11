package com.kiri.app.mypage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.domain.model.term.AgreementTerm
import com.kiri.feature.mypage.model.terms.TermsUiEvent
import com.kiri.feature.mypage.model.terms.TermsUiState
import com.kiri.feature.mypage.presentation.terms.TermsScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TermsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<TermsUiEvent>()

    private fun setContent(state: TermsUiState) {
        composeTestRule.setContent {
            KiriTheme { TermsScreen(state = state, onEvent = { events += it }) }
        }
    }

    @Test
    fun 약관_목록과_최종_업데이트일이_보이고_약관을_누르면_이벤트를_전달한다() {
        setContent(TermsUiState())

        composeTestRule.onNodeWithText("세부 약관").assertIsDisplayed()
        composeTestRule.onNodeWithText("최종 업데이트 | 26. 06. 26").assertIsDisplayed()
        composeTestRule.onNodeWithTag("terms_detail_sheet").assertDoesNotExist()
        composeTestRule.onNodeWithTag("terms_item_term_3").performClick()
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        assertEquals(listOf(TermsUiEvent.TermClicked(AgreementTerm.TERM_3), TermsUiEvent.BackClicked), events)
    }

    @Test
    fun 세부_내용_창에는_동의하기_대신_확인이_있고_누르면_닫기를_전달한다() {
        setContent(TermsUiState(viewingTerm = AgreementTerm.TERM_1))

        composeTestRule.onNodeWithTag("terms_detail_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("제1조 (목적)", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("동의하기").assertDoesNotExist()
        composeTestRule.onNodeWithTag("terms_detail_confirm").assertIsDisplayed().performClick()

        assertEquals(listOf(TermsUiEvent.TermDetailClosed), events)
    }
}
