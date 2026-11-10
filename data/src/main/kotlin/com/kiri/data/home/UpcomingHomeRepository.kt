package com.kiri.data.home

import com.kiri.data.network.apiResult
import com.kiri.domain.home.*
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 질문 저장 결과에도 최신 서버 일정 카드를 유지한다. */
internal class UpcomingHomeRepository(private val questions: HomeRepository, private val api: HomeApi,
    private val json: Json) : HomeRepository {
    private val mutex = Mutex()
    override suspend fun load() = mutex.withLock { enrich(questions.load()) }
    // 가족 정보만 필요하면 일정 카드를 부르지 않는다
    override suspend fun family() = questions.family()
    override suspend fun invalidateFamily() = questions.invalidateFamily()
    override suspend fun saveAnswer(question: DailyQuestion, answer: String) = mutex.withLock {
        questions.saveAnswer(question, answer).copy(schedules = schedules)
    }
    override suspend fun poke(question: DailyQuestion, memberId: String) = mutex.withLock {
        questions.poke(question, memberId).copy(schedules = schedules)
    }

    private var schedules = emptyList<HomeSchedule>()
    private suspend fun enrich(home: FamilyHome): FamilyHome {
        schedules = emptyList()
        if (home.browsing) return home
        val result = apiResult(json) { api.upcoming() }.getOrElse { return home }
        schedules = listOfNotNull(result.responseRequired, result.upcoming).distinctBy { it.scheduleId }.map {
            HomeSchedule(it.scheduleId.toString(), it.title,
                LocalDateTime.parse("${it.date}T${it.time}").atZone(ZoneId.of("Asia/Seoul")).toInstant(),
                if (it.type == "RESPONSE_REQUIRED" && it.myStatus == "RECONFIRM_REQUIRED") ScheduleStatus.RECONFIRM_REQUIRED
                else ScheduleStatus.valueOf(it.status),
                finalParticipant = it.type != "RESPONSE_REQUIRED" && it.myStatus == "ACCEPTED",
                missionId = "", // 이 API는 missionId를 제공하지 않는다. scheduleId를 미션 ID로 쓰지 않는다.
            )
        }
        return home.copy(schedules = schedules)
    }
}
