package com.kiri.data.review

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.review.AlreadyReviewedException
import com.kiri.domain.model.review.InvalidReviewException
import com.kiri.domain.model.review.NotScheduleParticipantException
import com.kiri.domain.model.review.ScheduleReviewForm
import com.kiri.domain.model.review.ScheduleReviewResult
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

// 리뷰 작성 API 에 명세대로 본문을 보내고, 실패 코드를 도메인 오류로 바꾸는지 확인한다
class ScheduleReviewRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): ScheduleReviewRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ScheduleReviewApi::class.java)
        return ScheduleReviewRepositoryImpl(api, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    private fun failure(code: String, message: String) =
        """{"isSuccess":false,"code":"$code","message":"$message","result":null}"""

    private val form = ScheduleReviewForm(
        starRating = 4,
        content = "  가족과 즐거웠어요  ",
        rejoinIntended = true,
        imageKeys = listOf("reviews/1.jpg", "reviews/2.jpg"),
    )

    @Test
    fun 일정_경로로_POST_하고_별점_이미지키_내용_재참여_의사를_보낸다() = runTest {
        enqueue(
            200,
            """{"isSuccess":true,"code":"REVIEW200_1","message":"리뷰 작성이 완료되었습니다.","result":{"reviewId":1,"scheduleId":7}}""",
        )

        val result = repository().writeReview(7, form).getOrThrow()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/schedules/7/reviews", request.url.encodedPath)
        val body = kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals(4, body["starRating"]!!.jsonPrimitive.int)
        assertEquals(listOf("reviews/1.jpg", "reviews/2.jpg"), body["imageKeys"]!!.jsonArray.map { it.jsonPrimitive.content })
        // 앞뒤 공백은 지워서 보낸다
        assertEquals("가족과 즐거웠어요", body["content"]!!.jsonPrimitive.content)
        assertTrue(body["isRejoinIntended"]!!.jsonPrimitive.boolean)
        assertEquals(ScheduleReviewResult(1, 7), result)
    }

    @Test
    fun 실패_코드를_도메인_오류로_바꾸고_나머지는_그대로_돌려준다() = runTest {
        enqueue(403, """{"isSuccess":true,"code":"REVIEW403_1","message":"해당 일정에 참여한 가족 구성원이 아닙니다.","result":null}""")
        enqueue(409, failure("REVIEW409_1", "이미 리뷰를 작성했습니다."))
        enqueue(404, failure("SCHEDULE404_2", "존재하지 않는 일정입니다."))
        enqueue(400, """{"isSuccess":false,"code":"COMMON400_1","message":"별점을 입력해주세요.","result":{"starRating":"필수"}}""")
        enqueue(500, failure("COMMON500_1", "서버 내부 오류가 발생했습니다."))

        val repo = repository()
        assertTrue(repo.writeReview(1, form).exceptionOrNull() is NotScheduleParticipantException)
        assertTrue(repo.writeReview(1, form).exceptionOrNull() is AlreadyReviewedException)
        assertTrue(repo.writeReview(1, form).exceptionOrNull() is ScheduleNotFoundException)
        val invalid = repo.writeReview(1, form).exceptionOrNull()
        assertTrue(invalid is InvalidReviewException)
        assertEquals("별점을 입력해주세요.", invalid?.message)
        assertEquals("COMMON500_1", (repo.writeReview(1, form).exceptionOrNull() as KiriApiException).code)
    }
}
