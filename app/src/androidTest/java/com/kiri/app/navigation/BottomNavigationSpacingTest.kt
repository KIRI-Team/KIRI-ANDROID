package com.kiri.app.navigation

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.app.HiltTestActivity
import com.kiri.uikit.KiriTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class BottomNavigationSpacingTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()
    @Before fun setup() { hilt.inject() }
    @Test fun everyTabReservesTheEntireFloatingBarAndPreservesSelection() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home_question_card").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithTag("home_tutorial_skip").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithTag("home_tutorial_skip").performClick()
        listOf("home", "place", "record", "mypage").forEach { tab ->
            compose.onNodeWithTag("bottom_nav_$tab").performClick().assertIsSelected()
            val content = compose.onNodeWithTag("main_content").fetchSemanticsNode().boundsInRoot
            val area = compose.onNodeWithTag("bottom_nav_area").fetchSemanticsNode().boundsInRoot
            assertTrue("$tab content must finish above the bar including its outer gap", content.bottom <= area.top + 1f)
            val bar = compose.onNodeWithTag("floating_bottom_bar").fetchSemanticsNode().boundsInRoot
            assertTrue("$tab requires space above the pill", bar.top > content.bottom)
            val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
            File(directory, "floating-nav-$tab.png").outputStream().use {
                compose.onNodeWithTag("floating_bottom_bar").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        compose.onNodeWithTag("bottom_nav_place").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("place_list").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("place_list").performScrollToNode(hasTestTag("place_card_place-experience"))
        val last = compose.onNodeWithTag("place_save_place-experience").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val bar = compose.onNodeWithTag("floating_bottom_bar").fetchSemanticsNode().boundsInRoot
        assertTrue("Last bookmark button must be fully above the floating bar", last.bottom <= bar.top)
        compose.onNodeWithTag("place_save_place-experience").performClick()
        compose.onNodeWithTag("place_confirm_save").assertIsDisplayed()
    }
}
