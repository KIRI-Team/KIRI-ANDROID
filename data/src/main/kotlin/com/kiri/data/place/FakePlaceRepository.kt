package com.kiri.data.place

import com.kiri.domain.place.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Explicit demo fixtures. Replace this binding with an API repository; saves live for this process only. */
class FakePlaceRepository(
    private val userId: String = "demo-user",
    private val missions: List<PlaceMission> = demoMissions(),
    private val pageSize: Int = 12,
) : PlaceRepository {
    init { require(pageSize > 0) }
    private val mutex = Mutex()
    private val bookmarks = mutableSetOf<Pair<String, String>>()
    override suspend fun load(query: PlaceQuery): PlacePage = mutex.withLock {
        val keyword = query.keyword.trim()
        val filtered = missions.filter { (query.category == null || it.category == query.category) &&
            (keyword.isEmpty() || listOf(it.title, it.place, it.region).any { value -> value.contains(keyword, true) }) }
            .let { if (query.category == null) it.sortedByDescending { mission -> mission.pick } else it }
        val start = query.cursor?.toIntOrNull() ?: 0
        require(start in 0..filtered.size)
        val end = minOf(start + pageSize, filtered.size)
        PlacePage(filtered.subList(start, end).map(::snapshot), filtered.size, end.takeIf { it < filtered.size }?.toString())
    }
    override suspend fun save(missionId: String) = mutex.withLock {
        require(missions.any { it.id == missionId }) { "Mission not found" }
        bookmarks.add(userId to missionId)
        Unit
    }
    override suspend fun saved(): List<PlaceMission> = mutex.withLock {
        missions.filter { (userId to it.id) in bookmarks }.map(::snapshot)
    }
    private fun snapshot(mission: PlaceMission) = mission.copy(saved = (userId to mission.id) in bookmarks)
    companion object {
        fun demoMissions(): List<PlaceMission> = listOf(
            PlaceMission("place-market", "광장시장에서 시장음식 먹어보기", "광장시장", "종로구", PlaceCategory.FOOD, tags = listOf("대중교통 편리"), pick = true),
            PlaceMission("place-river", "한강공원에서 산책하기", "뚝섬한강공원", "광진구", PlaceCategory.OUTDOOR, "4.6", listOf("좌석 충분", "계단 없음")),
            PlaceMission("place-culture", "미술관에서 전시 관람하기", "미술관", "종로구", PlaceCategory.CULTURE),
            PlaceMission("place-popup", "팝업 행사 방문하기", "팝업 공간", "성동구", PlaceCategory.POPUP),
            PlaceMission("place-show", "가족과 공연 관람하기", "공연장", "중구", PlaceCategory.SHOW),
            PlaceMission("place-experience", "가족과 공예 체험하기", "공방", "마포구", PlaceCategory.EXPERIENCE),
        )
    }
}
@Module
@InstallIn(SingletonComponent::class)
object PlaceRepositoryModule {
    @Provides @Singleton fun providePlaceRepository(): PlaceRepository = FakePlaceRepository()
}
