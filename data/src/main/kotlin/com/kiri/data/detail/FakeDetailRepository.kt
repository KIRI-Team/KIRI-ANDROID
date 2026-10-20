package com.kiri.data.detail

import com.kiri.domain.detail.*
import com.kiri.domain.mission.MissionRepository
import com.kiri.data.mission.FakeMissionRepository
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
class FakeDetailRepository(private val homeRepository: HomeRepository, private val missionRepository: MissionRepository = FakeMissionRepository(), private val schedules: com.kiri.domain.schedule.ScheduleRepository? = null) : DetailRepository {
    private val mutex = Mutex()
    private val selections = mutableMapOf<String, Set<Condition>>()
    private fun selectionKey(id: String) = if (id == "schedule-demo") "weekly-demo" else id

    override suspend fun load(missionId: String): MissionDetail = mutex.withLock { snapshot(missionId) }

    override suspend fun preview(missionId: String): MissionPreview {
        val content = missionRepository.load(missionId)
        val home = familySnapshot()
        val schedule = home.schedules.firstOrNull { it.missionId == missionId && it.status != ScheduleStatus.CANCELLED }
        val dateTime = schedule?.startsAt?.atZone(ZoneId.of("Asia/Seoul"))
        return MissionPreview(
            MissionDetail(
                id = content.summary.id, title = content.summary.title, familyName = home.familyName.orEmpty(),
                date = dateTime?.toLocalDate(), startTime = dateTime?.format(DateTimeFormatter.ofPattern("a h시 m분", Locale.KOREAN)),
                place = content.summary.place, expectedSteps = content.expectedSteps, durationMinutes = content.durationMinutes,
                participants = if (schedule == null) emptyList() else home.members.filter { it.accessible }.take(3).map { DetailParticipant(it.nickname, true) },
                completed = schedule?.status == ScheduleStatus.COMPLETED,
                activities = content.activities, essentials = content.essentials, nearby = content.nearby,
            ), content.reviews, content.summary.pick,
        )
    }

    override suspend fun applyConditions(missionId: String, conditions: Set<Condition>): MissionDetail = mutex.withLock {
        val detail = snapshot(missionId)
        check(!detail.completed) { "Completed missions cannot be customized" }
        selections[selectionKey(missionId)] = conditions.toSet()
        detail.copy(conditions = conditions.toSet())
    }

    private suspend fun snapshot(id: String): MissionDetail {
        val home = familySnapshot()
        check(!home.browsing) { "Family access required" }
        val mission = home.mission?.takeIf { it.id == id && !it.unavailable }
        val schedule = home.schedules.firstOrNull { it.id == id && it.status != ScheduleStatus.CANCELLED }
        require(mission != null || schedule != null) { "Mission not found" }
        val isDemo = id == "weekly-demo" || id == "schedule-demo"
        val dateTime = (schedule ?: home.schedules.firstOrNull { isDemo && it.id == "schedule-demo" })?.startsAt?.atZone(ZoneId.of("Asia/Seoul"))
        val canonicalId = mission?.missionId ?: requireNotNull(schedule).missionId
        val content = missionRepository.load(canonicalId)
        return MissionDetail(
            id = content.summary.id, title = content.summary.title,
            familyName = home.familyName.orEmpty(), date = dateTime?.toLocalDate(),
            startTime = dateTime?.format(DateTimeFormatter.ofPattern("a h시 m분", Locale.KOREAN)),
            place = content.summary.place, expectedSteps = content.expectedSteps, durationMinutes = content.durationMinutes,
            participants = schedules?.schedules?.value?.find { it.id == schedule?.id || it.weeklyMissionId == id }?.let { activity ->
                activity.responses.map { (memberId, response) -> DetailParticipant(home.members.find { it.id == memberId && it.accessible }?.nickname ?: "탈퇴한 멤버", response == com.kiri.domain.schedule.MemberResponse.ACCEPTED) }
            } ?: home.members.filter { it.accessible }.take(3).map { DetailParticipant(it.nickname, true) },
            completed = mission?.completed == true || schedule?.status == ScheduleStatus.COMPLETED || (isDemo && home.mission?.completed == true),
            activities = content.activities, essentials = content.essentials, nearby = content.nearby,
            conditions = selections[selectionKey(id)].orEmpty(),
        )
    }

    private suspend fun familySnapshot(): com.kiri.domain.home.FamilyHome {
        val home = homeRepository.load()
        val repository = schedules ?: return home
        repository.refresh()
        return home.copy(schedules = repository.schedules.value.map { activity ->
            com.kiri.domain.home.HomeSchedule(activity.id, activity.title, activity.startsAt, activity.status(java.time.Instant.now()), home.currentUserId in activity.acceptedIds, activity.responseDeadline, activity.missionId)
        }, mission = home.mission?.let { weekly -> weekly.copy(completed = weekly.completed || repository.schedules.value.any { it.weeklyMissionId == weekly.id && it.completed }) })
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DetailRepositoryModule {
    @Provides @Singleton
    fun provideDetailRepository(home: HomeRepository, missions: MissionRepository, schedules: com.kiri.domain.schedule.ScheduleRepository): DetailRepository = FakeDetailRepository(home, missions, schedules)
}
