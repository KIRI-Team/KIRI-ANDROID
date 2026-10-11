package com.kiri.app.mypage

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.mypage.model.suggestion.SuggestionUiEvent
import com.kiri.feature.mypage.model.suggestion.SuggestionUiState
import com.kiri.feature.mypage.presentation.suggestion.SuggestionScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SuggestionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<SuggestionUiEvent>()

    private fun setContent(state: SuggestionUiState) {
        composeTestRule.setContent {
            KiriTheme { SuggestionScreen(state = state, onEvent = { events += it }) }
        }
    }

    @Test
    fun 제목과_안내_문구와_글자_수가_보인다() {
        setContent(SuggestionUiState())

        composeTestRule.onNodeWithText("건의함").assertIsDisplayed()
        composeTestRule.onNodeWithText("끼리가 더 발전할 수 있도록\n얘기해주세요!").assertIsDisplayed()
        composeTestRule.onNodeWithText("끼리를 사용하면서 말해주고 싶으셨던 점에 대해\n자유롭게 얘기해주세요!").assertIsDisplayed()
        composeTestRule.onNodeWithTag("suggestion_content_count").assertIsDisplayed()
        composeTestRule.onNodeWithText("0/500").assertIsDisplayed()
        composeTestRule.onNodeWithText("관련 화면을 함께 보내주시면 도움이 돼요.").assertIsDisplayed()
        composeTestRule.onNodeWithText("의견 보내기").assertIsDisplayed()
    }

    @Test
    fun 입력_사진추가_보내기_뒤로가기를_이벤트로_전달한다() {
        setContent(SuggestionUiState())

        composeTestRule.onNodeWithTag("suggestion_content_input").performTextInput("좋아요")
        composeTestRule.onNodeWithTag("suggestion_photo_add").performClick()
        composeTestRule.onNodeWithTag("kiri_button").performClick()
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        assertEquals(
            listOf(
                SuggestionUiEvent.ContentChanged("좋아요"),
                SuggestionUiEvent.PhotoAddClicked,
                SuggestionUiEvent.SendClicked,
                SuggestionUiEvent.BackClicked,
            ),
            events,
        )
    }

    @Test
    fun 고른_사진마다_빼기_버튼이_있고_누르면_그_사진을_뺀다() {
        setContent(SuggestionUiState(photoUris = listOf("content://photo/0", "content://photo/1")))

        composeTestRule.onAllNodesWithTag("suggestion_photo").assertCountEquals(2)
        composeTestRule.onAllNodesWithTag("suggestion_photo_remove")[1].performScrollTo().performClick()

        assertEquals(listOf(SuggestionUiEvent.PhotoRemoveClicked("content://photo/1")), events)
    }

    @Test
    fun 사진이_가득_차면_사진_추가_칸이_사라진다() {
        setContent(SuggestionUiState(photoUris = List(SuggestionUiState.MAX_PHOTO_COUNT) { "content://photo/$it" }))

        composeTestRule.onNodeWithTag("suggestion_photo_add").assertDoesNotExist()
    }
}
