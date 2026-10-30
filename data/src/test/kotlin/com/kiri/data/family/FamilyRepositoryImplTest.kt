package com.kiri.data.family

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.family.DuplicateFamilyNameException
import com.kiri.domain.model.family.Family
import com.kiri.domain.model.invite.Invite
import java.io.IOException
import java.time.LocalDate
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class FamilyRepositoryImplTest {

    private val server = MockWebServer()
    private val familyIdStore = FakeFamilyIdStore()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): FamilyRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FamilyApi::class.java)
        return FamilyRepositoryImpl(api, familyIdStore, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun 가족명을_POST로_보낸다() = runTest {
        enqueue(201, SUCCESS_BODY)

        repository().createFamily("끼리네가족")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/families", request.url.encodedPath)
        val body = kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("끼리네가족", body["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun 만들면_가족과_함께_발급된_초대를_돌려준다() = runTest {
        enqueue(201, SUCCESS_BODY)

        val family = repository().createFamily("끼리네가족").getOrThrow()

        assertEquals(
            Family(
                id = 1,
                name = "끼리네가족",
                invite = Invite(code = "NWTKYJ", url = "https://kiri.app/invite/NWTKYJ", expiresAt = LocalDate.of(2026, 10, 14)),
            ),
            family,
        )
    }

    @Test
    fun 만들면_초대를_불러올_수_있도록_가족_ID를_기억한다() = runTest {
        enqueue(201, SUCCESS_BODY)

        repository().createFamily("끼리네가족")

        assertEquals(1L, familyIdStore.saved)
    }

    @Test
    fun 만들지_못하면_가족_ID를_기억하지_않는다() = runTest {
        enqueue(409, """{"isSuccess":false,"code":"FAMILY409_1","message":"이미 존재하는 가족 이름입니다.","result":null}""")

        repository().createFamily("끼리네가족")

        assertNull(familyIdStore.saved)
    }

    @Test
    fun 만료일을_읽지_못하면_만료일만_비운다() = runTest {
        enqueue(201, SUCCESS_BODY.replace("2026-10-14T11:51:36", "곧"))

        val family = repository().createFamily("끼리네가족").getOrThrow()

        assertEquals("NWTKYJ", family.invite.code)
        assertNull(family.invite.expiresAt)
    }

    @Test
    fun 같은_이름의_가족이_있으면_중복_오류로_실패한다() = runTest {
        enqueue(409, """{"isSuccess":false,"code":"FAMILY409_1","message":"이미 존재하는 가족 이름입니다.","result":null}""")

        val error = repository().createFamily("끼리네가족").exceptionOrNull()

        assertTrue(error is DuplicateFamilyNameException)
    }

    @Test
    fun 이름_규칙에_맞지_않으면_서버의_code를_담아_실패한다() = runTest {
        enqueue(400, """{"isSuccess":false,"code":"COMMON400_1","message":"잘못된 요청입니다.","result":{"name":"가족 이름은 필수입니다."}}""")

        val error = repository().createFamily("").exceptionOrNull()

        assertEquals("COMMON400_1", (error as KiriApiException).code)
    }

    @Test
    fun 연결이_끊기면_네트워크_오류로_실패한다() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().createFamily("끼리네가족").exceptionOrNull()

        assertTrue(error is IOException)
    }

    private companion object {
        const val SUCCESS_BODY = """{"isSuccess":true,"code":"FAMILY201_1","message":"가족 공간이 생성되었습니다.",""" +
            """"result":{"familyId":1,"name":"끼리네가족","inviteCode":"NWTKYJ","inviteUrl":"https://kiri.app/invite/NWTKYJ",""" +
            """"inviteExpiresAt":"2026-10-14T11:51:36"}}"""
    }
}
