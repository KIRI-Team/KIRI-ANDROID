package com.kiri.data.push.repository

import com.kiri.data.common.network.KiriApiException
import com.kiri.data.common.network.apiBaseUrl
import com.kiri.data.common.network.kiriJson
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
import com.kiri.data.push.remote.DeviceApi

class PushDeviceRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): PushDeviceRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DeviceApi::class.java)
        return PushDeviceRepositoryImpl(api, { DEVICE_ID }, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun FCM_토큰과_기기_종류와_식별자를_POST로_등록한다() = runTest {
        enqueue(200, REGISTER_BODY)

        val result = repository().register("fcm-token-1")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/members/devices", request.url.encodedPath)
        val body = kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("fcm-token-1", body["fcmToken"]!!.jsonPrimitive.content)
        assertEquals("ANDROID", body["deviceType"]!!.jsonPrimitive.content)
        assertEquals(DEVICE_ID, body["deviceIdentifier"]!!.jsonPrimitive.content)
        assertTrue(result.isSuccess)
    }

    @Test
    fun 등록한_식별자로_DELETE해_기기_등록을_지운다() = runTest {
        enqueue(200, DELETE_BODY)

        val result = repository().unregister()

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/members/devices", request.url.encodedPath)
        assertEquals(DEVICE_ID, request.url.queryParameter("deviceIdentifier"))
        assertTrue(result.isSuccess)
    }

    @Test
    fun 등록된_기기가_없으면_서버_코드로_실패한다() = runTest {
        enqueue(404, """{"isSuccess":false,"code":"MEMBER_DEVICE404_1","message":"등록된 기기를 찾을 수 없습니다.","result":null}""")

        val error = repository().unregister().exceptionOrNull()

        assertEquals("MEMBER_DEVICE404_1", (error as KiriApiException).code)
    }

    @Test
    fun 필수값이_빠지면_서버_코드로_실패한다() = runTest {
        enqueue(400, """{"isSuccess":false,"code":"COMMON400_1","message":"잘못된 요청입니다.","result":{"fcmToken":"fcmToken은 필수입니다."}}""")

        val error = repository().register("").exceptionOrNull()

        assertEquals("COMMON400_1", (error as KiriApiException).code)
    }

    @Test
    fun 등록_중_연결이_끊기면_네트워크_오류로_실패한다() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().register("fcm-token-1").exceptionOrNull()

        assertTrue(error is IOException)
    }

    private companion object {
        const val DEVICE_ID = "8f6c2a1e-0d3b-4c5a-9e7f-1234567890ab"
        const val REGISTER_BODY = """{"isSuccess":true,"code":"MEMBER_DEVICE200_1","message":"기기가 성공적으로 등록되었습니다.","result":null}"""
        const val DELETE_BODY = """{"isSuccess":true,"code":"MEMBER_DEVICE200_2","message":"기기가 성공적으로 삭제되었습니다.","result":null}"""
    }
}
