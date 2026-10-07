package com.kiri.feature.home.presentation

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import com.kiri.uikit.KiriTheme
import com.kiri.domain.home.HomeRecommendation
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import com.kiri.domain.home.DailyQuestion
import com.kiri.domain.home.FamilyHome
import com.kiri.domain.home.FamilyMember
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.WeeklyMission
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun questionAndMissionFlipAreVisible() {
        val repository = ScreenRepository()
        val viewModel = HomeViewModel(repository)
        compose.setContent { KiriTheme { HomeScreen(viewModel = viewModel) } }
        compose.waitForIdle()

        compose.onNodeWithText("오늘 나의 질문카드").assertIsDisplayed()
        compose.onNodeWithText("오늘 가족과 함께하고 싶은 일은 무엇인가요?", substring = true).assertIsDisplayed()
        capture("home-avatars")
        compose.onNodeWithTag("home_question_card").performTouchInput { swipeLeft() }
        compose.onNodeWithText("이번주 미션이에요!").assertIsDisplayed()
        compose.onNodeWithText("광장시장에서 시장음식 먹어보기").assertIsDisplayed()
        capture("home-mission")
        compose.onNodeWithContentDescription("질문 카드로 전환").performClick()
        compose.onNodeWithTag("home_question_card").assertIsDisplayed()
        compose.onNodeWithTag("home_screen").performTouchInput { swipeUp() }
        capture("home-standard-places")
    }

    @Test
    fun answerEditorKeepsDraftAndSubmitsThroughViewModel() {
        val repository = ScreenRepository()
        val viewModel = HomeViewModel(repository)
        compose.setContent { HomeScreen(viewModel = viewModel) }
        compose.waitForIdle()

        compose.onNodeWithTag("home_question_card").performClick()
        compose.onNodeWithText("답변 제출").assertIsDisplayed()
        compose.onNodeWithContentDescription("오늘의 질문 답변").performTextInput("가족과 산책하기")
        compose.onNodeWithText("답변 제출").performClick()
        compose.waitForIdle()
        assertEquals("가족과 산책하기", repository.home.answers["me"])
    }

    @Test
    fun memberSelectionShowsAnswerToast() {
        val viewModel = HomeViewModel(ScreenRepository())
        compose.setContent { HomeScreen(viewModel = viewModel) }
        compose.waitForIdle()

        compose.onNodeWithText("엄마").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("엄마의 가장 좋아하는 음식은", substring = true).assertIsDisplayed()
        compose.onNodeWithText("같이 산책하기").assertIsDisplayed()
    }

    private class ScreenRepository : HomeRepository {
        private val question = DailyQuestion("q", LocalDate.of(2026, 10, 3), "오늘 가족과 함께하고 싶은 일은 무엇인가요?")
        var home = FamilyHome(
            question.date,
            "me",
            "우리 가족",
            listOf(FamilyMember("me", "나", "자녀"), FamilyMember("mom", "엄마", "엄마")),
            question,
            mapOf("mom" to "같이 산책하기"),
            emptySet(),
            WeeklyMission("m", "광장시장에서 시장음식 먹어보기", "광장시장 · 종로구", listOf("대중교통 편리", "혼잡도 높음"), false, place = "광장시장 · 종로구"),
            recommendations = listOf(HomeRecommendation("place", "경복궁에서 사진찍기", "경복궁 종로구", rating = "4.6", description = "대중교통이 편리하고, 부모님이랑 가기 좋아요! 추천합니다")),
        )

        override suspend fun load() = home
        override suspend fun saveAnswer(question: DailyQuestion, answer: String): FamilyHome {
            home = home.copy(answers = home.answers + ("me" to answer))
            return home
        }
        override suspend fun poke(question: DailyQuestion, memberId: String) = home
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")?.let(::File)
            ?: requireNotNull(context.getExternalFilesDir(null))
        output.mkdirs()
        File(output, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
