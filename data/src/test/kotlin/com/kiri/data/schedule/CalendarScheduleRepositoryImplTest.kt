package com.kiri.data.schedule

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.home.ScheduleStatus
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.CancelledScheduleException
import com.kiri.domain.model.schedule.MyScheduleStatus
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.model.schedule.ScheduleParticipant
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
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

class CalendarScheduleRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): CalendarScheduleRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CalendarScheduleApi::class.java)
        return CalendarScheduleRepositoryImpl(api, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun 월간_일정을_year와_month_쿼리로_GET_요청한다() = runTest {
        enqueue(200, body(""))

        repository().getMonthlySchedules(YearMonth.of(2026, 10))

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/schedules/calendar", request.url.encodedPath)
        assertEquals("2026", request.url.queryParameter("year"))
        assertEquals("10", request.url.queryParameter("month"))
    }

    @Test
    fun 초대받지_않은_일정과_취소된_일정도_그대로_돌려준다() = runTest {
        enqueue(200, body(SCHEDULES))

        val schedules = repository().getMonthlySchedules(YearMonth.of(2026, 10)).getOrThrow()

        assertEquals(
            listOf(
                CalendarSchedule(100, LocalDate.of(2026, 10, 3), LocalTime.of(14, 50), ScheduleStatus.CONFIRMED, MyScheduleStatus.ACCEPTED),
                CalendarSchedule(101, LocalDate.of(2026, 10, 24), LocalTime.of(10, 0), ScheduleStatus.PENDING_RESPONSE, MyScheduleStatus.NOT_INVITED),
                CalendarSchedule(102, LocalDate.of(2026, 10, 25), LocalTime.of(9, 0), ScheduleStatus.CANCELLED, MyScheduleStatus.DECLINED),
            ),
            schedules,
        )
    }

    @Test
    fun 상태나_날짜를_읽지_못한_일정만_빼고_나머지는_보여준다() = runTest {
        enqueue(
            200,
            body(
                """{"scheduleId":1,"date":"2026-10-03","time":"14:50","status":"POSTPONED","myStatus":"ACCEPTED"},""" +
                    """{"scheduleId":2,"date":"10월 4일","time":"14:50","status":"CONFIRMED","myStatus":"ACCEPTED"},""" +
                    """{"scheduleId":3,"date":"2026-10-05","time":"오후","status":"CONFIRMED","myStatus":"MAYBE"}""",
            ),
        )

        val schedules = repository().getMonthlySchedules(YearMonth.of(2026, 10)).getOrThrow()

        assertEquals(listOf(3L), schedules.map { it.id })
        assertNull(schedules.single().time)
        assertNull(schedules.single().myStatus)
    }

    @Test
    fun 가족_공간이_없으면_빈_목록을_돌려준다() = runTest {
        enqueue(403, """{"isSuccess":false,"code":"SCHEDULE403_1","message":"가족 공간이 없습니다.","result":null}""")

        assertEquals(emptyList<CalendarSchedule>(), repository().getMonthlySchedules(YearMonth.of(2026, 10)).getOrThrow())
    }

    @Test
    fun 잘못된_요청이면_서버의_code를_담아_실패한다() = runTest {
        enqueue(
            400,
            """{"isSuccess":false,"code":"COMMON400_1","message":"잘못된 요청입니다.",""" +
                """"result":{"month":"month는 1 이상 12 이하여야 합니다."}}""",
        )

        val error = repository().getMonthlySchedules(YearMonth.of(2026, 10)).exceptionOrNull()

        assertEquals("COMMON400_1", (error as KiriApiException).code)
    }

    @Test
    fun 연결이_끊기면_네트워크_오류로_실패한다() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())

        val error = repository().getMonthlySchedules(YearMonth.of(2026, 10)).exceptionOrNull()

        assertTrue(error is IOException)
    }

    @Test
    fun 일정_상세를_scheduleId_경로로_GET_요청하고_참여자를_순서대로_돌려준다() = runTest {
        enqueue(200, detailBody("CONFIRMED"))

        val detail = repository().getScheduleDetail(100).getOrThrow()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/schedules/100", request.url.encodedPath)
        assertEquals(
            ScheduleDetail(
                date = LocalDate.of(2026, 11, 17),
                time = LocalTime.of(14, 50),
                status = ScheduleDisplayStatus.CONFIRMED,
                participants = listOf(
                    ScheduleParticipant("엄마", accepted = true),
                    ScheduleParticipant("탈퇴한 사용자", accepted = true),
                    ScheduleParticipant("첫째", accepted = false),
                ),
            ),
            detail,
        )
    }

    @Test
    fun 상세의_당일_상태와_앱이_모르는_상태를_구분한다() = runTest {
        enqueue(200, detailBody("TODAY"))
        enqueue(200, detailBody("POSTPONED"))

        assertEquals(ScheduleDisplayStatus.TODAY, repository().getScheduleDetail(1).getOrThrow().status)
        assertNull(repository().getScheduleDetail(1).getOrThrow().status)
    }

    @Test
    fun 상세의_날짜를_읽지_못하면_실패한다() = runTest {
        enqueue(200, detailBody("CONFIRMED").replace("2026-11-17", "11월 17일"))

        assertTrue(repository().getScheduleDetail(1).isFailure)
    }

    @Test
    fun 상세_조회의_서버_오류를_도메인_오류로_바꾼다() = runTest {
        enqueue(403, errorBody("SCHEDULE403_1", "가족 공간이 없습니다."))
        enqueue(404, errorBody("SCHEDULE404_2", "존재하지 않는 일정입니다."))
        enqueue(409, errorBody("SCHEDULE409_7", "취소된 일정입니다."))
        enqueue(404, errorBody("MEMBER404_1", "존재하지 않는 회원입니다."))

        assertTrue(repository().getScheduleDetail(1).exceptionOrNull() is NoFamilyException)
        assertTrue(repository().getScheduleDetail(1).exceptionOrNull() is ScheduleNotFoundException)
        assertTrue(repository().getScheduleDetail(1).exceptionOrNull() is CancelledScheduleException)
        assertEquals("MEMBER404_1", (repository().getScheduleDetail(1).exceptionOrNull() as KiriApiException).code)
    }

    private fun errorBody(code: String, message: String) =
        """{"isSuccess":false,"code":"$code","message":"$message","result":null}"""

    private fun detailBody(status: String) =
        """{"isSuccess":true,"code":"SCHEDULE200_9","message":"일정 상세 조회 성공",""" +
            """"result":{"date":"2026-11-17","time":"14:50","status":"$status","participants":[""" +
            """{"nickname":"엄마","accepted":true},{"nickname":"탈퇴한 사용자","accepted":true},""" +
            """{"nickname":"첫째","accepted":false}]}}"""

    private fun body(schedules: String) =
        """{"isSuccess":true,"code":"SCHEDULE200_1","message":"캘린더 월간 일정 조회 성공",""" +
            """"result":{"year":2026,"month":10,"schedules":[$schedules]}}"""

    private companion object {
        const val SCHEDULES =
            """{"scheduleId":100,"date":"2026-10-03","time":"14:50","status":"CONFIRMED","myStatus":"ACCEPTED"},""" +
                """{"scheduleId":101,"date":"2026-10-24","time":"10:00","status":"PENDING_RESPONSE","myStatus":"NOT_INVITED"},""" +
                """{"scheduleId":102,"date":"2026-10-25","time":"09:00","status":"CANCELLED","myStatus":"DECLINED"}"""
    }
}
