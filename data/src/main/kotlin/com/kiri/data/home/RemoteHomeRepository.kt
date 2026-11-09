package com.kiri.data.home

import com.kiri.data.auth.TokenStore
import com.kiri.data.mission.MissionApi
import com.kiri.data.network.*
import com.kiri.domain.home.*
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/** Only server-backed question data is exposed; missing home APIs do not produce demo content. */
internal class RemoteHomeRepository(
    private val api: HomeApi,
    private val tokens: TokenStore,
    private val json: Json,
    private val clock: Clock,
    private val missions: MissionApi? = null,
) : HomeRepository {
    private val mutex = Mutex()
    private var snapshot: FamilyHome? = null
    private var sessionToken: String? = null
    private fun today() = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))
    // 토큰은 AuthInterceptor 가 붙인다. 여기서는 로그인하지 않았으면 요청하지 않고, 다른 계정으로 바뀌었으면 이전 화면 상태를 버린다
    private suspend fun ensureSession() {
        val token = tokens.get()?.accessToken ?: throw HomeException(HomeFailure.UNAVAILABLE)
        if (sessionToken != token) { snapshot = null; sessionToken = token }
    }

    override suspend fun load(): FamilyHome = mutex.withLock { mapped { ensureSession(); loadHome() } }

    private suspend fun loadHome(): FamilyHome {
        val date = today()
        val daily = apiResult(json) { api.today() }.getOrElse { error ->
            // A family may exist before its first daily question is assigned.
            if ((error as? KiriApiException)?.code == "QUESTION404_1") TodayQuestionResponse(hasFamily = true)
            else throw error
        }
        val family = if (daily.hasFamily && daily.questionId != null) {
            apiResult(json) { api.answers(daily.questionId.toString()) }.getOrThrow()
        } else null
        val me = family?.answers?.singleOrNull { it.isMe }?.memberId?.toString().orEmpty()
        val question = daily.questionId?.let { id -> daily.question?.let { DailyQuestion(id.toString(), date, it) } }
        val answers = family?.answers.orEmpty().filter { it.isAnswered && it.answer != null }
            .associate { it.memberId.toString() to requireNotNull(it.answer) }.toMutableMap()
        if (daily.isAnswered && daily.answer != null && me.isNotEmpty()) answers[me] = daily.answer
        val poked = snapshot?.takeIf { it.date == date && it.question?.id == question?.id && it.currentUserId == me }?.pokedMemberIds.orEmpty()
        val weekly = if (daily.hasFamily) missions?.let { missionApi ->
            apiResult(json) { missionApi.getWeeklyRecommendation() }.getOrNull()?.let {
                WeeklyMission(
                    id = it.missionId.toString(), title = it.title,
                    description = it.description ?: it.recommendationReason.orEmpty(),
                    tags = listOfNotNull(it.recommendationReason),
                    completed = false, imageUrl = it.imageUrl, recommendationOnly = false,
                )
            }
        } else null
        return FamilyHome(date, me, if (daily.hasFamily) "우리 가족" else null,
            family?.answers.orEmpty().map { FamilyMember(it.memberId.toString(), it.nickname, "") },
            question, answers, poked, mission = weekly, serverBacked = true).also { snapshot = it }
    }

    override suspend fun saveAnswer(question: DailyQuestion, answer: String): FamilyHome = mutex.withLock { mapped {
        ensureSession()
        val home = validate(question)
        if (answer.isBlank()) throw HomeException(HomeFailure.EMPTY_ANSWER)
        if (answer.length > 500) throw HomeException(HomeFailure.ANSWER_TOO_LONG)
        if (!home.canRead(home.currentUserId)) throw HomeException(HomeFailure.UNAVAILABLE)
        // Mutation responses intentionally have result:null, unlike query responses.
        apiCommand(json) { api.save(question.id, AnswerRequest(answer)) }.getOrThrow()
        home.copy(answers = home.answers + (home.currentUserId to answer)).also { snapshot = it }
    } }

    override suspend fun poke(question: DailyQuestion, memberId: String): FamilyHome = mutex.withLock { mapped {
        ensureSession()
        val home = validate(question)
        if (memberId in home.pokedMemberIds) throw HomeException(HomeFailure.ALREADY_POKED)
        if (!home.canPoke(memberId)) throw HomeException(HomeFailure.UNAVAILABLE)
        apiCommand(json) { api.poke(memberId) }.getOrThrow()
        home.copy(pokedMemberIds = home.pokedMemberIds + memberId).also { snapshot = it }
    } }

    private fun validate(question: DailyQuestion): FamilyHome {
        if (question.date != today()) throw HomeException(HomeFailure.EXPIRED)
        return snapshot?.takeIf { it.question == question && !it.browsing }
            ?: throw HomeException(HomeFailure.UNAVAILABLE)
    }

    private suspend fun <T> mapped(block: suspend () -> T): T = try { block() }
    catch (error: CancellationException) { throw error }
    catch (error: KiriApiException) {
        throw HomeException(when (error.code) {
            "QUESTION400_1" -> HomeFailure.EXPIRED
            "QUESTION409_2" -> HomeFailure.ALREADY_POKED
            "QUESTION404_1", "QUESTION404_2", "QUESTION404_3", "QUESTION403_1", "QUESTION400_2" -> HomeFailure.UNAVAILABLE
            else -> HomeFailure.NETWORK
        })
    }
}

