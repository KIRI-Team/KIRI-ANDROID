package com.kiri.data.home

import com.kiri.domain.home.*
import com.kiri.domain.mission.MissionRepository
import com.kiri.data.mission.FakeMissionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class HomeScenario { DEFAULT, BROWSING, UNASSIGNED, NO_MISSION, MISSION_UNAVAILABLE, COMPLETED_MISSION, MANY_MEMBERS, LOAD_FAILURE, SAVE_FAILURE, POKE_FAILURE }

/** Session-only fake. Failure scenarios fail once, then allow a normal retry. */
class FakeHomeRepository(
    private val clock: Clock = Clock.systemUTC(),
    private val scenario: HomeScenario = HomeScenario.DEFAULT,
    private val latencyMillis: Long = 350,
    private val missionRepository: MissionRepository = FakeMissionRepository(),
) : HomeRepository {
    private val mutex = Mutex()
    private var snapshot = seed(today())
    private var loadFailed = false
    private var saveFailed = false
    private var pokeFailed = false
    private fun today(): LocalDate = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))

    override suspend fun load(): FamilyHome = mutex.withLock {
        delay(latencyMillis)
        refreshDate()
        if (scenario == HomeScenario.LOAD_FAILURE && !loadFailed) {
            loadFailed = true
            throw HomeException(HomeFailure.NETWORK)
        }
        val content = missionRepository.load(FakeMissionRepository.RIVER_MISSION_ID).summary
        snapshot = snapshot.copy(
            mission = snapshot.mission?.copy(title = content.title, description = content.place, place = content.place, tags = content.tags),
            schedules = snapshot.schedules.map { it.copy(title = content.title) },
        )
        snapshot
    }

    override suspend fun saveAnswer(question: DailyQuestion, answer: String): FamilyHome = mutex.withLock {
        delay(latencyMillis)
        validate(question)
        if (answer.isBlank()) throw HomeException(HomeFailure.EMPTY_ANSWER)
        if (!snapshot.canRead(snapshot.currentUserId)) throw HomeException(HomeFailure.UNAVAILABLE)
        if (scenario == HomeScenario.SAVE_FAILURE && !saveFailed) {
            saveFailed = true
            throw HomeException(HomeFailure.NETWORK)
        }
        snapshot = snapshot.copy(answers = snapshot.answers + (snapshot.currentUserId to answer))
        snapshot
    }

    override suspend fun poke(question: DailyQuestion, memberId: String): FamilyHome = mutex.withLock {
        delay(latencyMillis)
        validate(question)
        if (!snapshot.canRead(memberId) || memberId == snapshot.currentUserId) throw HomeException(HomeFailure.UNAVAILABLE)
        if (memberId in snapshot.answers) throw HomeException(HomeFailure.ALREADY_ANSWERED)
        if (memberId in snapshot.pokedMemberIds) throw HomeException(HomeFailure.ALREADY_POKED)
        if (scenario == HomeScenario.POKE_FAILURE && !pokeFailed) {
            pokeFailed = true
            throw HomeException(HomeFailure.NETWORK)
        }
        snapshot = snapshot.copy(pokedMemberIds = snapshot.pokedMemberIds + memberId)
        snapshot
    }

    private fun validate(question: DailyQuestion) {
        refreshDate()
        if (question.date != today()) throw HomeException(HomeFailure.EXPIRED)
        if (snapshot.browsing || snapshot.question != question) throw HomeException(HomeFailure.UNAVAILABLE)
    }

    override suspend fun confirmSchedule(scheduleId: String, expectedStartsAt: java.time.Instant): FamilyHome = mutex.withLock {
        delay(latencyMillis)
        refreshDate()
        val schedule = snapshot.schedules.firstOrNull { it.id == scheduleId } ?: throw HomeException(HomeFailure.UNAVAILABLE)
        if (snapshot.browsing || snapshot.members.none { it.id == snapshot.currentUserId && it.accessible } ||
            schedule.startsAt != expectedStartsAt || schedule.status in setOf(ScheduleStatus.CANCELLED, ScheduleStatus.COMPLETED, ScheduleStatus.NOT_CONFIRMED)) {
            throw HomeException(HomeFailure.UNAVAILABLE)
        }
        if (!clock.instant().isBefore(schedule.startsAt) || schedule.responseDeadline?.let { !clock.instant().isBefore(it) } == true) {
            throw HomeException(HomeFailure.EXPIRED)
        }
        snapshot = snapshot.copy(schedules = snapshot.schedules.map {
            if (it.id == scheduleId) it.copy(status = ScheduleStatus.CONFIRMED, finalParticipant = true,
                acceptedMemberIds = it.acceptedMemberIds + snapshot.currentUserId) else it
        })
        snapshot
    }

    private fun refreshDate() {
        if (snapshot.date != today()) snapshot = seed(today())
    }

    private fun seed(date: LocalDate): FamilyHome {
        val browsing = scenario == HomeScenario.BROWSING
        val members = if (browsing) emptyList() else buildList {
            add(FamilyMember("me", "끼리", "아들"))
            add(FamilyMember("mother", "마마", "엄마"))
            add(FamilyMember("father", "빠빠", "아빠"))
            add(FamilyMember("sibling", "돌돌이", "아들"))
            add(FamilyMember("daughter", "달달이", "딸"))
            if (scenario == HomeScenario.MANY_MEMBERS) {
                repeat(20) { add(FamilyMember("member-$it", "아주 긴 닉네임을 사용하는 가족 $it", "기타")) }
                add(FamilyMember("left", "탈퇴한 구성원", "기타", accessible = false))
            }
        }
        val question = if (browsing || scenario == HomeScenario.UNASSIGNED) null
            else DailyQuestion("question-$date", date, "내가 가장 좋아하는 음식은?")
        return FamilyHome(
            date, "me", if (browsing) null else "끼리네 가족", members, question,
            if (question == null) emptyMap() else mapOf("mother" to "뚝배기불고기", "sibling" to "가족과 함께 먹는 음식이 제일 좋아요."),
            emptySet(),
            if (browsing || scenario == HomeScenario.NO_MISSION) null else WeeklyMission(
                "weekly-demo", "한강 뚜벅뚜벅 축제 가보기", "여의도 한강 공원",
                listOf("대중교통 편리", "혼잡도 높음"), scenario == HomeScenario.COMPLETED_MISSION,
                place = "여의도 한강 공원",
                unavailable = scenario == HomeScenario.MISSION_UNAVAILABLE,
                missionId = FakeMissionRepository.RIVER_MISSION_ID,
            ),
            schedules = if (browsing) emptyList() else listOf(
                HomeSchedule("schedule-demo", "한강 뚜벅뚜벅 축제 가보기", date.atTime(14, 50).atZone(ZoneId.of("Asia/Seoul")).toInstant(), ScheduleStatus.CONFIRMED, true, missionId = FakeMissionRepository.RIVER_MISSION_ID),
                HomeSchedule("schedule-invite-demo", "한강 뚜벅뚜벅 축제 가보기", date.plusDays(3).atTime(14, 50).atZone(ZoneId.of("Asia/Seoul")).toInstant(), ScheduleStatus.PENDING_RESPONSE, false,
                    missionId = FakeMissionRepository.RIVER_MISSION_ID, acceptedMemberIds = setOf("mother", "sibling", "daughter"),
                    invitationMessage = "엄마, 아빠 나 여기 가고싶은데 우리 같이 갈래요??"),
                HomeSchedule("schedule-changed-demo", "한강 뚜벅뚜벅 축제 가보기", date.plusDays(5).atTime(14, 50).atZone(ZoneId.of("Asia/Seoul")).toInstant(), ScheduleStatus.RECONFIRM_REQUIRED, false,
                    missionId = FakeMissionRepository.RIVER_MISSION_ID, acceptedMemberIds = setOf("daughter"),
                    invitationMessage = "엄마, 아빠 나 여기 가고싶은데 우리 같이 갈래요??"),
            ),
            recommendations = if (browsing) emptyList() else listOf(
                HomeRecommendation("gyeongbokgung", "경복궁에서 사진찍기", "경복궁 종로구", listOf("대중교통 편리", "가족과 함께"), "4.6", description = "대중교통이 편리하고, 부모님이랑 가기 좋아요! 추천합니다"),
                HomeRecommendation("namsan", "남산 산책하기", "남산 중구", listOf("대중교통 편리", "가족과 함께"), "4.4", description = "산책하기 좋고, 가족과 함께 여유를 즐길 수 있어요"),
            ),
            inviteCode = if (browsing) null else "KIRI-${date.toString().replace("-", "").takeLast(6)}",
        )
    }
}
