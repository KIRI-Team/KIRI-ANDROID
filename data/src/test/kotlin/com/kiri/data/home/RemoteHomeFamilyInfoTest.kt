package com.kiri.data.home

import com.kiri.data.auth.AuthInterceptor
import com.kiri.data.auth.AuthTokens
import com.kiri.data.auth.TokenStore
import com.kiri.data.auth.memberIdOf
import com.kiri.data.family.FakeFamilyIdStore
import com.kiri.data.family.FamilyApi
import com.kiri.data.network.kiriJson
import com.kiri.domain.home.FamilyMember
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// 가족 정보만 필요한 곳(family())은 가족 정보 API 하나로 채우고, 홈(load())도 가족 이름·구성원을 그 API 에서 받는지 확인한다
class RemoteHomeFamilyInfoTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneOffset.UTC)

    // sub 가 1 인 로그인 토큰 (header.payload.signature)
    private val token = "e30.eyJzdWIiOiIxIn0.sig"

    private class Tokens(access: String) : TokenStore {
        private val state = MutableStateFlow<AuthTokens?>(AuthTokens(access, "refresh"))
        override val tokens: Flow<AuthTokens?> = state
        override suspend fun get() = state.value
        override suspend fun save(tokens: AuthTokens) { state.value = tokens }
        override suspend fun clear() { state.value = null }
    }

    private fun repository(server: MockWebServer, ids: FakeFamilyIdStore): RemoteHomeRepository {
        val tokens = Tokens(token)
        val retrofit = Retrofit.Builder().baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokens)).build())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
        return RemoteHomeRepository(retrofit.create(HomeApi::class.java), tokens, kiriJson, clock,
            families = retrofit.create(FamilyApi::class.java), familyIds = ids)
    }

    private fun MockWebServer.reply(status: Int, body: String) = enqueue(MockResponse.Builder().code(status).body(body).build())

    private fun familyBody(id: Long) = """{"isSuccess":true,"code":"FAMILY200_9","message":"ok","result":{"familyId":$id,"familyName":"끼리네가족",""" +
        """"ownerMemberId":1,"memberCount":3,"members":[{"memberId":1,"nickname":"재현1","familyRole":"FATHER","characterImageUrl":null},""" +
        """{"memberId":2,"nickname":"재현2","familyRole":"MOTHER"},{"memberId":3,"nickname":"재현3","familyRole":null}]}}"""

    private fun failure(code: String) = """{"isSuccess":false,"code":"$code","message":"실패","result":null}"""

    private val members = listOf(FamilyMember("1", "재현1", "아빠"), FamilyMember("2", "재현2", "엄마"), FamilyMember("3", "재현3", ""))

    @Test fun 가족_정보는_가족_정보_API_하나로_채우고_잠깐_재사용한다() = runTest {
        MockWebServer().use { server ->
            server.start(); server.reply(200, familyBody(9))
            val repo = repository(server, FakeFamilyIdStore(saved = 9))

            val family = repo.family()

            assertEquals("/api/v1/families/9", server.takeRequest().url.encodedPath)
            assertEquals("끼리네가족", family.familyName)
            assertEquals(members, family.members)
            assertEquals("1", family.currentUserId)
            assertEquals("9", family.familyId)
            assertNull(family.question)
            // 여러 곳이 잇달아 찾아도 다시 부르지 않는다
            repo.family(); repo.family()
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun 기억한_가족_ID가_없으면_가족_ID를_받아_기억하고_가족이_없으면_둘러보기다() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.reply(200, """{"isSuccess":true,"code":"FAMILY200_6","message":"ok","result":{"familyId":5}}""")
            server.reply(200, familyBody(5))
            val ids = FakeFamilyIdStore()
            assertEquals("끼리네가족", repository(server, ids).family().familyName)
            assertEquals("/api/v1/families/me/id", server.takeRequest().url.encodedPath)
            assertEquals("/api/v1/families/5", server.takeRequest().url.encodedPath)
            assertEquals(5L, ids.saved)

            server.reply(404, failure("FAMILY404_1"))
            val none = repository(server, FakeFamilyIdStore()).family()
            assertTrue(none.browsing)
            assertTrue(none.members.isEmpty())
        }
    }

    @Test fun 기억한_가족이_더는_내_가족이_아니면_가족_ID를_다시_받아_불러온다() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.reply(403, failure("FAMILY403_1"))
            server.reply(200, """{"isSuccess":true,"code":"FAMILY200_6","message":"ok","result":{"familyId":10}}""")
            server.reply(200, familyBody(10))
            val ids = FakeFamilyIdStore(saved = 9)

            val family = repository(server, ids).family()

            assertEquals("10", family.familyId)
            assertEquals(10L, ids.saved)
            assertEquals(listOf("/api/v1/families/9", "/api/v1/families/me/id", "/api/v1/families/10"),
                List(3) { server.takeRequest().url.encodedPath })
        }
    }

    @Test fun 홈은_질문이_없어도_가족_이름과_구성원을_가족_정보로_채우고_매번_새로_받는다() = runTest {
        MockWebServer().use { server ->
            server.start()
            val repo = repository(server, FakeFamilyIdStore(saved = 9))
            repeat(2) {
                server.reply(404, failure("QUESTION404_1"))
                server.reply(200, familyBody(9))
                val home = repo.load()
                assertEquals("끼리네가족", home.familyName)
                assertEquals(members, home.members)
                assertEquals("1", home.currentUserId)
            }
            assertEquals(4, server.requestCount)
        }
    }

    @Test fun 홈에서_가족_정보를_받지_못하면_답변_목록으로_채운다() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.reply(200, """{"isSuccess":true,"code":"OK","message":"ok","result":{"hasFamily":true,"questionId":12,"question":"질문","isAnswered":false,"answer":null}}""")
            server.reply(200, """{"isSuccess":true,"code":"OK","message":"ok","result":{"question":"질문","answers":[{"isMe":true,"memberId":1,"nickname":"나","isAnswered":false,"answer":null}]}}""")
            server.reply(500, failure("COMMON500_1"))

            val home = repository(server, FakeFamilyIdStore(saved = 9)).load()

            assertEquals("우리 가족", home.familyName)
            assertEquals(listOf(FamilyMember("1", "나", "")), home.members)
        }
    }

    @Test fun 로그인_토큰에서_내_회원_id를_꺼내고_형식이_다르면_null이다() {
        assertEquals("1", memberIdOf(token))
        assertNull(memberIdOf("access"))
        assertNull(memberIdOf("a.@@@.c"))
    }
}
