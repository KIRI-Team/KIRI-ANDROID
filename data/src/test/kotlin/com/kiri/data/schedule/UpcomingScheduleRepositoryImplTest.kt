package com.kiri.data.schedule

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.schedule.MyScheduleStatus
import com.kiri.domain.model.schedule.UpcomingSchedule
import com.kiri.domain.model.schedule.UpcomingScheduleType
import com.kiri.domain.model.schedule.UpcomingSchedules
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
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

// 다가오는 일정 API 응답을 카드 두 장으로 바꾸고, 가족 공간이 없으면 빈 카드로 보는지 확인한다
class UpcomingScheduleRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): UpcomingScheduleRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CalendarScheduleApi::class.java)
        return UpcomingScheduleRepositoryImpl(api, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    private fun success(result: String) =
        """{"isSuccess":true,"code":"SCHEDULE200_5","message":"다가오는 일정 조회 성공","result":$result}"""

    @Test
    fun 파라미터_없이_GET_요청하고_응답_필요_카드를_먼저_돌려준다() = runTest {
        enqueue(200, SUCCESS)

        val result = repository().getUpcomingSchedules().getOrThrow()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/schedules/upcoming", request.url.encodedPath)
        assertNull(request.url.query)
        val responseRequired = UpcomingSchedule(
            120, LocalDate.of(2026, 11, 20), LocalTime.of(18, 0), UpcomingScheduleType.RESPONSE_REQUIRED, 5,
            "광장시장에서 시장음식 먹어보기", MyScheduleStatus.PENDING,
        )
        val upcoming = UpcomingSchedule(
            118, LocalDate.of(2026, 11, 17), LocalTime.of(14, 50), UpcomingScheduleType.UPCOMING, 2,
            "한강 뚜벅뚜벅 축제 가보기", MyScheduleStatus.ACCEPTED,
        )
        assertEquals(UpcomingSchedules(responseRequired, upcoming), result)
        assertEquals(listOf(responseRequired, upcoming), result.cards)
    }

    @Test
    fun 없는_카드는_null_이고_당일은_TODAY_로_읽는다() = runTest {
        enqueue(
            200,
            success(
                """{"responseRequired":null,"upcoming":{"scheduleId":7,"date":"2026-11-15","time":"10:00",""" +
                    """"status":"CONFIRMED","myStatus":"ACCEPTED","dDay":0,"type":"TODAY","title":"산책"}}""",
            ),
        )

        val result = repository().getUpcomingSchedules().getOrThrow()

        assertNull(result.responseRequired)
        assertEquals(UpcomingScheduleType.TODAY, result.upcoming?.type)
        assertEquals(0, result.upcoming?.dDay)
        assertEquals(listOf(7L), result.cards.map { it.id })
    }

    @Test
    fun 읽지_못한_값은_받은_칸으로_채우고_날짜를_못_읽은_카드만_뺀다() = runTest {
        enqueue(
            200,
            success(
                """{"responseRequired":{"scheduleId":1,"date":"bad","type":"RESPONSE_REQUIRED","title":"x"},""" +
                    """"upcoming":{"scheduleId":2,"date":"2026-11-17","time":"later","myStatus":"NEW","type":"SOON","title":" "}}""",
            ),
        )

        val result = repository().getUpcomingSchedules().getOrThrow()

        assertNull(result.responseRequired)
        val card = result.upcoming!!
        assertNull(card.time)
        assertNull(card.myStatus)
        // 모르는 type 은 받은 칸(다가오는)으로, 비어 있는 제목은 날짜 제목으로 둔다
        assertEquals(UpcomingScheduleType.UPCOMING, card.type)
        assertEquals("11월 17일 약속", card.title)
    }

    @Test
    fun 가족_공간이_없으면_빈_카드이고_그_밖의_실패는_그대로_돌려준다() = runTest {
        enqueue(403, """{"isSuccess":false,"code":"SCHEDULE403_1","message":"가족 공간이 없습니다.","result":null}""")
        enqueue(401, """{"isSuccess":false,"code":"COMMON401_1","message":"인증이 필요합니다.","result":null}""")

        assertEquals(UpcomingSchedules(), repository().getUpcomingSchedules().getOrThrow())
        assertTrue(repository().getUpcomingSchedules().exceptionOrNull() is KiriApiException)
    }

    private companion object {
        // 명세의 성공 응답 예시 (제목만 한글 예시로 바꿨다)
        const val SUCCESS = """{
  "isSuccess": true,
  "code": "SCHEDULE200_5",
  "message": "다가오는 일정 조회 성공",
  "result": {
    "responseRequired": {
      "scheduleId": 120,
      "date": "2026-11-20",
      "time": "18:00",
      "status": "PENDING_RESPONSE",
      "myStatus": "PENDING",
      "dDay": 5,
      "type": "RESPONSE_REQUIRED",
      "title": "광장시장에서 시장음식 먹어보기"
    },
    "upcoming": {
      "scheduleId": 118,
      "date": "2026-11-17",
      "time": "14:50",
      "status": "CONFIRMED",
      "myStatus": "ACCEPTED",
      "dDay": 2,
      "type": "UPCOMING",
      "title": "한강 뚜벅뚜벅 축제 가보기"
    }
  }
}"""
    }
}
