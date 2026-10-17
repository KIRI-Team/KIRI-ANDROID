package com.kiri.data.mission

import com.kiri.domain.detail.*
import com.kiri.domain.mission.*
import com.kiri.domain.place.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Explicit design/demo fixtures, not live places, ratings, weather, or user reviews. */
class FakeMissionRepository : MissionRepository {
    private val missions = demoCatalog()
    override suspend fun catalog(): List<PlaceMission> = missions
    override suspend fun load(missionId: String): MissionContent {
        val summary = requireNotNull(missions.find { it.id == missionId }) { "Mission not found" }
        if (missionId != RIVER_MISSION_ID) return MissionContent(summary)
        return MissionContent(
            summary, expectedSteps = 20000, durationMinutes = 480,
            activities = listOf(
                DetailSuggestion("잠수교 위에서 한강 보며 걷기", "차 없는 잠수교를 천천히 걸으며 한강 풍경과 축제 분위기를 즐길 수 있어요!"),
                DetailSuggestion("야외 취식존에서 한강 보며 식사하기", "한강을 바라보며 푸드트럭에서 산 음식을 여유롭게 먹을 수 있어요!"),
            ),
            essentials = List(2) { DetailSuggestion("양산", "오늘 자외선 농도가 많이 높아요! 피부 조심하세요~") },
            nearby = listOf(
                NearbyPlace("demo-sebit-1", "세빛섬", "서울시 반포구", "잠수교 바로 옆에 있어 산책을 마친 뒤 한강 위 세빛섬과 야경을 함께 구경하기 좋아요", 600, false, 0),
                NearbyPlace("demo-sebit-2", "세빛섬", "서울시 반포구", "잠수교 바로 옆에 있어 산책을 마친 뒤 한강 위 세빛섬과 야경을 함께 구경하기 좋아요", 200, true, 5000),
            ),
            reviews = List(3) { index -> MissionReview("river-review-$index", "USER)@#@#@", "가족과 함께 한강을 걸었다. 풍경을 보며 이야기를 나누니 서로를 조금 더 알게 된 기분이었다.", 10) },
        )
    }

    companion object {
        const val RIVER_MISSION_ID = "place-river"
        fun demoCatalog(): List<PlaceMission> = listOf(
            PlaceMission("place-market", "광장시장에서 시장음식 먹어보기", "광장시장", "종로구", PlaceCategory.FOOD, "4.6", listOf("대중교통 편리", "혼잡도 높음"), pick = true),
            PlaceMission(RIVER_MISSION_ID, "한강 뚜벅뚜벅 축제 가보기", "여의도 한강 공원", "영등포구", PlaceCategory.OUTDOOR, "4.6", listOf("좌석 충분", "계단 없음")),
            PlaceMission("place-culture", "미술관에서 전시 관람하기", "미술관", "종로구", PlaceCategory.CULTURE, "4.6", listOf("실내 활동", "대중교통 편리")),
            PlaceMission("place-popup", "팝업 행사 방문하기", "팝업 공간", "성동구", PlaceCategory.POPUP, "4.6", listOf("대중교통 편리", "혼잡도 높음")),
            PlaceMission("place-show", "가족과 공연 관람하기", "공연장", "중구", PlaceCategory.SHOW, "4.6", listOf("실내 활동", "좌석 충분")),
            PlaceMission("place-experience", "가족과 공예 체험하기", "공방", "마포구", PlaceCategory.EXPERIENCE, "4.6", listOf("실내 활동", "가족과 함께")),
        ).mapIndexed { index, mission ->
            // Search examples only: these are fixture distances, prices and availability, not live booking facts.
            mission.copy(searchMetadata = PlaceSearchMetadata("서울", (index + 1) * .5,
                index * 5000, 6, java.time.LocalDate.of(2026, 1, 1).toEpochDay(),
                java.time.LocalDate.of(2027, 12, 31).toEpochDay(), 540, 1260))
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object MissionRepositoryModule {
    @Provides @Singleton fun provideMissionRepository(): MissionRepository = FakeMissionRepository()
}
