package com.kiri.data.home

import com.kiri.data.auth.TokenStore
import com.kiri.data.auth.memberIdOf
import com.kiri.data.family.FamilyApi
import com.kiri.data.family.FamilyIdStore
import com.kiri.data.family.FamilyInfoResponse
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
    // 가족 정보 API. 없으면(테스트 등) family() 가 load() 를 그대로 쓴다
    private val families: FamilyApi? = null,
    private val familyIds: FamilyIdStore? = null,
) : HomeRepository {
    private val mutex = Mutex()
    private var snapshot: FamilyHome? = null
    private var sessionToken: String? = null

    // 가족 정보 캐시. 여러 화면이 잇달아 가족 정보를 찾아도 한 번만 부르도록 잠깐 재사용한다
    // (가족 없음이면 info 가 null). 계정이 바뀌면 지운다
    private class CachedFamily(val info: FamilyInfoResponse?, val at: java.time.Instant)
    private var familyCache: CachedFamily? = null
    private fun today() = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))
    // 토큰은 AuthInterceptor 가 붙인다. 여기서는 로그인하지 않았으면 요청하지 않고, 다른 계정으로 바뀌었으면 이전 화면 상태를 버린다
    private suspend fun ensureSession() {
        val token = tokens.get()?.accessToken ?: throw HomeException(HomeFailure.UNAVAILABLE)
        if (sessionToken != token) { snapshot = null; familyCache = null; sessionToken = token }
    }

    override suspend fun load(): FamilyHome = mutex.withLock { mapped { ensureSession(); loadHome() } }

    override suspend fun family(): FamilyHome {
        if (families == null || familyIds == null) return load()
        return mutex.withLock { mapped {
            ensureSession()
            val info = familyInfo(reuseWithinSeconds = FAMILY_CACHE_SECONDS)
            val me = sessionToken?.let(::memberIdOf).orEmpty()
            if (info == null) FamilyHome(today(), me, null, emptyList(), null, emptyMap(), emptySet(), null, serverBacked = true)
            else FamilyHome(today(), me, info.familyName, info.members(), null, emptyMap(), emptySet(), null,
                serverBacked = true, familyId = info.familyId.toString())
        } }
    }

    // 가족 정보를 불러온다. reuseWithinSeconds 안에 불러온 값이 있으면 다시 부르지 않는다(null 이면 항상 새로 부른다).
    // 소속된 가족이 없으면 null
    private suspend fun familyInfo(reuseWithinSeconds: Long?): FamilyInfoResponse? {
        val api = requireNotNull(families)
        val ids = requireNotNull(familyIds)
        val now = clock.instant()
        if (reuseWithinSeconds != null) {
            familyCache?.takeIf { !it.at.plusSeconds(reuseWithinSeconds).isBefore(now) }?.let { return it.info }
        }
        val info = run {
            val savedId = ids.get()
            if (savedId != null) {
                val result = apiResult(json) { api.getFamily(savedId) }
                result.getOrNull()?.let { return@run it }
                // 기억한 가족 ID 가 더는 내 가족이 아니면(다른 가족에 가입 등) 내 가족 ID 를 다시 받아 한 번 더 부른다
                val code = (result.exceptionOrNull() as? KiriApiException)?.code
                if (code != "FAMILY403_1" && code != "FAMILY404_1") throw requireNotNull(result.exceptionOrNull())
            }
            val myId = apiResult(json) { api.getMyFamilyId() }.getOrElse { error ->
                if ((error as? KiriApiException)?.code?.startsWith("FAMILY404") == true) return@run null
                throw error
            }.familyId
            ids.save(myId)
            apiResult(json) { api.getFamily(myId) }.getOrThrow()
        }
        familyCache = CachedFamily(info, now)
        return info
    }

    private fun FamilyInfoResponse.members() = members.map {
        FamilyMember(it.memberId.toString(), it.nickname, roleLabel(it.familyRole))
    }

    private fun roleLabel(role: String?) = when (role) {
        "MOTHER" -> "엄마"
        "FATHER" -> "아빠"
        "DAUGHTER" -> "딸"
        "SON" -> "아들"
        else -> ""
    }

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
        // 가족 이름·구성원은 가족 정보 API 에서 받는다. 홈은 매번 새로 받는다. 받지 못하면 예전처럼 답변 목록으로 채운다
        val info = if (daily.hasFamily && families != null && familyIds != null) {
            try { familyInfo(reuseWithinSeconds = null) } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
        } else null
        val me = family?.answers?.singleOrNull { it.isMe }?.memberId?.toString()
            ?: sessionToken?.let(::memberIdOf).orEmpty()
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
        return FamilyHome(date, me, if (daily.hasFamily) info?.familyName ?: "우리 가족" else null,
            info?.members() ?: family?.answers.orEmpty().map { FamilyMember(it.memberId.toString(), it.nickname, "") },
            question, answers, poked, mission = weekly, serverBacked = true,
            familyId = info?.familyId?.toString()).also { snapshot = it }
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
        throw HomeException(when {
            error.code == "QUESTION400_1" -> HomeFailure.EXPIRED
            error.code == "QUESTION409_2" -> HomeFailure.ALREADY_POKED
            error.code in setOf("QUESTION404_1", "QUESTION404_2", "QUESTION404_3", "QUESTION403_1", "QUESTION400_2") -> HomeFailure.UNAVAILABLE
            // TokenAuthenticator 가 재발급을 시도하고도 401 이 남았다
            error.code.startsWith("AUTH401") || error.code == "COMMON401_1" -> HomeFailure.SESSION_EXPIRED
            error.isServerError() -> HomeFailure.SERVER
            else -> HomeFailure.NETWORK
        })
    }
    catch (error: java.io.IOException) { throw HomeException(HomeFailure.NETWORK) }
}

// 서버 응답 code 가 COMMON500_1 처럼 5xx 이거나, 본문을 읽지 못한 HTTP 5xx(예: nginx 502 → HTTP502)
private val SERVER_ERROR_CODE = Regex("""(HTTP5\d\d|[A-Z]+5\d\d_\d+)""")

private fun KiriApiException.isServerError() = SERVER_ERROR_CODE.matches(code)

// 가족 정보를 재사용하는 시간(초). 홈을 열면 새로 받는다
private const val FAMILY_CACHE_SECONDS = 30L
