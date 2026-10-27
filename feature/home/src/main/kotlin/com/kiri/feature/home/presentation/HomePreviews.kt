package com.kiri.feature.home.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.kiri.domain.home.*
import com.kiri.feature.home.model.*
import java.time.LocalDate

private class PreviewHomeRepository(private val browsing: Boolean = false) : HomeRepository {
    private val q = DailyQuestion("preview-question", LocalDate.of(2026, 10, 13), "내가 가장 좋아하는 음식은?")
    val home =
        FamilyHome(
            LocalDate.of(2026, 10, 13),
            "me",
            if (browsing) null else "끼리네 가족",
            if (browsing) emptyList()
            else
                listOf(
                    FamilyMember("me", "나", "자녀"),
                    FamilyMember("mother", "마마", "엄마"),
                    FamilyMember("father", "빠빠", "아빠"),
                    FamilyMember("sibling", "돌돌이", "자녀"),
                ),
            if (browsing) null else q,
            if (browsing) emptyMap() else mapOf("mother" to "뚝배기불고기"),
            emptySet(),
            if (browsing) null
            else
                WeeklyMission(
                    "preview-mission",
                    "광장시장에서 시장음식 먹어보기",
                    "광장시장 · 종로구",
                    listOf("대중교통 편리", "혼잡도 높음"),
                    false,
                    place = "광장시장 · 종로구",
                ),
            recommendations =
                if (browsing) emptyList()
                else
                    listOf(
                        HomeRecommendation(
                            "preview-place",
                            "경복궁에서 사진찍기",
                            "경복궁 종로구",
                            listOf("대중교통 편리", "가족과 함께"),
                            "4.6",
                            description = "대중교통이 편리하고, 부모님이랑 가기 좋아요! 추천합니다",
                        )
                    ),
        )

    override suspend fun load() = home

    override suspend fun saveAnswer(question: DailyQuestion, answer: String) = home

    override suspend fun poke(question: DailyQuestion, memberId: String) = home
}

@Preview(
    name = "Home",
    showBackground = true,
    backgroundColor = 0xFF202020,
    device = "spec:width=411dp,height=891dp,dpi=420",
)
@Composable
private fun HomePreview() {
    HomeScreen(HomeUiState(loading = false, home = PreviewHomeRepository().home), {})
}

@Preview(
    name = "Home browsing",
    showBackground = true,
    backgroundColor = 0xFF202020,
    device = "spec:width=411dp,height=891dp,dpi=420",
)
@Composable
private fun HomeBrowsingPreview() {
    HomeScreen(HomeUiState(loading = false, home = PreviewHomeRepository(true).home), {})
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen(HomeUiState(), {})
}
