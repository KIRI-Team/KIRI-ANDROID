package com.kiri.data.auth

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.auth.LoginResult
import com.kiri.domain.model.auth.SocialProvider
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class AuthRepositoryImplTest {

    private val server = MockWebServer()
    private val tokenStore = FakeTokenStore()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): AuthRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuthApi::class.java)
        return AuthRepositoryImpl(api, tokenStore, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun 로그인하면_소셜_종류와_토큰과_nonce를_보낸다() = runTest {
        enqueue(200, SUCCESS_BODY)

        repository().login(SocialProvider.KAKAO, "kakao-id-token", "nonce-1")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/login", request.url.encodedPath)
        val body = kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("KAKAO", body["socialProvider"]!!.jsonPrimitive.content)
        assertEquals("kakao-id-token", body["socialProviderToken"]!!.jsonPrimitive.content)
        assertEquals("nonce-1", body["nonce"]!!.jsonPrimitive.content)
    }

    @Test
    fun 로그인_상태는_저장된_토큰이_있는지를_따른다() = runTest {
        val repository = repository()
        assertFalse(repository.isLoggedIn.first())

        tokenStore.save(AuthTokens("access-1", "refresh-1"))
        assertTrue(repository.isLoggedIn.first())

        // 토큰 재발급이 거절되면 토큰이 지워진다
        tokenStore.clear()
        assertFalse(repository.isLoggedIn.first())
    }

    @Test
    fun 로그인에_성공하면_토큰을_저장하고_회원_상태를_돌려준다() = runTest {
        enqueue(200, SUCCESS_BODY)

        val result = repository().login(SocialProvider.GOOGLE, "google-id-token", "nonce-1")

        assertEquals(LoginResult(isNewUser = true, termsAgreed = false), result.getOrThrow())
        assertEquals(AuthTokens("access-1", "refresh-1"), tokenStore.saved)
    }

    @Test
    fun 서버가_실패로_답하면_서버의_code와_message를_담아_실패하고_토큰을_저장하지_않는다() = runTest {
        enqueue(401, """{"isSuccess":false,"code":"AUTH401_3","message":"소셜 인증 토큰 검증에 실패했습니다.","result":null}""")

        val error = repository().login(SocialProvider.KAKAO, "wrong", "nonce-1").exceptionOrNull()

        assertTrue(error is KiriApiException)
        assertEquals("AUTH401_3", (error as KiriApiException).code)
        assertEquals("소셜 인증 토큰 검증에 실패했습니다.", error.message)
        assertNull(tokenStore.saved)
    }

    @Test
    fun 성공_상태코드여도_isSuccess가_false면_실패로_본다() = runTest {
        enqueue(200, """{"isSuccess":false,"code":"AUTH400_1","message":"실패","result":null}""")

        val error = repository().login(SocialProvider.KAKAO, "token", "nonce-1").exceptionOrNull()

        assertEquals("AUTH400_1", (error as KiriApiException).code)
        assertNull(tokenStore.saved)
    }

    @Test
    fun 실패_응답_본문을_읽을_수_없으면_HTTP_상태코드로_구분한다() = runTest {
        enqueue(502, "<html>Bad Gateway</html>")

        val error = repository().login(SocialProvider.KAKAO, "token", "nonce-1").exceptionOrNull()

        assertEquals("HTTP502", (error as KiriApiException).code)
    }

    @Test
    fun 연결이_끊기면_네트워크_오류로_실패한다() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().login(SocialProvider.KAKAO, "token", "nonce-1").exceptionOrNull()

        assertTrue(error is IOException)
        assertNull(tokenStore.saved)
    }

    @Test
    fun 로그아웃하면_서버에_알리고_저장된_토큰을_지운다() = runTest {
        tokenStore.save(AuthTokens("access-1", "refresh-1"))
        enqueue(200, LOGOUT_BODY)

        val result = repository().logout()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/logout", request.url.encodedPath)
        assertTrue(result.isSuccess)
        assertNull(tokenStore.saved)
    }

    @Test
    fun 로그아웃을_서버가_거절해도_토큰을_지우고_실패를_돌려준다() = runTest {
        tokenStore.save(AuthTokens("access-1", "refresh-1"))
        enqueue(500, """{"isSuccess":false,"code":"COMMON500","message":"서버 오류","result":null}""")

        val error = repository().logout().exceptionOrNull()

        assertEquals("COMMON500", (error as KiriApiException).code)
        assertNull(tokenStore.saved)
    }

    @Test
    fun 로그아웃_중_연결이_끊겨도_토큰을_지운다() = runTest {
        tokenStore.save(AuthTokens("access-1", "refresh-1"))
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().logout().exceptionOrNull()

        assertTrue(error is IOException)
        assertNull(tokenStore.saved)
    }

    @Test
    fun 탈퇴하면_서버에_요청하고_저장된_토큰을_지운다() = runTest {
        tokenStore.save(AuthTokens("access-1", "refresh-1"))
        enqueue(200, WITHDRAW_BODY)

        val result = repository().withdraw()

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/members/me", request.url.encodedPath)
        assertTrue(result.isSuccess)
        assertNull(tokenStore.saved)
    }

    @Test
    fun 탈퇴를_서버가_거절하면_실패를_돌려주고_토큰을_남긴다() = runTest {
        tokenStore.save(AuthTokens("access-1", "refresh-1"))
        enqueue(500, """{"isSuccess":false,"code":"COMMON500","message":"서버 오류","result":null}""")

        val error = repository().withdraw().exceptionOrNull()

        assertEquals("COMMON500", (error as KiriApiException).code)
        assertEquals(AuthTokens("access-1", "refresh-1"), tokenStore.saved)
    }

    @Test
    fun 탈퇴_중_연결이_끊기면_토큰을_남긴다() = runTest {
        tokenStore.save(AuthTokens("access-1", "refresh-1"))
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().withdraw().exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals(AuthTokens("access-1", "refresh-1"), tokenStore.saved)
    }

    private companion object {
        const val WITHDRAW_BODY = """{"isSuccess":true,"code":"MEMBER200_2","message":"회원 탈퇴가 완료되었습니다.","result":null}"""
        const val LOGOUT_BODY = """{"isSuccess":true,"code":"AUTH200_3","message":"로그아웃 성공","result":null}"""

        const val SUCCESS_BODY = """{"isSuccess":true,"code":"AUTH200_1","message":"로그인 성공",""" +
            """"result":{"accessToken":"access-1","refreshToken":"refresh-1","isNewUser":true,"termsAgreed":false}}"""
    }
}
