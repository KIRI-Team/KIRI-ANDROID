package com.kiri.data.auth

import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

class TokenAuthenticatorTest {

    private interface TestApi {
        @GET("api/v1/me")
        fun me(): Call<ResponseBody>

        @NoAuth
        @GET("api/v1/open")
        fun open(): Call<ResponseBody>
    }

    // 재발급 API 와 토큰이 필요한 API 를 흉내 내는 서버
    // 토큰이 필요한 API 는 Bearer {validAccessToken} 일 때만 200, 아니면 401 + unauthorizedCode 로 답한다
    private inner class FakeServer : Dispatcher() {
        @Volatile var validAccessToken = NEW_ACCESS
        @Volatile var unauthorizedCode = "AUTH401_1"
        @Volatile var reissueResponse: () -> MockResponse = { json(200, reissueBody(NEW_ACCESS, NEW_REFRESH)) }

        // 401 을 돌려주기 직전에 부른다 (동시 요청, 늦게 도착한 401 흉내)
        @Volatile var beforeUnauthorized: () -> Unit = {}
        val reissueCalls = AtomicInteger()
        val requests = mutableListOf<RecordedRequest>()

        override fun dispatch(request: RecordedRequest): MockResponse {
            synchronized(requests) { requests += request }
            return when (request.url.encodedPath) {
                "/api/v1/auth/reissue" -> {
                    reissueCalls.incrementAndGet()
                    reissueResponse()
                }
                "/api/v1/open" -> json(200, "{}")
                else -> if (request.headers["Authorization"] == "Bearer $validAccessToken") {
                    json(200, "{}")
                } else {
                    beforeUnauthorized()
                    json(401, """{"isSuccess":false,"code":"$unauthorizedCode","message":"인증 실패","result":null}""")
                }
            }
        }

        fun requestsTo(path: String) = synchronized(requests) { requests.filter { it.url.encodedPath == path } }
    }

    private val server = MockWebServer()
    private val fakeServer = FakeServer()
    private val tokenStore = FakeTokenStore(AuthTokens(OLD_ACCESS, OLD_REFRESH))

    @Before
    fun setUp() {
        server.dispatcher = fakeServer
        server.start()
    }

    @After
    fun tearDown() = server.close()

    private fun retrofit(client: OkHttpClient) = Retrofit.Builder()
        .baseUrl(apiBaseUrl(server.url("/").toString()))
        .client(client)
        .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
        .build()

    // 앱과 같은 구성: 메인 client 에 토큰 처리를 붙이고, 재발급은 토큰 처리 없는 client 로 보낸다
    private fun authClient(): OkHttpClient {
        val reissueApi = retrofit(OkHttpClient()).create(ReissueApi::class.java)
        return OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .authenticator(TokenAuthenticator(tokenStore, reissueApi, kiriJson))
            .build()
    }

    private fun api(): TestApi = retrofit(authClient()).create(TestApi::class.java)

    private fun json(code: Int, body: String) = MockResponse.Builder().code(code).body(body).build()

    @Test
    fun 저장된_토큰이_있으면_Authorization_헤더를_붙인다() {
        fakeServer.validAccessToken = OLD_ACCESS

        val response = api().me().execute()

        assertEquals(200, response.code())
        assertEquals("Bearer $OLD_ACCESS", fakeServer.requestsTo("/api/v1/me").single().headers["Authorization"])
    }

    @Test
    fun NoAuth가_붙은_API에는_토큰을_붙이지_않는다() {
        api().open().execute()

        assertNull(fakeServer.requestsTo("/api/v1/open").single().headers["Authorization"])
    }

