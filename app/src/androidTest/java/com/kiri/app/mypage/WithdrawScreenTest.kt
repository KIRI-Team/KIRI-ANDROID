package com.kiri.app.mypage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kiri.feature.mypage.presentation.withdraw.WithdrawScreen
import com.kiri.uikit.KiriTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WithdrawScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun 안내_문구가_보이고_탈퇴할게요와_뒤로가기를_전달한다() {
        val clicks = mutableListOf<String>()
        composeTestRule.setContent {
            KiriTheme { WithdrawScreen(onBackClick = { clicks += "back" }, onWithdrawClick = { clicks += "withdraw" }) }
        }

        composeTestRule.onNodeWithText("잠깐! 떠나기 전에 확인해주세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("삭제되는 내 정보").assertIsDisplayed()
        composeTestRule.onNodeWithText("기록의 작성자는 ‘탈퇴한 멤버’로 표시돼요.").assertIsDisplayed()
        composeTestRule.onNodeWithText("탈퇴할게요").performClick()
        composeTestRule.onNodeWithTag("kiri_back_button").performClick()

        assertEquals(listOf("withdraw", "back"), clicks)
    }
}
