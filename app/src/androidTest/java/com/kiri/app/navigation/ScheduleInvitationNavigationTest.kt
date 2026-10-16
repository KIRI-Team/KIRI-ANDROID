package com.kiri.app.navigation

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.app.HiltTestActivity
import com.kiri.uikit.KiriTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ScheduleInvitationNavigationTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()
    @Before fun setup() { hilt.inject() }
    @Test fun inboxInvitationReturnsFromExistingDetailAndConfirmsTheSameSchedule() {
        start()
        open("demo-invite")
        compose.onNodeWithText("요청알림").assertIsDisplayed()
        capture("invitation-new")
        compose.onNodeWithTag("invitation_details").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("open_custom").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("mission_detail_screen").assertIsDisplayed()
        compose.onNodeWithContentDescription("뒤로").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("invitation_card").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("약속 확정하기").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("invitation_confirmed").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("확정 완료").assertIsNotEnabled()
        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithTag("notification_list").assertIsDisplayed()
    }
    @Test fun changedInvitationHighlightsTheCurrentDateAndCancelReturnsToInbox() {
        start()
        open("demo-change")
        compose.onNodeWithText("일정 변경알림").assertIsDisplayed()
        compose.onNodeWithTag("invitation_changed").assertIsDisplayed()
        capture("invitation-changed")
        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithTag("notification_list").assertIsDisplayed()
    }
    private fun start() {
        compose.setContent { KiriTheme { MainScreen() } }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("home_question_card").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithTag("home_tutorial_skip").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithTag("home_tutorial_skip").performClick()
        compose.onNodeWithTag("open_notifications").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("notification_list").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("notification_filter_SCHEDULE").performClick()
    }
    private fun open(id: String) {
        compose.onNodeWithTag("notification_list").performScrollToNode(hasTestTag("notification_$id"))
        compose.onNodeWithTag("notification_$id").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("invitation_card").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
