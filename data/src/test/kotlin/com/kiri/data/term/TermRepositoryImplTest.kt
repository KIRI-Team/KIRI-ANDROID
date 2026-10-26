package com.kiri.data.term

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.term.Term
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class TermRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): TermRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TermApi::class.java)
        return TermRepositoryImpl(api, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun 약관_목록을_받아_순서대로_돌려준다() = runTest {
        enqueue(200, TERMS_BODY)

        val terms = repository().getTerms().getOrThrow()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/terms", request.url.encodedPath)
        assertEquals(
            listOf(
                Term(id = 1, title = "서비스 이용약관", content = "제1조(목적) 이 약관은 ...", required = true),
                Term(id = 3, title = "마케팅 정보 수신 동의", content = "이벤트, 혜택 등 ...", required = false),
            ),
            terms,
        )
    }

    @Test
    fun 약관_목록_조회에_실패하면_서버의_code를_담아_실패한다() = runTest {
        enqueue(500, """{"isSuccess":false,"code":"COMMON500","message":"서버 오류","result":null}""")

        val error = repository().getTerms().exceptionOrNull()

        assertEquals("COMMON500", (error as KiriApiException).code)
    }

    @Test
    fun 동의하면_약관마다_동의_여부를_보낸다() = runTest {
        enqueue(200, AGREE_BODY)

        repository().agree(linkedMapOf(1L to true, 3L to false))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/terms/agreements", request.url.encodedPath)
        val agreements = kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject["agreements"]!!.jsonArray
            .map { it.jsonObject["termId"]!!.jsonPrimitive.long to it.jsonObject["isAgreed"]!!.jsonPrimitive.boolean }
        assertEquals(listOf(1L to true, 3L to false), agreements)
    }

    @Test
    fun 동의_저장_응답의_result가_null이어도_성공으로_본다() = runTest {
        enqueue(200, AGREE_BODY)

        val result = repository().agree(mapOf(1L to true))

        assertTrue(result.isSuccess)
    }

    @Test
    fun 성공_상태코드여도_isSuccess가_false면_동의_저장에_실패한다() = runTest {
        enqueue(200, """{"isSuccess":false,"code":"TERM400_1","message":"필수 약관 미동의","result":null}""")

        val error = repository().agree(mapOf(1L to false)).exceptionOrNull()

        assertEquals("TERM400_1", (error as KiriApiException).code)
    }

    @Test
    fun 동의_저장_중_연결이_끊기면_네트워크_오류로_실패한다() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().agree(mapOf(1L to true)).exceptionOrNull()

        assertTrue(error is IOException)
    }

    private companion object {
        // 화면에서 쓰지 않는 shortDescription, updatedAt 이 와도 읽을 수 있어야 한다
        const val TERMS_BODY = """{"isSuccess":true,"code":"TERM200_1","message":"약관 목록 조회 성공","result":[""" +
            """{"termId":1,"title":"서비스 이용약관","shortDescription":"서비스 이용 기준 안내",""" +
            """"description":"제1조(목적) 이 약관은 ...","isRequired":true,"updatedAt":"2026-10-04T12:00:00"},""" +
            """{"termId":3,"title":"마케팅 정보 수신 동의","shortDescription":"이벤트·업데이트 안내",""" +
            """"description":"이벤트, 혜택 등 ...","isRequired":false,"updatedAt":"2026-10-04T12:00:00"}]}"""
        const val AGREE_BODY = """{"isSuccess":true,"code":"TERM200_2","message":"약관 동의가 저장되었습니다.","result":null}"""
    }
}
