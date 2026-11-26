package com.kiri.app.mypage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.mypage.model.common.MypageMenu
import com.kiri.feature.mypage.model.common.MypageProfile
import com.kiri.feature.mypage.model.common.MypageSection
import com.kiri.feature.mypage.presentation.main.MypageScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MypageScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 프로필과_섹션별_메뉴가_보인다() {
        composeTestRule.setContent {
            KiriTheme {
                MypageScreen(onMenuClick = {}, profile = MypageProfile(nickname = "주연", familyName = "끼리네", role = "엄마"))
            }
        }

        composeTestRule.onNodeWithText("주연").assertIsDisplayed()
        composeTestRule.onNodeWithText("끼리네 I 엄마").assertIsDisplayed()
        // 상단 제목과 마지막 섹션 제목이 모두 "설정"이라 섹션 제목은 마지막 노드로 찾는다
        MypageSection.entries.forEach {
            composeTestRule.onAllNodesWithText(it.title).onLast().performScrollTo().assertIsDisplayed()
        }
        MypageMenu.entries.forEach {
            composeTestRule.onNodeWithText(it.title).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun 메뉴를_누르면_해당_메뉴를_전달한다() {
        val clicked = mutableListOf<MypageMenu>()
        composeTestRule.setContent {
            KiriTheme { MypageScreen(onMenuClick = { clicked += it }) }
        }

        MypageMenu.entries.forEach {
            composeTestRule.onNodeWithTag("mypage_menu_${it.name.lowercase()}").performScrollTo().performClick()
        }

        assertEquals(MypageMenu.entries, clicked)
    }
}
