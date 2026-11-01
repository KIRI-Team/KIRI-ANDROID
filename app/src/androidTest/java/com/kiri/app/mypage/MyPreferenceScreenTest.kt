package com.kiri.app.mypage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.mypage.model.preference.MyPreferenceUiEvent
import com.kiri.feature.mypage.model.preference.MyPreferenceUiState
import com.kiri.feature.mypage.model.preference.PreferenceActivity
import com.kiri.feature.mypage.model.preference.PreferenceCategory
import com.kiri.feature.mypage.model.preference.PreferenceDuration
import com.kiri.feature.mypage.model.preference.PreferenceSpace
import com.kiri.feature.mypage.presentation.preference.MyPreferenceScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyPreferenceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<MyPreferenceUiEvent>()

    private fun setContent(state: MyPreferenceUiState) {
        composeTestRule.setContent {
            KiriTheme { MyPreferenceScreen(state = state, onEvent = { events += it }) }
        }
    }

    @Test
    fun 닉네임이_들어간_제목과_항목별_선택지가_보인다() {
        setContent(MyPreferenceUiState(nickname = "쥬123"))

        composeTestRule.onNodeWithText("내 취향 설정").assertIsDisplayed()
        composeTestRule.onNodeWithText("쥬123님, 가족과 어떤 시간을\n함께하고 싶나요?").assertIsDisplayed()
        composeTestRule.onNodeWithText("선호 활동 · 최대 3개").assertIsDisplayed()
        composeTestRule.onNodeWithText("많이 움직여도 괜찮음").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("반나절이상").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("취향 저장하기").assertIsDisplayed()
    }

    @Test
    fun 저장된_취향이_없으면_아무_칩도_선택되지_않는다() {
        setContent(MyPreferenceUiState(nickname = "쥬123"))

        composeTestRule.onNodeWithTag("preference_category_EXHIBITION").assertIsOff()
        composeTestRule.onNodeWithTag("preference_space_INDOOR").assertIsNotSelected()
    }

    @Test
    fun 고른_항목은_선택된_칩으로_보인다() {
        setContent(
            MyPreferenceUiState(
                nickname = "쥬123",
                categories = setOf(PreferenceCategory.EXHIBITION),
                space = PreferenceSpace.INDOOR,
                duration = PreferenceDuration.WITHIN_1H,
            ),
        )

        composeTestRule.onNodeWithTag("preference_category_EXHIBITION").assertIsOn()
        composeTestRule.onNodeWithTag("preference_category_POPUP").assertIsOff()
        composeTestRule.onNodeWithTag("preference_space_INDOOR").assertIsSelected()
        composeTestRule.onNodeWithTag("preference_duration_WITHIN_1H").performScrollTo().assertIsSelected()
    }

    @Test
    fun 선호_활동과_활동량을_고르기_전에는_저장할_수_없다() {
        setContent(MyPreferenceUiState(nickname = "쥬123", categories = setOf(PreferenceCategory.EXHIBITION)))

        composeTestRule.onNodeWithTag("kiri_button").assertIsNotEnabled()
    }

    @Test
    fun 칩_저장_뒤로가기를_이벤트로_전달한다() {
        setContent(
            MyPreferenceUiState(
                nickname = "쥬123",
                categories = setOf(PreferenceCategory.EXHIBITION),
                activity = PreferenceActivity.LIGHT,
            ),
        )

        composeTestRule.onNodeWithTag("preference_category_FOOD_CAFE").performClick()
        composeTestRule.onNodeWithTag("preference_space_OUTDOOR").performClick()
        composeTestRule.onNodeWithTag("preference_duration_ANY").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("kiri_button").performClick()
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        assertEquals(
            listOf(
                MyPreferenceUiEvent.CategoryClicked(PreferenceCategory.FOOD_CAFE),
                MyPreferenceUiEvent.SpaceClicked(PreferenceSpace.OUTDOOR),
                MyPreferenceUiEvent.DurationClicked(PreferenceDuration.ANY),
                MyPreferenceUiEvent.SaveClicked,
                MyPreferenceUiEvent.BackClicked,
            ),
            events,
        )
    }
}
