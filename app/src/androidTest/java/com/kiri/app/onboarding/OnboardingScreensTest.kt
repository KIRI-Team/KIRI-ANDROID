package com.kiri.app.onboarding

import android.content.ClipboardManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.feature.onboarding.model.FamilyRole
import com.kiri.feature.onboarding.presentation.ChooseRoleScreen
import com.kiri.feature.onboarding.presentation.GenerateFamilyCodeLoadingScreen
import com.kiri.feature.onboarding.presentation.GenerateFamilyCodeScreen
import com.kiri.feature.onboarding.presentation.InputFamilyCodeScreen
import com.kiri.feature.onboarding.presentation.InputFamilyNameScreen
import com.kiri.feature.onboarding.presentation.InputNicknameScreen
import com.kiri.feature.onboarding.presentation.OnboardingMainScreen
import com.kiri.feature.onboarding.presentation.component.OnboardingInputBox
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

    @Test
    fun 가족명이_규칙에_맞아야_다음_버튼이_활성화된다() {
        var nextClicked = false
        composeTestRule.setContent {
            var familyName by remember { mutableStateOf("") }
            KiriTheme {
                InputFamilyNameScreen(
                    familyName = familyName,
                    onFamilyNameChange = { familyName = it },
                    onNextClick = { nextClicked = true },
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("끼리에서 사용할\n가족명을 작성해주세요").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_next_button").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("onboarding_input_box").performTextInput("끼리네가족")

        composeTestRule.onNodeWithTag("onboarding_next_button").assertIsEnabled().performClick()
        assertEquals(true, nextClicked)
    }

    @Test
    fun 코드_생성_중에는_안내_문구와_로딩이_보이고_뒤로가기가_비활성화된다() {
        // 로딩 아이콘이 무한히 회전해서 자동 시간 진행을 끈다
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { KiriTheme { GenerateFamilyCodeLoadingScreen() } }
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.onNodeWithText("가족코드를\n생성하고 있어요!").assertIsDisplayed()
        composeTestRule.onNodeWithText("조금만 기다려주세요").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_back_button").assertIsNotEnabled()
    }

    @Test
    fun 완료_화면에서_가족코드를_누르면_클립보드에_복사된다() {
        val clipboard = FakeClipboard()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                KiriTheme {
                    GenerateFamilyCodeScreen(familyCode = "NWTKYJ", onCancelClick = {}, onInviteClick = {}, onBackClick = {})
                }
            }
        }

        composeTestRule.onNodeWithText("NWTKYJ").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_invite_code").performClick()
        composeTestRule.waitForIdle()

        assertEquals("NWTKYJ", clipboard.entry?.clipData?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun 가족코드_입력은_영문_대문자와_숫자_6자리까지만_받는다() {
        var code = ""
        composeTestRule.setContent {
            var familyCode by remember { mutableStateOf("") }
            KiriTheme {
                InputFamilyCodeScreen(
                    familyCode = familyCode,
                    onFamilyCodeChange = {
                        familyCode = it
                        code = it
                    },
                    onCancelClick = {},
                    onInviteClick = {},
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("가족코드를\n입력해주세요!").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_code_input").performTextInput("ab-12c9z")

        assertEquals("AB12C9", code)
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
        composeTestRule.onNodeWithText("9").assertIsDisplayed()
    }

    @Test
    fun 비활성화된_입력창은_입력을_받지_않는다() {
        composeTestRule.setContent {
            KiriTheme {
                OnboardingInputBox(value = "", onValueChange = {}, guides = emptyList(), placeholder = "이름을 입력하세요.", enabled = false)
            }
        }

        composeTestRule.onNodeWithTag("onboarding_input_box").assertIsNotEnabled()
    }

    // 시스템 클립보드 읽기 제한(Android 10+)에 영향받지 않도록 복사된 값을 기록만 한다
    private class FakeClipboard : Clipboard {
        var entry: ClipEntry? = null

        override val nativeClipboard: NativeClipboard
            get() = InstrumentationRegistry.getInstrumentation().targetContext
                .getSystemService(ClipboardManager::class.java)

        override suspend fun getClipEntry(): ClipEntry? = entry

        override suspend fun setClipEntry(clipEntry: ClipEntry?) {
            entry = clipEntry
        }
    }

    @Test
    fun 완료_화면의_취소와_초대하기_버튼이_각각의_동작을_호출한다() {
        val clicked = mutableListOf<String>()
        composeTestRule.setContent {
            KiriTheme {
                GenerateFamilyCodeScreen(
                    familyCode = "NWTKYJ",
                    onCancelClick = { clicked += "cancel" },
                    onInviteClick = { clicked += "invite" },
                    onBackClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("취소").performClick()
        composeTestRule.onNodeWithText("초대하기").performClick()

        assertEquals(listOf("cancel", "invite"), clicked)
    }
}
