package com.kiri.app.mypage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.domain.model.term.Term
import com.kiri.feature.mypage.model.terms.TermsUiEvent
import com.kiri.feature.mypage.model.terms.TermsUiState
import com.kiri.feature.mypage.presentation.terms.TermsScreen
import com.kiri.uikit.KiriTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TermsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<TermsUiEvent>()

    private val terms = listOf(
        Term(id = 1, title = "서비스 이용약관", content = "제1조 (목적) 본 약관은 ...", required = true, updatedAt = LocalDate.of(2026, 10, 4)),
        Term(id = 3, title = "마케팅 정보 수신 동의", content = "이벤트, 혜택 등 ...", required = false),
    )

    private fun setContent(state: TermsUiState) {
        composeTestRule.setContent {
            KiriTheme { TermsScreen(state = state, onEvent = { events += it }) }
        }
    }

    @Test
    fun 서버_약관_목록과_최종_업데이트일이_보이고_약관을_누르면_이벤트를_전달한다() {
        setContent(TermsUiState(terms = terms))

        composeTestRule.onNodeWithText("세부 약관").assertIsDisplayed()
        composeTestRule.onNodeWithText("서비스 이용약관").assertIsDisplayed()
        composeTestRule.onNodeWithText("마케팅 정보 수신 동의").assertIsDisplayed()
        composeTestRule.onNodeWithText("최종 업데이트 | 26. 10. 04").assertIsDisplayed()
        composeTestRule.onNodeWithTag("terms_detail_sheet").assertDoesNotExist()
        composeTestRule.onNodeWithTag("terms_item_3").performClick()
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        assertEquals(listOf(TermsUiEvent.TermClicked(terms[1]), TermsUiEvent.BackClicked), events)
    }

    @Test
    fun 약관을_받는_동안에는_로딩만_보인다() {
        setContent(TermsUiState(isLoading = true))

        composeTestRule.onNodeWithTag("kiri_loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("terms_item_1").assertDoesNotExist()
    }

    @Test
    fun 약관을_받지_못하면_다시_시도를_누를_수_있다() {
        setContent(TermsUiState(loadFailed = true))

        composeTestRule.onNodeWithText("약관을 불러오지 못했어요").assertIsDisplayed()
        composeTestRule.onNodeWithTag("terms_retry_button").performClick()

        assertEquals(listOf(TermsUiEvent.RetryClicked), events)
    }

    @Test
    fun 세부_내용_창에는_동의하기_대신_확인이_있고_누르면_닫기를_전달한다() {
        setContent(TermsUiState(terms = terms, viewingTerm = terms[0]))

        composeTestRule.onNodeWithTag("terms_detail_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("제1조 (목적)", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("동의하기").assertDoesNotExist()
        composeTestRule.onNodeWithTag("terms_detail_confirm").assertIsDisplayed().performClick()

        assertEquals(listOf(TermsUiEvent.TermDetailClosed), events)
    }
}
