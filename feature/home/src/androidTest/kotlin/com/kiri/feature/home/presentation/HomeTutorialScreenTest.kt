package com.kiri.feature.home.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.kiri.domain.home.*
import com.kiri.feature.home.HomeViewModel
import com.kiri.uikit.KiriTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

class HomeTutorialScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allThreeStagesFollowLiveTargetsAndFlipIsButtonOnly() {
        val vm = HomeViewModel(Repository(), tutorialRepository = FirstVisit())
        compose.setContent { KiriTheme { HomeRoute(viewModel = vm) } }
        waitFor("home_tutorial_INITIAL")
        assertCardHighlightAligned()
        capture("tutorial-initial")
        compose.onNodeWithTag("home_tutorial_next").performClick()
        waitFor("home_tutorial_MISSION")
        compose.onNodeWithText("우상단 플립 버튼을 눌러", substring = true, useUnmergedTree = true).assertIsDisplayed()
        assertCardHighlightAligned()
        capture("tutorial-mission")
        compose.onNodeWithTag("home_tutorial_next").performClick()
        waitFor("home_tutorial_RECOMMENDATIONS")
        compose.onNodeWithText("끼리가 큐레이팅 한 미션 목록!", useUnmergedTree = true).assertIsDisplayed()
        capture("tutorial-recommendations")
        compose.onNodeWithTag("home_tutorial_next").performClick()
        compose.onNodeWithTag("home_tutorial_RECOMMENDATIONS").assertDoesNotExist()
        compose.onNodeWithTag("home_mission_card").performScrollTo().performTouchInput { swipeRight() }
        compose.onNodeWithTag("home_mission_card").assertIsDisplayed()
        compose.onNodeWithContentDescription("질문 카드로 전환").performClick()
        compose.onNodeWithTag("home_question_card").assertIsDisplayed()
        assertFalse(vm.uiState.value.editing)
        compose.onNodeWithContentDescription("미션 카드로 전환").performClick()
        compose.onNodeWithTag("home_mission_card").assertIsDisplayed()
        assertFalse(vm.uiState.value.editing)
    }

    @Test fun narrowHomeWithLargeTextKeepsGuideAnchoredAndDismissible() {
        val vm = HomeViewModel(Repository(), tutorialRepository = FirstVisit())
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                KiriTheme { Box(Modifier.width(320.dp).height(640.dp)) { HomeRoute(viewModel = vm) } }
            }
        }
        waitFor("home_tutorial_INITIAL")
        assertCardHighlightAligned()
        compose.onNodeWithTag("home_tutorial_next").assertIsDisplayed().performClick()
        waitFor("home_tutorial_MISSION")
        assertCardHighlightAligned()
        capture("tutorial-narrow-large-text")
        compose.onNodeWithTag("home_tutorial_skip").assertIsDisplayed().performClick()
        assertNull(vm.uiState.value.tutorialStep)
    }

    @Test fun rolesUseTheirOwnCharactersAndPokePopupRemainsAttached() {
        val vm = HomeViewModel(Repository())
        compose.setContent { KiriTheme { HomeRoute(viewModel = vm) } }
        waitFor("home_member_mother")
        for (role in listOf("엄마", "아빠", "아들", "딸")) {
            compose.onAllNodesWithContentDescription(role, useUnmergedTree = true).assertCountEquals(1)
        }
        capture("role-avatars")
        compose.onNodeWithTag("home_member_mother").performClick()
        compose.onNodeWithTag("member_answer_tooltip").assertIsDisplayed()
        compose.onNodeWithText("따뜻한 국수").assertIsDisplayed()
        capture("poke-mother-alignment")
    }

    private fun waitFor(tag: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
    }
    private fun assertCardHighlightAligned() {
        compose.waitForIdle()
        val card = compose.onNodeWithTag("home_anchor_CARD", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val highlight = compose.onNodeWithTag("tutorial_highlight_CARD", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val scale = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
        assertEquals(card.left - 4 * scale, highlight.left, 2f)
        assertEquals(card.top - 4 * scale, highlight.top, 2f)
        assertEquals(card.width + 8 * scale, highlight.width, 2f)
        assertEquals(card.height + 8 * scale, highlight.height, 2f)
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = File(context.getExternalFilesDir(null), "home-tutorial-qa").apply { mkdirs() }
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        File(output, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private class FirstVisit : HomeTutorialRepository {
        private var visited = false
        override suspend fun claimFirstVisit(userId: String) = (!visited).also { visited = true }
    }
    private class Repository : HomeRepository {
        private val question = DailyQuestion("q", LocalDate.now(), "오늘 가족과 함께하고 싶은 일은?")
        private val home = FamilyHome(question.date, "son", "끼리네 가족",
            listOf(FamilyMember("son", "나", "아들"), FamilyMember("mother", "마마", "엄마"), FamilyMember("father", "빠빠", "아빠"), FamilyMember("daughter", "딸", "딸")),
            question, mapOf("mother" to "따뜻한 국수"), emptySet(),
            WeeklyMission("m", "광장시장에서 시장음식 먹어보기", "광장시장 · 종로구", listOf("대중교통 편리", "혼잡도 높음"), false, place = "광장시장 · 종로구"),
            recommendations = listOf(HomeRecommendation("p", "경복궁에서 사진찍기", "경복궁 종로구", rating = "4.6", description = "가족과 함께 둘러보기 좋아요")))
        override suspend fun load() = home
        override suspend fun saveAnswer(question: DailyQuestion, answer: String) = home
        override suspend fun poke(question: DailyQuestion, memberId: String) = home
    }
}
