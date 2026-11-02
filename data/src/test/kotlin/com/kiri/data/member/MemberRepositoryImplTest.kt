package com.kiri.data.member

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.member.FamilyRole
import com.kiri.domain.model.nickname.DuplicateNicknameException
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class MemberRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): MemberRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MemberApi::class.java)
        return MemberRepositoryImpl(api, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun 닉네임을_PATCH로_보낸다() = runTest {
        enqueue(200, SUCCESS_BODY)

        val result = repository().updateNickname("재현")

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/members/nickname", request.url.encodedPath)
        val body = kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("재현", body["nickname"]!!.jsonPrimitive.content)
        assertTrue(result.isSuccess)
    }

    @Test
    fun 이미_사용_중인_닉네임이면_중복_오류로_실패한다() = runTest {
        enqueue(409, """{"isSuccess":false,"code":"MEMBER409_1","message":"이미 사용 중인 닉네임입니다.","result":null}""")

        val error = repository().updateNickname("재현").exceptionOrNull()

        assertTrue(error is DuplicateNicknameException)
    }

    @Test
    fun 입력_조건을_충족하지_않으면_서버의_code를_담아_실패한다() = runTest {
        enqueue(400, """{"isSuccess":false,"code":"COMMON400_1","message":"잘못된 요청입니다.","result":{"nickname":"닉네임은 필수입니다."}}""")

        val error = repository().updateNickname("").exceptionOrNull()

        assertEquals("COMMON400_1", (error as KiriApiException).code)
    }

    @Test
    fun 연결이_끊기면_네트워크_오류로_실패한다() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().updateNickname("재현").exceptionOrNull()

        assertTrue(error is IOException)
    }

    @Test
    fun 가족_역할을_PATCH로_보낸다() = runTest {
        enqueue(200, ROLE_SUCCESS_BODY)

        val result = repository().updateFamilyRole(FamilyRole.FATHER)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/members/family-role", request.url.encodedPath)
        val body = kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("FATHER", body["familyRole"]!!.jsonPrimitive.content)
        assertTrue(result.isSuccess)
    }

    @Test
    fun 가족_역할을_서버가_거절하면_서버의_code를_담아_실패한다() = runTest {
        enqueue(400, """{"isSuccess":false,"code":"COMMON400_1","message":"잘못된 요청입니다.","result":{"familyRole":"가족 역할은 필수입니다."}}""")

        val error = repository().updateFamilyRole(FamilyRole.MOTHER).exceptionOrNull()

        assertEquals("COMMON400_1", (error as KiriApiException).code)
    }

    @Test
    fun 가족_역할_저장_중_연결이_끊기면_네트워크_오류로_실패한다() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().updateFamilyRole(FamilyRole.SON).exceptionOrNull()

        assertTrue(error is IOException)
    }

    private companion object {
        const val ROLE_SUCCESS_BODY = """{"isSuccess":true,"code":"MEMBER200_2","message":"가족 역할이 설정되었습니다.","result":{"familyRole":"FATHER"}}"""
        const val SUCCESS_BODY = """{"isSuccess":true,"code":"MEMBER200_1","message":"닉네임이 설정되었습니다.","result":{"nickname":"재현"}}"""
    }
}
