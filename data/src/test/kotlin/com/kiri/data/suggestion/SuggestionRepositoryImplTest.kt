package com.kiri.data.suggestion

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import java.io.IOException
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SuggestionRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): SuggestionRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SuggestionApi::class.java)
        return SuggestionRepositoryImpl(api, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun 내용과_이미지_키를_POST로_보내고_건의_id를_돌려준다() = runTest {
        enqueue(200, SUCCESS_BODY)

        val result = repository().send("알림이 늦게 와요", "suggestions/abc.jpg")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/suggestions", request.url.encodedPath)
        val body = kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("알림이 늦게 와요", body["content"]!!.jsonPrimitive.content)
        assertEquals("suggestions/abc.jpg", body["imageKey"]!!.jsonPrimitive.content)
        assertEquals(1L, result.getOrThrow())
    }

    @Test
    fun 사진이_없으면_이미지_키를_보내지_않는다() = runTest {
        enqueue(200, SUCCESS_BODY)

        repository().send("좋아요", null)

        val body = kiriJson.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertFalse("imageKey" in body)
    }

    @Test
    fun 내용이_없거나_길면_서버의_code를_담아_실패한다() = runTest {
        enqueue(400, """{"isSuccess":false,"code":"COMMON400_1","message":"잘못된 요청입니다.","result":{"content":"건의 내용은 필수입니다."}}""")

        val error = repository().send("", null).exceptionOrNull()

        assertEquals("COMMON400_1", (error as KiriApiException).code)
    }

    @Test
    fun 회원이_없으면_서버의_code를_담아_실패한다() = runTest {
        enqueue(404, """{"isSuccess":false,"code":"MEMBER404_1","message":"존재하지 않는 회원입니다.","result":null}""")

        val error = repository().send("좋아요", null).exceptionOrNull()

        assertEquals("MEMBER404_1", (error as KiriApiException).code)
    }

    @Test
    fun 연결이_끊기면_네트워크_오류로_실패한다() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().send("좋아요", null).exceptionOrNull()

        assertTrue(error is IOException)
    }

    private companion object {
        const val SUCCESS_BODY = """{"isSuccess":true,"code":"SUGGESTION201_1","message":"건의 작성에 성공했습니다.","result":{"suggestionId":1}}"""
    }
}
