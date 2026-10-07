package com.kiri.data.detail

import com.kiri.domain.detail.*
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.ScheduleStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Session-only adapter, matching the existing fake home data source.
 * Missing schedule/forecast data stays unknown; no fabricated weather or participants.
 * Replace with an API implementation when the detail endpoint becomes available.
 */
class FakeDetailRepository(private val homeRepository: HomeRepository) : DetailRepository {
    private val mutex = Mutex()
    private val selections = mutableMapOf<String, Set<Condition>>()
    private fun selectionKey(id: String) = if (id == "schedule-demo") "weekly-demo" else id

    override suspend fun load(missionId: String): MissionDetail = mutex.withLock { snapshot(missionId) }

    override suspend fun applyConditions(missionId: String, conditions: Set<Condition>): MissionDetail = mutex.withLock {
        val detail = snapshot(missionId)
        check(!detail.completed) { "Completed missions cannot be customized" }
        selections[selectionKey(missionId)] = conditions.toSet()
        detail.copy(conditions = conditions.toSet())
    }

    private suspend fun snapshot(id: String): MissionDetail {
        val home = homeRepository.load()
        check(!home.browsing) { "Family access required" }
        val mission = home.mission?.takeIf { it.id == id && !it.unavailable }
        val schedule = home.schedules.firstOrNull { it.id == id && it.status != ScheduleStatus.CANCELLED }
        require(mission != null || schedule != null) { "Mission not found" }
        val isDemo = id == "weekly-demo" || id == "schedule-demo"
        val dateTime = (schedule ?: home.schedules.firstOrNull { isDemo && it.id == "schedule-demo" })?.startsAt?.atZone(ZoneId.of("Asia/Seoul"))
        val detail = MissionDetail(
            id = id, title = mission?.title ?: requireNotNull(schedule).title,
            familyName = home.familyName.orEmpty(), date = dateTime?.toLocalDate(),
            startTime = dateTime?.format(DateTimeFormatter.ofPattern("a h시 m분", Locale.KOREAN)),
            place = mission?.place, expectedSteps = null, durationMinutes = null,
            participants = emptyList(),
            completed = mission?.completed == true || schedule?.status == ScheduleStatus.COMPLETED || (isDemo && home.mission?.completed == true),
            activities = emptyList(), essentials = emptyList(), nearby = emptyList(),
            conditions = selections[selectionKey(id)].orEmpty(),
        )
        // Explicit Figma fixture, NOT live event, forecast, distance, cost or crowd data.
        // Unknown IDs above never fall back to this fixture.
        return if (!isDemo) detail else detail.copy(
            place = "여의도 한강 공원", expectedSteps = 20000, durationMinutes = 480,
            participants = home.members.filter { it.accessible }.take(3).map { DetailParticipant(it.nickname, true) },
            activities = listOf(
                DetailSuggestion("잠수교 위에서 한강 보며 걷기", "차 없는 잠수교를 천천히 걸으며 한강 풍경과 축제 분위기를 즐길 수 있어요!"),
                DetailSuggestion("야외 취식존에서 한강 보며 식사하기", "한강을 바라보며 푸드트럭에서 산 음식을 여유롭게 먹을 수 있어요!"),
            ),
            essentials = List(2) { DetailSuggestion("양산", "오늘 자외선 농도가 많이 높아요! 피부 조심하세요~") },
            nearby = listOf(
                NearbyPlace("demo-sebit-1", "세빛섬", "서울시 반포구", "잠수교 바로 옆에 있어 축제를 둘러본 뒤 한강 위 세빛섬과 야경을 함께 구경하기 좋아요", 600, false, 0),
                NearbyPlace("demo-sebit-2", "세빛섬", "서울시 반포구", "잠수교 바로 옆에 있어 축제를 둘러본 뒤 한강 위 세빛섬과 야경을 함께 구경하기 좋아요", 200, true, 5000),
            ),
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DetailRepositoryModule {
    @Provides @Singleton
    fun provideDetailRepository(home: HomeRepository): DetailRepository = FakeDetailRepository(home)
}
