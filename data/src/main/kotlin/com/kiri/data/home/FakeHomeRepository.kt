package com.kiri.data.home

import com.kiri.domain.home.*
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class HomeScenario { DEFAULT, BROWSING, UNASSIGNED, NO_MISSION, COMPLETED_MISSION, MANY_MEMBERS, LOAD_FAILURE, SAVE_FAILURE, POKE_FAILURE }

/** Session-only fake. Failure scenarios fail once, then allow a normal retry. */
class FakeHomeRepository(
    private val clock: Clock = Clock.systemUTC(),
    private val scenario: HomeScenario = HomeScenario.DEFAULT,
    private val latencyMillis: Long = 350,
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

    private fun refreshDate() {
        if (snapshot.date != today()) snapshot = seed(today())
    }

    private fun seed(date: LocalDate): FamilyHome {
        val browsing = scenario == HomeScenario.BROWSING
        val members = if (browsing) emptyList() else buildList {
            add(FamilyMember("me", "끼리", "자녀"))
            add(FamilyMember("mother", "마마", "엄마"))
            add(FamilyMember("father", "빠빠", "아빠"))
            add(FamilyMember("sibling", "돌돌이", "자녀"))
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
                "weekly-demo", "광장시장에서 시장음식 먹어보기", "광장시장 · 종로구",
                listOf("대중교통 편리", "혼잡도 높음"), scenario == HomeScenario.COMPLETED_MISSION,
                place = "광장시장 · 종로구",
            ),
            schedules = if (browsing) emptyList() else listOf(
                HomeSchedule("schedule-demo", "가족과 함께 경복궁 가기", date.atTime(18, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(), ScheduleStatus.CONFIRMED, true),
            ),
            recommendations = if (browsing) emptyList() else listOf(
                HomeRecommendation("gyeongbokgung", "경복궁에서 사진찍기", "경복궁 · 종로구", listOf("대중교통 편리", "가족과 함께"), "4.6"),
                HomeRecommendation("namsan", "남산 산책하기", "남산 · 중구", listOf("대중교통 편리"), "4.4"),
            ),
        )
    }
}
