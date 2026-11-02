package com.kiri.app.mypage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.domain.model.nickname.NicknameRule
import com.kiri.feature.mypage.model.nameedit.NameEditUiEvent
import com.kiri.feature.mypage.model.nameedit.NameEditUiState
import com.kiri.feature.mypage.presentation.nameedit.NicknameEditScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NicknameEditScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<NameEditUiEvent>()

    private fun setContent(state: NameEditUiState) {
        composeTestRule.setContent {
            KiriTheme { NicknameEditScreen(state = state, onEvent = { events += it }) }
        }
    }

    @Test
    fun 제목과_지금_닉네임_안내가_보인다() {
        setContent(NameEditUiState(currentName = "쥬123"))

        composeTestRule.onNodeWithText("닉네임을 입력하세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("닉네임은 월 1회 수정 가능합니다").assertIsDisplayed()
        composeTestRule.onNodeWithText("지금 닉네임: 쥬123").assertIsDisplayed()
    }

    @Test
    fun 입력과_다음_뒤로가기를_이벤트로_전달한다() {
        setContent(NameEditUiState(currentName = "쥬123"))

        composeTestRule.onNodeWithTag("mypage_input_box").performTextInput("끼리")
        composeTestRule.onNodeWithTag("kiri_button").performClick()
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        assertEquals(
            listOf(
                NameEditUiEvent.NameChanged("끼리"),
                NameEditUiEvent.NextClicked,
                NameEditUiEvent.BackClicked,
            ),
            events,
        )
    }

    @Test
    fun 지키지_못한_규칙만_오류_문구로_보여준다() {
        setContent(NameEditUiState(name = "쥬", violations = setOf(NicknameRule.LENGTH)))

        composeTestRule.onNodeWithText("최소 2자 ~ 최대 12자 이내로 입력해주세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("특수 문자는 입력할 수 없어요").assertDoesNotExist()
    }
}
