package com.kiri.feature.detail.presentation

import com.kiri.feature.detail.model.*

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kiri.domain.detail.*
import com.kiri.uikit.KiriTheme
import java.time.LocalDate

/** Literal design fixture for previews and visual tests only. */
fun figmaDetailSample(completed: Boolean = false) = MissionDetail(
    "figma-preview", "한강 뚜벅뚜벅 축제 가보기", "끼리네 가족", LocalDate.of(2026, 12, 3), "오후 2시 50분", "여의도 한강 공원", 20000, 480,
    listOf(DetailParticipant("김주연", true), DetailParticipant("엄마", true), DetailParticipant("아빠", true)), completed,
    listOf(DetailSuggestion("잠수교 위에서 한강 보며 걷기", "차 없는 잠수교를 천천히 걸으며 한강 풍경과 축제 분위기를 즐길 수 있어요!"),
        DetailSuggestion("야외 취식존에서 한강 보며 식사하기", "한강을 바라보며 푸드트럭에서 산 음식을 여유롭게 먹을 수 있어요!")),
    List(2) { DetailSuggestion("양산", "오늘 자외선 농도가 많이 높아요! 피부 조심하세요~") },
    List(2) { NearbyPlace("preview-$it", "세빛섬", "서울시 반포구", "잠수교 바로 옆에 있어 축제를 둘러본 뒤 한강 위 세빛섬과 야경을 함께 구경하기 좋아요") },
)

@Preview(name = "미션 상세", widthDp = 390, heightDp = 844, showBackground = true)
@Composable private fun DetailPreview() { KiriTheme { MissionDetailScreen(DetailUiState(loading = false, detail = figmaDetailSample()), {}) } }
@Preview(name = "컨디션 커스텀", widthDp = 390, heightDp = 784, showBackground = true)
@Composable private fun CustomPreview() { KiriTheme { ConditionCustomScreen(DetailUiState(loading = false, detail = figmaDetailSample().copy(expectedSteps = 8000, title = "한강 뚜벅뚜벅 축제 방문")), {}) } }
@Preview(name = "미션 완료", widthDp = 390, heightDp = 844, showBackground = true)
@Composable private fun CompletedPreview() { KiriTheme { MissionDetailScreen(DetailUiState(loading = false, detail = figmaDetailSample(true)), {}) } }
