package com.kiri.app.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.onboarding.model.FamilyRole
import com.kiri.feature.onboarding.presentation.ChooseRoleScreen
import com.kiri.feature.onboarding.presentation.InputNicknameScreen
import com.kiri.feature.onboarding.presentation.OnboardingMainScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 닉네임이_비어있으면_안내_문구가_보인다() {
        composeTestRule.setContent {
            KiriTheme {
                InputNicknameScreen(nickname = "", onNicknameChange = {}, onNextClick = {}, onBackClick = {})
            }
        }

        composeTestRule.onNodeWithText("이름을 입력하세요.").assertIsDisplayed()
    }

    @Test
    fun 역할_카드를_누르면_해당_역할만_선택된다() {
        composeTestRule.setContent {
            var selectedRole by remember { mutableStateOf<FamilyRole?>(null) }
            KiriTheme {
                ChooseRoleScreen(
                    nickname = "주연주",
                    selectedRole = selectedRole,
                    onRoleSelect = { selectedRole = it },
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("주연주님의\n역할을 선택해주세요.").assertIsDisplayed()
        composeTestRule.onNodeWithText("아빠").performClick()

        composeTestRule.onNodeWithText("아빠").assertIsSelected()
        composeTestRule.onNodeWithText("엄마").assertIsNotSelected()

        composeTestRule.onNodeWithText("딸").performClick()

        composeTestRule.onNodeWithText("딸").assertIsSelected()
        composeTestRule.onNodeWithText("아빠").assertIsNotSelected()
    }

    @Test
    fun 메인_메뉴_카드를_누르면_각각의_동작이_호출된다() {
        val clicked = mutableListOf<String>()
        composeTestRule.setContent {
            KiriTheme {
                OnboardingMainScreen(
                    onCreateFamilyClick = { clicked += "create" },
                    onInputFamilyCodeClick = { clicked += "code" },
                    onBrowseClick = { clicked += "browse" },
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("가족공간 만들러 가기").performClick()
        composeTestRule.onNodeWithText("가족코드 입력하기").performClick()
        composeTestRule.onNodeWithText("먼저 둘러보러가기").performClick()

        assertEquals(listOf("create", "code", "browse"), clicked)
    }
}
