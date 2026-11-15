package com.kiri.app.mypage

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.mypage.model.family.FamilyMember
import com.kiri.feature.mypage.model.family.FamilySettingUiState
import com.kiri.feature.mypage.model.nameedit.NameEditUiEvent
import com.kiri.feature.mypage.model.nameedit.NameEditUiState
import com.kiri.feature.mypage.presentation.family.FamilySettingScreen
import com.kiri.feature.mypage.presentation.nameedit.FamilyNameEditScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilySettingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val members = listOf(
        FamilyMember(role = "딸", nickname = "쥬123", isMe = true, id = "1"),
        FamilyMember(role = "아들", nickname = "첫째", id = "2"),
        FamilyMember(role = "", nickname = "막내", id = "3"),
        FamilyMember(role = "아빠", nickname = "빠빠", id = "4"),
    )
    private val loaded = FamilySettingUiState(loading = false, familyName = "끼리네가족", members = members)

    @Test
    fun 가족_이름과_구성원_수_닉네임_역할이_보이고_내_카드에만_수정_아이콘이_있다() {
        composeTestRule.setContent {
            KiriTheme {
                FamilySettingScreen(onBackClick = {}, onFamilyNameEditClick = {}, onMyNicknameEditClick = {}, state = loaded)
            }
        }

        composeTestRule.onNodeWithText("가족 기본 설정").assertIsDisplayed()
        composeTestRule.onNodeWithText("현재 가족 이름").assertIsDisplayed()
        composeTestRule.onNodeWithText("끼리네가족").assertIsDisplayed()
        composeTestRule.onNodeWithTag("family_member_count").assertTextEquals("4")
        members.forEach { composeTestRule.onNodeWithText(it.nickname).assertIsDisplayed() }
        listOf("딸", "아들", "아빠").forEach { composeTestRule.onNodeWithText(it).assertIsDisplayed() }
        composeTestRule.onAllNodesWithTag("family_member_nickname_edit").assertCountEquals(1)
    }

    @Test
    fun 불러오지_못하면_다시_시도를_전달하고_가족이_없으면_가족명_수정_아이콘이_없다() {
        var retries = 0
        val state = androidx.compose.runtime.mutableStateOf(FamilySettingUiState(loading = false, failed = true))
        composeTestRule.setContent {
            KiriTheme {
                FamilySettingScreen(onBackClick = {}, onFamilyNameEditClick = {}, onMyNicknameEditClick = {},
                    state = state.value, onRetry = { retries++ })
            }
        }

        composeTestRule.onNodeWithText("가족 정보를 불러오지 못했어요.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("family_setting_retry").performClick()
        assertEquals(1, retries)
        composeTestRule.onNodeWithTag("family_name_edit").assertDoesNotExist()

        composeTestRule.runOnIdle { state.value = FamilySettingUiState(loading = false) }
        composeTestRule.onNodeWithText("가족에 참여하면 구성원을 볼 수 있어요.").assertIsDisplayed()
    }

    @Test
    fun 방장이_아니면_가족명_수정_아이콘을_보여주지_않는다() {
        // 나는 1번, 방장은 4번
        composeTestRule.setContent {
            KiriTheme {
                FamilySettingScreen(onBackClick = {}, onFamilyNameEditClick = {}, onMyNicknameEditClick = {},
                    state = loaded.copy(ownerId = "4"))
            }
        }

        composeTestRule.onNodeWithText("끼리네가족").assertIsDisplayed()
        composeTestRule.onNodeWithTag("family_name_edit").assertDoesNotExist()
        // 내 닉네임 수정은 그대로 할 수 있다
        composeTestRule.onAllNodesWithTag("family_member_nickname_edit").assertCountEquals(1)
    }

    @Test
    fun 수정_아이콘과_뒤로가기를_누르면_각각_전달한다() {
        val clicks = mutableListOf<String>()
        composeTestRule.setContent {
            KiriTheme {
                FamilySettingScreen(
                    onBackClick = { clicks += "back" },
                    onFamilyNameEditClick = { clicks += "familyName" },
                    onMyNicknameEditClick = { clicks += "nickname" },
                    state = loaded,
                )
            }
        }

        composeTestRule.onNodeWithTag("family_name_edit").performClick()
        composeTestRule.onNodeWithTag("family_member_nickname_edit").performClick()
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        assertEquals(listOf("familyName", "nickname", "back"), clicks)
    }

    @Test
    fun 가족명_수정_화면에_지금_가족명_안내가_보이고_다음을_전달한다() {
        val events = mutableListOf<NameEditUiEvent>()
        composeTestRule.setContent {
            KiriTheme {
                FamilyNameEditScreen(state = NameEditUiState(currentName = "끼리네가족"), onEvent = { events += it })
            }
        }

        composeTestRule.onNodeWithText("가족명 수정").assertIsDisplayed()
        composeTestRule.onNodeWithText("가족명을 수정해요").assertIsDisplayed()
        composeTestRule.onNodeWithText("가족명은 월 1회 수정 가능합니다").assertIsDisplayed()
        composeTestRule.onNodeWithText("현재 가족 이름: 끼리네가족").assertIsDisplayed()
        composeTestRule.onNodeWithText("저장하기").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kiri_button").performClick()

        assertEquals(listOf(NameEditUiEvent.NextClicked), events)
    }

    @Test
    fun 이미_있는_가족명이면_가족명_수정_화면에_중복_안내가_보인다() {
        composeTestRule.setContent {
            KiriTheme {
                FamilyNameEditScreen(
                    state = NameEditUiState(currentName = "끼리네가족", name = "끼리네가족2", isDuplicated = true),
                    onEvent = {},
                )
            }
        }

        composeTestRule.onNodeWithText("이미 존재하는 가족 이름입니다.").assertIsDisplayed()
    }
}
