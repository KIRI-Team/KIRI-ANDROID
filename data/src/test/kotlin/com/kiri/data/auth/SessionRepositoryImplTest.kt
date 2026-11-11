package com.kiri.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.kiri.data.network.*
import com.kiri.data.family.*
import com.kiri.data.term.*
import com.kiri.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import mockwebserver3.*
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import org.junit.Assert.*
import org.junit.Test

class SessionRepositoryImplTest {
    private class MemoryPreferences : DataStore<Preferences> {
        val values = MutableStateFlow<Preferences>(emptyPreferences())
        override val data: Flow<Preferences> = values
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = transform(values.value).also { values.value = it }
    }
    private val tokens = FakeTokenStore(AuthTokens("access", "refresh"))
    private val ids = FakeFamilyIdStore()
    private val prefs = MemoryPreferences()
    private fun repository(server: MockWebServer): SessionRepositoryImpl {
        val retrofit = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType())).build()
        return SessionRepositoryImpl(retrofit.create(TermApi::class.java), retrofit.create(FamilyApi::class.java), tokens, ids, kiriJson, prefs)
    }
    private fun MockWebServer.reply(body: String, code: Int = 200) = enqueue(MockResponse.Builder().code(code).body(body).build())
    private val agreed = """{"isSuccess":true,"code":"OK","message":"ok","result":{"termsAgreed":true}}"""
    private val family = """{"isSuccess":true,"code":"OK","message":"ok","result":{"familyId":7}}"""
    private val noFamily = """{"isSuccess":false,"code":"FAMILY404_1","message":"없음","result":null}"""

    @Test fun noTokensSkipsNetworkAndExpiredSessionReturnsToLogin() = runTest {
        MockWebServer().use { server ->
            server.start(); val repo = repository(server)
            tokens.clear(); assertEquals(SessionDestination.LOGIN, repo.restore().getOrThrow()); assertEquals(0, server.requestCount)
            // 재발급을 서버가 거절하면 TokenAuthenticator 가 토큰을 지운다. 그 뒤 401 이 오면 로그인으로 간다
            tokens.save(AuthTokens("bad", "bad"))
            server.enqueue(MockResponse.Builder().code(401)
                .body("""{"isSuccess":false,"code":"AUTH401_2","message":"토큰이 만료되었습니다.","result":null}""").build())
            // 익명 객체 안에서 tokens 는 TokenStore 의 tokens(Flow) 속성을 가리키므로, 바깥 저장소는 따로 받아 둔다
            val saved = tokens
            val clearedDuringCall = object : TokenStore by saved {
                var calls = 0
                override suspend fun get(): AuthTokens? = if (calls++ == 0) saved.get() else null
            }
            val retrofit = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType())).build()
            val repo2 = SessionRepositoryImpl(retrofit.create(TermApi::class.java), retrofit.create(FamilyApi::class.java), clearedDuringCall, ids, kiriJson, prefs)
            assertEquals(SessionDestination.LOGIN, repo2.restore().getOrThrow())
        }
    }
    @Test fun tokenFailureWithTokensStillSavedIsRetryableInsteadOfLogout() = runTest {
        MockWebServer().use { server ->
            server.start(); val repo = repository(server)
            // 재발급이 연결·서버 문제로 실패하면 토큰이 남은 채 401 이 온다. 다시 로그인시키지 않는다
            listOf("AUTH401_1", "AUTH401_2").forEach { code ->
                server.reply("""{"isSuccess":false,"code":"$code","message":"token","result":null}""", 401)
                assertTrue(repo.restore().isFailure); assertNotNull(tokens.get())
            }
            server.reply(agreed)
            server.reply("""{"isSuccess":false,"code":"AUTH401_2","message":"token","result":null}""", 401)
            assertTrue(repo.restore().isFailure); assertNotNull(tokens.get())
            // 탈퇴 등으로 회원이 없으면 로그인으로 간다
            server.reply("""{"isSuccess":false,"code":"MEMBER404_1","message":"없음","result":null}""", 404)
            assertEquals(SessionDestination.LOGIN, repo.restore().getOrThrow()); assertNull(tokens.get())
        }
    }
    @Test fun existingFamilyRestoresIdAndMainButInterruptedProfileRestoresOnboarding() = runTest {
        MockWebServer().use { server ->
            server.start(); val repo = repository(server)
            server.reply(agreed); server.reply(family)
            assertEquals(SessionDestination.MAIN, repo.restore().getOrThrow()); assertEquals(7L, ids.saved)
            repo.saveNickname("주연")
            server.reply(agreed); server.reply(family)
            assertEquals(SessionDestination.ONBOARDING, repo.restore().getOrThrow())
            assertEquals(ProfileProgress("주연", null), repo.profile())
            repo.saveRole("DAUGHTER")
            server.reply(agreed); server.reply(family)
            assertEquals(SessionDestination.MAIN, repo.restore().getOrThrow())
        }
    }
    @Test fun browsingChoicePersistsAndNetworkFailureNeverPretendsUserHasNoFamily() = runTest {
        MockWebServer().use { server ->
            server.start(); val repo = repository(server)
            server.reply(agreed); server.reply(noFamily, 404)
            assertEquals(SessionDestination.ONBOARDING, repo.restore().getOrThrow())
            repo.completeOnboarding(); server.reply(agreed); server.reply(noFamily, 404)
            assertEquals(SessionDestination.MAIN, repository(server).restore().getOrThrow())
            server.reply(agreed); server.reply("""{"isSuccess":false,"code":"COMMON500_1","message":"failed","result":null}""", 500)
            assertTrue(repo.restore().isFailure); assertNotNull(tokens.get())
        }
    }
    @Test fun missingRequiredTermsDoesNotRequestFamilyOrEnterMain() = runTest {
        MockWebServer().use { server ->
            server.start(); server.reply("""{"isSuccess":true,"code":"OK","message":"ok","result":{"termsAgreed":false}}""")
            assertEquals(SessionDestination.TERMS, repository(server).restore().getOrThrow()); assertEquals(1, server.requestCount)
        }
    }
}
