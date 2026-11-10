package com.kiri.data.taste

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.taste.MyTaste
import com.kiri.domain.model.taste.Taste
import com.kiri.domain.model.taste.TasteActivity
import com.kiri.domain.model.taste.TasteCategory
import com.kiri.domain.model.taste.TasteDuration
import com.kiri.domain.model.taste.TastePrice
import com.kiri.domain.model.taste.TasteSpace
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
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

class TasteRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): TasteRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TasteApi::class.java)
        return TasteRepositoryImpl(api, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    private val savedTaste = Taste(
        categories = setOf(TasteCategory.EXHIBITION, TasteCategory.FOOD_CAFE),
        activity = TasteActivity.LIGHT,
        space = TasteSpace.INDOOR,
        price = TastePrice.FREE,
        duration = TasteDuration.WITHIN_1H,
    )

    @Test fun completionFlagSurvivesUnknownActivityValue() {
        val taste = TasteResponse(isSet = true, activityLevel = "NEW_ACTIVITY").toMyTaste()
        assertTrue(taste.isSet)
        assertNull(taste.taste)
    }

    @Test
    fun 내_취향을_GET으로_불러온다() = runTest {
        enqueue(200, SET_BODY)

        val myTaste = repository().getMyTaste().getOrThrow()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/members/taste", request.url.encodedPath)
        assertEquals(MyTaste(nickname = "쥬123", taste = savedTaste), myTaste)
    }

    @Test
    fun 취향을_정한_적이_없으면_닉네임만_돌려준다() = runTest {
        enqueue(200, """{"isSuccess":true,"code":"TASTE200_1","message":"내 취향 조회 성공",""" +
            """"result":{"nickname":"쥬123","isSet":false,"categories":[],"indoorOutdoor":null,""" +
            """"activityLevel":null,"priceLevel":null,"durationLevel":null}}""")

        assertEquals(MyTaste(nickname = "쥬123", taste = null), repository().getMyTaste().getOrThrow())
    }

    @Test
    fun 상관없음으로_저장된_항목은_비어_있다() = runTest {
        enqueue(200, """{"isSuccess":true,"code":"TASTE200_1","message":"내 취향 조회 성공",""" +
            """"result":{"nickname":"쥬123","isSet":true,"categories":["OUTDOOR"],"indoorOutdoor":null,""" +
            """"activityLevel":"ACTIVE","priceLevel":null,"durationLevel":null}}""")

        val taste = repository().getMyTaste().getOrThrow().taste!!

        assertEquals(setOf(TasteCategory.OUTDOOR), taste.categories)
        assertEquals(TasteActivity.ACTIVE, taste.activity)
        assertNull(taste.space)
        assertNull(taste.price)
        assertNull(taste.duration)
    }

    @Test
    fun 앱이_모르는_값은_고르지_않은_것으로_본다() = runTest {
        enqueue(200, SET_BODY.replace("\"FOOD_CAFE\"", "\"SPORTS\"").replace("\"INDOOR\"", "\"ROOFTOP\""))

        val taste = repository().getMyTaste().getOrThrow().taste!!

        assertEquals(setOf(TasteCategory.EXHIBITION), taste.categories)
        assertNull(taste.space)
    }

    @Test
    fun 내_취향을_PUT으로_저장하고_상관없음은_보내지_않는다() = runTest {
        enqueue(200, SET_BODY)

        repository().saveMyTaste(
            Taste(categories = setOf(TasteCategory.POPUP), activity = TasteActivity.MODERATE, duration = TasteDuration.H1_TO_3),
        )

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/members/taste", request.url.encodedPath)
        val body = kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals(listOf("POPUP"), body["categories"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals("MODERATE", body["activityLevel"]!!.jsonPrimitive.content)
        assertEquals("H1_TO_3", body["durationLevel"]!!.jsonPrimitive.content)
        assertFalse("indoorOutdoor" in body)
        assertFalse("priceLevel" in body)
    }

    @Test
    fun 저장하면_저장된_취향을_돌려준다() = runTest {
        enqueue(200, SET_BODY.replace("TASTE200_1", "TASTE200_2"))

        assertEquals(MyTaste(nickname = "쥬123", taste = savedTaste), repository().saveMyTaste(savedTaste).getOrThrow())
    }

    @Test
    fun 규칙에_맞지_않으면_서버의_code를_담아_실패한다() = runTest {
        enqueue(400, """{"isSuccess":false,"code":"COMMON400_1","message":"잘못된 요청입니다.",""" +
            """"result":{"categories":"선호 활동은 1~3개 선택해야 합니다."}}""")

        val error = repository().saveMyTaste(savedTaste).exceptionOrNull()

        assertEquals("COMMON400_1", (error as KiriApiException).code)
    }

    @Test
    fun 연결이_끊기면_네트워크_오류로_실패한다() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().getMyTaste().exceptionOrNull()

        assertTrue(error is IOException)
    }

    private companion object {
        const val SET_BODY = """{"isSuccess":true,"code":"TASTE200_1","message":"내 취향 조회 성공",""" +
            """"result":{"nickname":"쥬123","isSet":true,"categories":["EXHIBITION","FOOD_CAFE"],""" +
            """"indoorOutdoor":"INDOOR","activityLevel":"LIGHT","priceLevel":"FREE","durationLevel":"WITHIN_1H"}}"""
    }
}