    @Test
    fun 로그인_API에는_토큰을_붙이지_않는다() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = json(200, LOGIN_BODY)
        }

        retrofit(authClient()).create(AuthApi::class.java).login(LoginRequest("KAKAO", "id-token", "nonce"))

        assertNull(server.takeRequest().headers["Authorization"])
    }

    @Test
    fun 토큰이_없으면_헤더_없이_보내고_401이어도_재발급하지_않는다() = runBlocking {
        tokenStore.clear()

        val response = api().me().execute()

        assertEquals(401, response.code())
        assertNull(fakeServer.requestsTo("/api/v1/me").single().headers["Authorization"])
        assertEquals(0, fakeServer.reissueCalls.get())
    }

    @Test
    fun 유효하지_않은_토큰이면_재발급한_토큰을_저장하고_같은_요청을_다시_보낸다() {
        val response = api().me().execute()

        assertEquals(200, response.code())
        assertEquals(AuthTokens(NEW_ACCESS, NEW_REFRESH), tokenStore.saved)
        val reissue = fakeServer.requestsTo("/api/v1/auth/reissue").single()
        assertNull(reissue.headers["Authorization"])
        val body = kiriJson.parseToJsonElement(reissue.body!!.utf8()).jsonObject
        assertEquals(OLD_REFRESH, body["refreshToken"]!!.jsonPrimitive.content)
        val sentTokens = fakeServer.requestsTo("/api/v1/me").map { it.headers["Authorization"] }
        assertEquals(listOf("Bearer $OLD_ACCESS", "Bearer $NEW_ACCESS"), sentTokens)
    }

    @Test
    fun 여러_요청이_동시에_401을_받아도_재발급은_한_번만_한다() {
        val requestCount = 3
        // 세 요청이 모두 예전 토큰으로 서버에 도착한 뒤에 한꺼번에 401 을 돌려준다
        val allArrived = CountDownLatch(requestCount)
        fakeServer.beforeUnauthorized = {
            allArrived.countDown()
            allArrived.await(5, TimeUnit.SECONDS)
        }
        val api = api()
        val executor = Executors.newFixedThreadPool(requestCount)

        val codes = try {
            (1..requestCount).map { executor.submit<Int> { api.me().execute().code() } }.map { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        assertEquals(List(requestCount) { 200 }, codes)
        assertEquals(1, fakeServer.reissueCalls.get())
        assertEquals(1, tokenStore.saves)
        assertEquals(AuthTokens(NEW_ACCESS, NEW_REFRESH), tokenStore.saved)
    }

    @Test
    fun 다른_요청이_이미_재발급했으면_재발급하지_않고_새_토큰으로_다시_보낸다() {
        // 예전 토큰으로 보낸 요청의 401 이 도착하기 전에 다른 요청이 재발급을 마친 상황
        fakeServer.beforeUnauthorized = { runBlocking { tokenStore.save(AuthTokens(NEW_ACCESS, NEW_REFRESH)) } }

        val response = api().me().execute()

        assertEquals(200, response.code())
        assertEquals(0, fakeServer.reissueCalls.get())
        assertEquals("Bearer $NEW_ACCESS", fakeServer.requestsTo("/api/v1/me").last().headers["Authorization"])
    }

    @Test
    fun 토큰_문제가_아닌_401은_재발급하지_않는다() {
        fakeServer.unauthorizedCode = "AUTH401_2"

        val response = api().me().execute()

        assertEquals(401, response.code())
        assertEquals(0, fakeServer.reissueCalls.get())
        assertEquals(AuthTokens(OLD_ACCESS, OLD_REFRESH), tokenStore.saved)
    }

    @Test
    fun 재발급한_토큰으로도_401이면_더_반복하지_않는다() {
        fakeServer.validAccessToken = "never-valid"

        val response = api().me().execute()

        assertEquals(401, response.code())
        assertEquals(1, fakeServer.reissueCalls.get())
        assertEquals(2, fakeServer.requestsTo("/api/v1/me").size)
    }

    @Test
    fun 재발급을_서버가_거절하면_토큰을_지운다() {
        fakeServer.reissueResponse = {
            json(401, """{"isSuccess":false,"code":"AUTH401_1","message":"유효하지 않은 토큰","result":null}""")
        }

        val response = api().me().execute()

        assertEquals(401, response.code())
        assertNull(tokenStore.saved)
    }

    @Test
    fun 재발급_중_서버_오류가_나면_토큰을_남긴다() {
        fakeServer.reissueResponse = { json(500, "<html>Internal Server Error</html>") }

        val response = api().me().execute()

        assertEquals(401, response.code())
        assertEquals(AuthTokens(OLD_ACCESS, OLD_REFRESH), tokenStore.saved)
    }

    @Test
    fun 재발급_중_연결이_끊기면_토큰을_남긴다() {
        fakeServer.reissueResponse = { MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build() }

        val response = api().me().execute()

        assertEquals(401, response.code())
        assertEquals(AuthTokens(OLD_ACCESS, OLD_REFRESH), tokenStore.saved)
    }

    @Test
    fun 재발급_응답을_읽을_수_없으면_토큰을_남긴다() {
        fakeServer.reissueResponse = {
            json(200, """{"isSuccess":true,"code":"AUTH200_2","message":"토큰 재발급 성공","result":{"accessToken":"$NEW_ACCESS"}}""")
        }

        val response = api().me().execute()

        assertEquals(401, response.code())
        assertEquals(AuthTokens(OLD_ACCESS, OLD_REFRESH), tokenStore.saved)
    }

    private companion object {
        const val OLD_ACCESS = "access-1"
        const val OLD_REFRESH = "refresh-1"
        const val NEW_ACCESS = "access-2"
        const val NEW_REFRESH = "refresh-2"
        const val LOGIN_BODY =
            """{"isSuccess":true,"code":"AUTH200_1","message":"로그인 성공","result":{"accessToken":"a","refreshToken":"r","isNewUser":true,"termsAgreed":false}}"""

        fun reissueBody(accessToken: String, refreshToken: String) =
            """{"isSuccess":true,"code":"AUTH200_2","message":"토큰 재발급 성공","result":{"accessToken":"$accessToken","refreshToken":"$refreshToken","termsAgreed":true}}"""
    }
}
