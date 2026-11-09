package com.kiri.data.family

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.invite.Invite
import com.kiri.domain.model.invite.InviteReissueFailedException
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
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

class RemoteInviteRepositoryTest {

    private val server = MockWebServer()
    private val familyIdStore = FakeFamilyIdStore(saved = 7)

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): RemoteInviteRepository {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FamilyApi::class.java)
        return RemoteInviteRepository(api, familyIdStore, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun 기억해_둔_가족_ID로_지금_초대를_불러온다() = runTest {
        enqueue(200, SUCCESS_BODY)

        val invite = repository().getCurrentInvite().getOrThrow()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/families/7/invite", request.url.encodedPath)
        assertEquals(
            Invite(code = "NWTKYJ", url = "https://kiri.app/invite/NWTKYJ", expiresAt = LocalDate.of(2026, 10, 14)),
            invite,
        )
    }

    @Test
    fun 기억한_가족_ID가_없으면_가족_ID_조회로_받아_기억하고_초대를_불러온다() = runTest {
        familyIdStore.saved = null
        enqueue(200, """{"isSuccess":true,"code":"FAMILY200_6","message":"가족 ID 조회에 성공했습니다.","result":{"familyId":9}}""")
        enqueue(200, SUCCESS_BODY)

        repository().getCurrentInvite().getOrThrow()

        assertEquals("/api/v1/families/me/id", server.takeRequest().url.encodedPath)
        assertEquals("/api/v1/families/9/invite", server.takeRequest().url.encodedPath)
        // 다음부터는 다시 조회하지 않는다
        assertEquals(9L, familyIdStore.saved)
    }

    @Test
    fun 소속된_가족이_없으면_초대를_요청하지_않고_가족_정보_없음으로_실패한다() = runTest {
        familyIdStore.saved = null
        enqueue(404, """{"isSuccess":false,"code":"FAMILY404_1","message":"가족이 존재하지 않습니다.","result":null}""")

        val error = repository().getCurrentInvite().exceptionOrNull()

        assertTrue(error is NoFamilyException)
        assertEquals(1, server.requestCount)
        assertEquals(null, familyIdStore.saved)
    }

    @Test
    fun 가족_구성원이_아니면_서버의_code를_담아_실패한다() = runTest {
        // 명세상 403 응답의 isSuccess 가 true 로 오지만 HTTP 상태로 실패를 판단한다
        enqueue(403, """{"isSuccess":true,"code":"FAMILY403_1","message":"해당 가족에 대한 권한이 없습니다.","result":null}""")

        val error = repository().getCurrentInvite().exceptionOrNull()

        assertEquals("FAMILY403_1", (error as KiriApiException).code)
    }

    @Test
    fun 연결이_끊기면_네트워크_오류로_실패한다() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().getCurrentInvite().exceptionOrNull()

        assertTrue(error is IOException)
    }

    @Test
    fun 재발급하면_지금_초대를_DELETE로_지운_뒤_PATCH로_새_초대를_받는다() = runTest {
        enqueue(200, DELETE_BODY)
        enqueue(200, REISSUE_BODY)

        val invite = repository().createInvite().getOrThrow()

        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/families/7/invite", delete.url.encodedPath)
        val reissue = server.takeRequest()
        assertEquals("PATCH", reissue.method)
        assertEquals("/api/v1/families/7/invite", reissue.url.encodedPath)
        assertEquals(
            Invite(code = "NEW123", url = "https://kiri.app/invite/NEW123", expiresAt = LocalDate.of(2026, 10, 21)),
            invite,
        )
    }

    @Test
    fun 지우지_못하면_재발급을_요청하지_않고_서버의_code를_담아_실패한다() = runTest {
        enqueue(403, """{"isSuccess":true,"code":"FAMILY403_1","message":"해당 가족에 대한 권한이 없습니다.","result":null}""")

        val error = repository().createInvite().exceptionOrNull()

        assertEquals("FAMILY403_1", (error as KiriApiException).code)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun 지운_뒤_재발급하지_못하면_재발급_실패로_알린다() = runTest {
        enqueue(200, DELETE_BODY)
        enqueue(500, """{"isSuccess":false,"code":"COMMON500_1","message":"서버 내부 오류가 발생했습니다.","result":null}""")

        val error = repository().createInvite().exceptionOrNull()

        assertTrue(error is InviteReissueFailedException)
        assertEquals("COMMON500_1", (error!!.cause as KiriApiException).code)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun 소속된_가족이_없으면_재발급을_요청하지_않고_가족_정보_없음으로_실패한다() = runTest {
        familyIdStore.saved = null
        enqueue(404, """{"isSuccess":false,"code":"FAMILY404_1","message":"가족이 존재하지 않습니다.","result":null}""")

        val error = repository().createInvite().exceptionOrNull()

        assertTrue(error is NoFamilyException)
        // 가족 ID 조회만 하고 초대 삭제·재발급은 요청하지 않는다
        assertEquals(1, server.requestCount)
    }

    @Test
    fun 재발급_중_연결이_끊기면_네트워크_오류로_실패한다() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().createInvite().exceptionOrNull()

        assertTrue(error is IOException)
    }

    private companion object {
        const val DELETE_BODY = """{"isSuccess":true,"code":"FAMILY200_3","message":"가족 초대 정보가 삭제되었습니다.","result":null}"""
        const val REISSUE_BODY = """{"isSuccess":true,"code":"FAMILY200_1","message":"가족 초대 정보가 생성되었습니다.",""" +
            """"result":{"inviteCode":"NEW123","inviteUrl":"https://kiri.app/invite/NEW123","inviteExpiresAt":"2026-10-21T09:00:00"}}"""
        const val SUCCESS_BODY = """{"isSuccess":true,"code":"FAMILY200_2","message":"가족 초대 정보를 조회했습니다.",""" +
            """"result":{"inviteCode":"NWTKYJ","inviteUrl":"https://kiri.app/invite/NWTKYJ","inviteExpiresAt":"2026-10-14T11:51:36"}}"""
    }
}
