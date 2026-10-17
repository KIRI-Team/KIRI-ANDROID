package com.kiri.data.place

import com.kiri.domain.place.*
import com.kiri.domain.mission.MissionRepository
import com.kiri.data.mission.FakeMissionRepository
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
    private val missions: List<PlaceMission>? = null,
    private val pageSize: Int = 12,
    private val missionRepository: MissionRepository = FakeMissionRepository(),
) : PlaceRepository {
    init { require(pageSize > 0) }
    private val mutex = Mutex()
    private val bookmarks = mutableSetOf<Pair<String, String>>()
    override suspend fun load(query: PlaceQuery): PlacePage = mutex.withLock {
        val keyword = query.keyword.trim()
        val catalog = missions ?: missionRepository.catalog()
        val filtered = catalog.filter { query.filters.matches(it) && (query.category == null || it.category == query.category) &&
            (keyword.isEmpty() || listOf(it.title, it.place, it.region).any { value -> value.contains(keyword, true) }) }
            .let { if (query.category == null) it.sortedByDescending { mission -> mission.pick } else it }
        val start = query.cursor?.toIntOrNull() ?: 0
        require(start in 0..filtered.size)
        val end = minOf(start + pageSize, filtered.size)
        PlacePage(filtered.subList(start, end).map(::snapshot), filtered.size, end.takeIf { it < filtered.size }?.toString())
    }
    override suspend fun save(missionId: String) = mutex.withLock {
        require((missions ?: missionRepository.catalog()).any { it.id == missionId }) { "Mission not found" }
        bookmarks.add(userId to missionId)
        Unit
    }
    override suspend fun saved(): List<PlaceMission> = mutex.withLock {
        (missions ?: missionRepository.catalog()).filter { (userId to it.id) in bookmarks }.map(::snapshot)
    }
    private fun snapshot(mission: PlaceMission) = mission.copy(saved = (userId to mission.id) in bookmarks)
    companion object {
        fun demoMissions(): List<PlaceMission> = FakeMissionRepository.demoCatalog()
    }
}
@Module
@InstallIn(SingletonComponent::class)
object PlaceRepositoryModule {
    @Provides @Singleton fun providePlaceRepository(missions: MissionRepository): PlaceRepository = FakePlaceRepository(missionRepository = missions)
}
