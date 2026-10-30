package com.kiri.data.place

import com.kiri.domain.mission.MissionRepository
import com.kiri.domain.place.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Catalogue view; bookmark persistence and account ownership are provided by BucketRepository. */
open class LocalPlaceRepository(
    private val userId: String = "demo-user",
    private val missions: List<PlaceMission>? = null,
    private val pageSize: Int = 12,
    private val missionRepository: MissionRepository =
        com.kiri.data.mission.UnavailableMissionRepository(),
    private val familyBuckets: com.kiri.domain.record.BucketRepository? = null,
) : PlaceRepository {
    init {
        require(pageSize > 0)
    }

    private val mutex = Mutex()
    private val bookmarks = mutableSetOf<Pair<String, String>>()
    private val completedMissions = mutableSetOf<String>()

    override suspend fun load(query: PlaceQuery): PlacePage = mutex.withLock {
        syncBookmarks()
        val keyword = query.keyword.trim()
        val catalog = missions ?: missionRepository.catalog()
        val filtered =
            catalog
                .filter {
                    query.filters.matches(it) &&
                        (query.category == null || it.category == query.category) &&
                        (keyword.isEmpty() ||
                            listOf(it.title, it.place, it.region).any { value ->
                                value.contains(keyword, true)
                            })
                }
                .let {
                    if (query.category == null) it.sortedByDescending { mission -> mission.pick }
                    else it
                }
        val start = query.cursor?.toIntOrNull() ?: 0
        require(start in 0..filtered.size)
        val end = minOf(start + pageSize, filtered.size)
        PlacePage(
            filtered.subList(start, end).map(::snapshot),
            filtered.size,
            end.takeIf { it < filtered.size }?.toString(),
        )
    }

    override suspend fun save(missionId: String) = mutex.withLock {
        require((missions ?: missionRepository.catalog()).any { it.id == missionId }) {
            "현재 저장할 수 없는 미션이에요."
        }
        familyBuckets?.save(missionId)
        bookmarks.add(userId to missionId)
        Unit
    }

    override suspend fun saved(): List<PlaceMission> = mutex.withLock {
        syncBookmarks()
        (missions ?: missionRepository.catalog())
            .filter { (userId to it.id) in bookmarks || it.id in completedMissions }
            .map(::snapshot)
    }

    private fun snapshot(mission: PlaceMission) =
        mission.copy(
            saved = (userId to mission.id) in bookmarks,
            previouslyCompleted = mission.id in completedMissions,
        )

    private suspend fun syncBookmarks() {
        val repository = familyBuckets ?: return
        repository.refresh()
        // This repository represents the current user's discovery view; family ownership lives in
        // BucketRepository.
        val owned = repository.mine()
        bookmarks.clear()
        bookmarks += owned.filterNot { it.completed }.map { userId to it.missionId }
        completedMissions.clear()
        completedMissions += owned.filter { it.completed }.map { it.missionId }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PlaceRepositoryModule {
    @Provides
    @Singleton
    fun providePlaceRepository(
        missions: MissionRepository,
        buckets: com.kiri.domain.record.BucketRepository,
    ): PlaceRepository = LocalPlaceRepository(missionRepository = missions, familyBuckets = buckets)
}
