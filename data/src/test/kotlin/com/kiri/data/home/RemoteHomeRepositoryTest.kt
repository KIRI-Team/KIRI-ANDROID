package com.kiri.data.home

import com.kiri.data.auth.*
import com.kiri.data.mission.MissionApi
import com.kiri.data.network.kiriJson
import com.kiri.domain.home.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RemoteHomeRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-22T16:00:00Z"), ZoneOffset.UTC)
    private class Tokens : TokenStore {
        private val state = MutableStateFlow<AuthTokens?>(AuthTokens("access", "refresh"))
        var value: AuthTokens?
            get() = state.value
            set(tokens) { state.value = tokens }
        override val tokens: Flow<AuthTokens?> = state
        override suspend fun get() = value
        override suspend fun save(tokens: AuthTokens) { value = tokens }
        override suspend fun clear() { value = null }
    }
    // 실제 앱처럼 AuthInterceptor 가 토큰을 붙인다
    private fun repository(server: MockWebServer, tokens: Tokens = Tokens(), weekly: Boolean = false): RemoteHomeRepository {
        val retrofit = Retrofit.Builder().baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokens)).build())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
        return RemoteHomeRepository(retrofit.create(HomeApi::class.java), tokens, kiriJson, clock,
            retrofit.create(MissionApi::class.java).takeIf { weekly })
    }

    @Test fun weeklyRecommendationKeepsMissionIdAndDoesNotEnableAssignmentScheduling() = runTest {
        MockWebServer().use { server ->
            server.start(); server.family()
            server.reply("""{"missionId":130,"title":"미술관 관람","description":"전시 둘러보기","recommendationReason":"가족 취향 반영"}""")
            server.reply("null")
            val repo = repository(server, weekly = true)
            val home = repo.load()
            val weekly = requireNotNull(home.mission)
            assertEquals("130", weekly.missionId)
            org.junit.Assert.assertFalse(weekly.recommendationOnly)
            assertEquals(weekly, repo.saveAnswer(requireNotNull(home.question), "답변").mission)
            server.takeRequest(); server.takeRequest()
            assertEquals("/api/v1/missions/recommendations", server.takeRequest().url.encodedPath)
        }
    }

    @Test fun weeklyRecommendationFailureKeepsQuestionVisible() = runTest {
        MockWebServer().use { server ->
            server.start(); server.family(); server.reply("null", status = 503)
            val home = repository(server, weekly = true).load()
            assertEquals("12", home.question?.id)
            assertNull(home.mission)
        }
    }
    private fun MockWebServer.reply(result: String, code: String = "OK", status: Int = 200) {
        enqueue(MockResponse.Builder().code(status).body("""{"isSuccess":${status == 200},"code":"$code","message":"message","result":$result}""").build())
    }
    private fun MockWebServer.family() {
        reply("""{"hasFamily":true,"questionId":12,"question":"질문","isAnswered":false,"answer":null}""")
        reply("""{"question":"질문","answers":[{"isMe":true,"memberId":1,"nickname":"나","isAnswered":false,"answer":null},{"isMe":false,"memberId":2,"nickname":"엄마","isAnswered":true,"answer":"답변"}]}""")
    }
    @Test fun mapsRealIdsAndKoreanDateAndSendsBearerToken() = runTest {
        MockWebServer().use { server ->
            server.start(); server.family()
            val home = repository(server).load()
            assertEquals("2026-10-23", home.date.toString())
            assertEquals("1", home.currentUserId)
            assertEquals("12", home.question!!.id)
            assertEquals("답변", home.answers["2"])
            assertNull(home.mission); assertNull(home.inviteCode)
            assertEquals("Bearer access", server.takeRequest().headers["Authorization"])
            val answers = server.takeRequest()
            assertEquals("/api/v1/questions/12/answers", answers.url.encodedPath)
            assertEquals("Bearer access", answers.headers["Authorization"])
        }
    }
    @Test fun nullResultMutationsSucceedAndAnsweredMemberCanBePokedOnce() = runTest {
        MockWebServer().use { server ->
            server.start(); server.family(); server.reply("null"); server.reply("null")
            val repo = repository(server)
            val home = repo.load()
            assertTrue(home.canPoke("2"))
            val saved = repo.saveAnswer(home.question!!, "새 답변")
            assertEquals("새 답변", saved.answers["1"])
            assertTrue(repo.poke(home.question!!, "2").pokedMemberIds.contains("2"))
            val error = runCatching { repo.poke(home.question!!, "2") }.exceptionOrNull() as HomeException
            assertEquals(HomeFailure.ALREADY_POKED, error.reason)
            server.takeRequest(); server.takeRequest()
            val request = server.takeRequest()
            assertEquals("PUT", request.method)
            assertEquals("/api/v1/questions/12/answers/me", request.url.encodedPath)
            assertEquals("POST", server.takeRequest().method)
        }
    }
    @Test fun noFamilyDoesNotRequestAnswers() = runTest {
        MockWebServer().use { server ->
            server.start(); server.reply("""{"hasFamily":false}""")
            assertTrue(repository(server).load().browsing)
            assertEquals(1, server.requestCount)
        }
    }
    @Test fun validatesAnswerLengthAndMapsDuplicatePoke() = runTest {
        MockWebServer().use { server ->
            server.start(); server.family(); server.reply("null", "QUESTION409_2", 409)
            val repo = repository(server); val home = repo.load()
            assertEquals(HomeFailure.ANSWER_TOO_LONG, (runCatching { repo.saveAnswer(home.question!!, "가".repeat(501)) }.exceptionOrNull() as HomeException).reason)
            assertEquals(2, server.requestCount)
            assertEquals(HomeFailure.ALREADY_POKED, (runCatching { repo.poke(home.question!!, "2") }.exceptionOrNull() as HomeException).reason)
        }
    }

    @Test fun unassignedQuestionUsesEmptyStateInsteadOfLoadFailure() = runTest {
        MockWebServer().use { server ->
            server.start(); server.reply("null", "QUESTION404_1", 404)
            val home = repository(server).load()
            assertFalse(home.browsing)
            assertNull(home.question)
            assertTrue(home.members.isEmpty())
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun loadFailuresAreSplitIntoServerSessionAndNetwork() = runTest {
        MockWebServer().use { server ->
            server.start(); val repo = repository(server)
            suspend fun reason() = (runCatching { repo.load() }.exceptionOrNull() as HomeException).reason
            // nginx 가 HTML 로 답한 502
            server.enqueue(MockResponse.Builder().code(502).body("<html><head><title>502 Bad Gateway</title></head></html>").build())
            assertEquals(HomeFailure.SERVER, reason())
            server.reply("null", code = "COMMON500_1", status = 500)
            assertEquals(HomeFailure.SERVER, reason())
            // 재발급 후에도 남은 만료 토큰 401
            server.reply("null", code = "AUTH401_2", status = 401)
            assertEquals(HomeFailure.SESSION_EXPIRED, reason())
            server.reply("null", code = "UNKNOWN400_1", status = 400)
            assertEquals(HomeFailure.NETWORK, reason())
        }
    }

    @Test fun missingTokenDoesNotSendRequest() = runTest {
        MockWebServer().use { server ->
            server.start()
            val tokens = Tokens().apply { value = null }
            val error = runCatching { repository(server, tokens).load() }.exceptionOrNull() as HomeException
            assertEquals(HomeFailure.UNAVAILABLE, error.reason)
            assertEquals(0, server.requestCount)
        }
    }

    @Test fun failedMutationDoesNotReplaceSavedAnswer() = runTest {
        MockWebServer().use { server ->
            server.start(); server.family(); server.reply("null", "QUESTION409_1", 409)
            server.reply("null")
            val repo = repository(server); val home = repo.load()
            assertTrue(runCatching { repo.saveAnswer(home.question!!, "실패 답변") }.isFailure)
            assertFalse(repo.poke(home.question!!, "2").answers.containsKey("1"))
        }
    }
}
