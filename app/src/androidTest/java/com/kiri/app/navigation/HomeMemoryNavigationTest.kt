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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@HiltAndroidTest
class HomeMemoryNavigationTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @Before fun setUp() { hiltRule.inject() }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()
    @Test fun homeInboxMemoryEmptyAndEditorReturnKeepSelectedTab() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("home_taste_banner").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithTag("home_tutorial_skip").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithTag("home_tutorial_skip").performClick()
        compose.onNodeWithTag("home_taste_banner").performScrollTo().assertIsDisplayed()
        capture("home-banner")
        compose.onNodeWithTag("open_notifications").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("notification_demo-question").fetchSemanticsNodes().isNotEmpty() }
        capture("notifications")
        compose.onNodeWithTag("notification_filter_RECORD").performClick()
        compose.onNodeWithTag("notification_demo-memory").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("family_memories_empty").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("mission_memory_tab").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("family_memories_empty").performScrollTo().assertIsDisplayed()
        capture("family-memory-empty")
        compose.onNodeWithTag("open_memory_editor").performScrollTo().performClick()
        compose.onNodeWithTag("memory_editor").assertIsDisplayed()
        compose.onNodeWithTag("submit_memory").assertIsNotEnabled()
        capture("memory-editor")
        compose.onNodeWithTag("memory_rating_3").performClick()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.onNodeWithTag("mission_memory_tab").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("open_memory_editor").performScrollTo().performClick()
        compose.onNodeWithTag("memory_rating_3").assertIsSelected()
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "home-memory-qa").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
