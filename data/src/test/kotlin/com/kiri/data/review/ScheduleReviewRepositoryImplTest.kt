package com.kiri.data.review

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.review.AlreadyReviewedException
import com.kiri.domain.model.review.InvalidReviewException
import com.kiri.domain.model.member.FamilyRole
import com.kiri.domain.model.review.NotScheduleParticipantException
import com.kiri.domain.model.review.ReviewAccessDeniedException
import com.kiri.domain.model.review.ReviewWriter
import com.kiri.domain.model.review.ScheduleReview
import com.kiri.domain.model.review.ScheduleReviewForm
import com.kiri.domain.model.review.ScheduleReviewResult
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import java.time.LocalDateTime
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

    @Test
    fun 같은_경로로_GET_하고_리뷰를_작성순_그대로_돌려준다() = runTest {
        enqueue(200, LIST)

        val reviews = repository().getReviews(10).getOrThrow()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/schedules/10/reviews", request.url.encodedPath)
        assertEquals(
            listOf(
                ScheduleReview(
                    1, ReviewWriter(5, "김주연", FamilyRole.DAUGHTER), "아빠가 자주 가는 식당에 처음 가봤다.", 5, true,
                    listOf("https://cdn.example.com/review/1/image-1.jpg", "https://cdn.example.com/review/1/image-2.jpg"),
                    LocalDateTime.of(2026, 6, 13, 14, 50), isMine = true,
                    photoKeys = mapOf(
                        "https://cdn.example.com/review/1/image-1.jpg" to "user/original/5/image-1.jpg",
                        "https://cdn.example.com/review/1/image-2.jpg" to "user/original/5/image-2.jpg",
                    ),
                    photoIds = mapOf("https://cdn.example.com/review/1/image-1.jpg" to 1L, "https://cdn.example.com/review/1/image-2.jpg" to 2L),
                ),
                // 모르는 역할·읽지 못한 날짜는 null, 빈 사진 주소는 뺀다
                ScheduleReview(2, ReviewWriter(6, "아빠", null), "또 가요", 4, false, emptyList(), null, isMine = false),
            ),
            reviews,
        )
    }

    @Test
    fun 리뷰가_없으면_빈_목록이고_권한이_없거나_없는_일정이면_도메인_오류로_바꾼다() = runTest {
        enqueue(200, """{"isSuccess":true,"code":"REVIEW200_2","message":"리뷰 목록 조회가 완료되었습니다.","result":{"scheduleId":3,"reviews":[]}}""")
        enqueue(403, failure("REVIEW403_2", "해당 일정의 리뷰를 조회할 권한이 없습니다."))
        enqueue(404, failure("SCHEDULE404_2", "존재하지 않는 일정입니다."))
        enqueue(404, failure("MEMBER404_1", "존재하지 않는 회원입니다."))

        val repo = repository()
        assertEquals(emptyList<ScheduleReview>(), repo.getReviews(3).getOrThrow())
        assertTrue(repo.getReviews(3).exceptionOrNull() is ReviewAccessDeniedException)
        assertTrue(repo.getReviews(3).exceptionOrNull() is ScheduleNotFoundException)
        assertEquals("MEMBER404_1", (repo.getReviews(3).exceptionOrNull() as KiriApiException).code)
    }

    @Test
    fun 리뷰_수정은_리뷰_경로로_PUT_하고_최종_상태를_통째로_보낸다() = runTest {
        enqueue(200, """{"isSuccess":true,"code":"REVIEW200_3","message":"리뷰 수정이 완료되었습니다.","result":{"reviewId":3,"scheduleId":10}}""")

        val result = repository().updateReview(10, 3,
            ScheduleReviewForm(5, " 다시 갈래요 ", false, listOf("pending/new.jpg", "user/original/b.jpg"))).getOrThrow()

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/schedules/10/reviews/3", request.url.encodedPath)
        val body = kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals(5, body["starRating"]!!.jsonPrimitive.int)
        assertEquals("다시 갈래요", body["content"]!!.jsonPrimitive.content)
        assertEquals(false, body["isRejoinIntended"]!!.jsonPrimitive.boolean)
        assertEquals(listOf("pending/new.jpg", "user/original/b.jpg"), body["imageKeys"]!!.jsonArray.map { it.jsonPrimitive.content })
        assertEquals(ScheduleReviewResult(3, 10), result)
    }

    @Test
    fun 리뷰_수정_실패_코드를_도메인_오류로_바꾼다() = runTest {
        fun failure(code: String) = """{"isSuccess":false,"code":"$code","message":"별점을 입력해주세요.","result":null}"""
        enqueue(404, failure("REVIEW404_1"))
        enqueue(403, failure("REVIEW403_2"))
        enqueue(403, failure("REVIEW403_3"))
        enqueue(400, failure("COMMON400_1"))
        enqueue(500, failure("IMAGE500_1"))
        val form = ScheduleReviewForm(5, "내용", true, listOf("user/original/a.jpg"))

        val repo = repository()
        assertTrue(repo.updateReview(10, 3, form).exceptionOrNull() is com.kiri.domain.model.review.ReviewNotFoundException)
        assertTrue(repo.updateReview(10, 3, form).exceptionOrNull() is ReviewAccessDeniedException)
        assertTrue(repo.updateReview(10, 3, form).exceptionOrNull() is ReviewAccessDeniedException)
        assertEquals("별점을 입력해주세요.", (repo.updateReview(10, 3, form).exceptionOrNull() as InvalidReviewException).message)
        assertEquals("IMAGE500_1", (repo.updateReview(10, 3, form).exceptionOrNull() as KiriApiException).code)
    }

    @Test
    fun 리뷰_삭제는_리뷰_경로로_DELETE_하고_실패_코드를_도메인_오류로_바꾼다() = runTest {
        enqueue(200, """{"isSuccess":true,"code":"REVIEW200_4","message":"리뷰 삭제가 완료되었습니다.","result":null}""")
        fun failure(code: String) = """{"isSuccess":false,"code":"$code","message":"실패","result":null}"""
        enqueue(404, failure("REVIEW404_1"))
        enqueue(403, failure("REVIEW403_2"))
        enqueue(500, failure("COMMON500_1"))

        val repo = repository()
        repo.deleteReview(10, 3).getOrThrow()
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/schedules/10/reviews/3", request.url.encodedPath)

        assertTrue(repo.deleteReview(10, 3).exceptionOrNull() is com.kiri.domain.model.review.ReviewNotFoundException)
        assertTrue(repo.deleteReview(10, 3).exceptionOrNull() is ReviewAccessDeniedException)
        assertEquals("COMMON500_1", (repo.deleteReview(10, 3).exceptionOrNull() as KiriApiException).code)
    }

    @Test
    fun 대표_사진은_일정_경로로_PATCH_하고_정해진_사진_id를_돌려준다() = runTest {
        enqueue(200, """{"isSuccess":true,"code":"REVIEW200_5","message":"대표사진 변경이 완료되었습니다.","result":{"scheduleId":10,"representativePhotoId":3}}""")
        fun failure(code: String) = """{"isSuccess":false,"code":"$code","message":"실패","result":null}"""
        enqueue(404, failure("REVIEW404_2"))
        enqueue(403, failure("REVIEW403_2"))
        enqueue(404, failure("SCHEDULE404_2"))
        enqueue(500, failure("COMMON500_1"))

        val repo = repository()
        assertEquals(3L, repo.changeRepresentativePhoto(10, 3).getOrThrow())
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/schedules/10/representative-photo", request.url.encodedPath)
        assertEquals(3, kiriJson.parseToJsonElement(request.body!!.utf8()).jsonObject["reviewPhotoId"]!!.jsonPrimitive.int)

        assertTrue(repo.changeRepresentativePhoto(10, 3).exceptionOrNull() is com.kiri.domain.model.review.ReviewPhotoNotFoundException)
        assertTrue(repo.changeRepresentativePhoto(10, 3).exceptionOrNull() is ReviewAccessDeniedException)
        assertTrue(repo.changeRepresentativePhoto(10, 3).exceptionOrNull() is ScheduleNotFoundException)
        assertEquals("COMMON500_1", (repo.changeRepresentativePhoto(10, 3).exceptionOrNull() as KiriApiException).code)
    }

    @Test
    fun 목록_조회는_일정의_대표_사진_id를_함께_돌려주고_없으면_null이다() = runTest {
        enqueue(200, """{"isSuccess":true,"code":"REVIEW200_2","message":"ok","result":{"scheduleId":10,"representativePhotoId":2,"reviews":[
            {"reviewId":1,"writer":{"memberId":5,"nickname":"나"},"photos":[
              {"reviewPhotoId":1,"imageKey":"user/original/a.jpg","imageUrl":"https://cdn/a.jpg"},
              {"reviewPhotoId":2,"imageKey":"user/original/b.jpg","imageUrl":"https://cdn/b.jpg"}]}]}}""")
        enqueue(200, """{"isSuccess":true,"code":"REVIEW200_2","message":"ok","result":{"scheduleId":10,"representativePhotoId":null,"reviews":[]}}""")

        val repo = repository()
        val list = repo.getReviewList(10).getOrThrow()
        assertEquals(2L, list.representativePhotoId)
        assertEquals(1, list.reviews.size)
        assertEquals(null, repo.getReviewList(10).getOrThrow().representativePhotoId)
    }

    @Test
    fun 리뷰_사진은_표시_순서대로_주소와_수정용_Key를_함께_읽는다() = runTest {
        enqueue(200, """{"isSuccess":true,"code":"REVIEW200_2","message":"ok","result":{"scheduleId":10,"reviews":[
            {"reviewId":1,"writer":{"memberId":5,"nickname":"나"},"isMine":true,"photos":[
              {"reviewPhotoId":1,"imageKey":"user/original/5/a.jpg","imageUrl":"https://cdn.example.com/user/original/5/a.jpg?Expires=1&Signature=s"},
              {"reviewPhotoId":2,"imageKey":null,"imageUrl":"https://cdn.example.com/b.jpg"},
              {"reviewPhotoId":3,"imageKey":"user/original/5/c.jpg","imageUrl":""}]}]}}""")

        val review = repository().getReviews(10).getOrThrow().single()

        // 주소가 없는 사진은 빼고, Key 가 없는 사진은 그리기만 한다
        assertEquals(listOf("https://cdn.example.com/user/original/5/a.jpg?Expires=1&Signature=s", "https://cdn.example.com/b.jpg"), review.imageUrls)
        assertEquals(mapOf("https://cdn.example.com/user/original/5/a.jpg?Expires=1&Signature=s" to "user/original/5/a.jpg"), review.photoKeys)
        // 대표 사진으로 지정할 때 쓰는 사진 id
        assertEquals(mapOf("https://cdn.example.com/user/original/5/a.jpg?Expires=1&Signature=s" to 1L, "https://cdn.example.com/b.jpg" to 2L), review.photoIds)
    }

    private companion object {
        // 명세의 성공 응답 예시에 앱이 모르는 값을 담은 리뷰를 하나 더했다
        const val LIST = """{
  "isSuccess": true,
  "code": "REVIEW200_2",
  "message": "리뷰 목록 조회가 완료되었습니다.",
  "result": {
    "scheduleId": 10,
    "reviews": [
      {
        "reviewId": 1,
        "writer": { "memberId": 5, "nickname": "김주연", "familyRole": "DAUGHTER" },
        "content": "아빠가 자주 가는 식당에 처음 가봤다.",
        "starRating": 5,
        "isRejoinIntended": true,
        "photos": [
          { "reviewPhotoId": 1, "imageKey": "user/original/5/image-1.jpg", "imageUrl": "https://cdn.example.com/review/1/image-1.jpg" },
          { "reviewPhotoId": 2, "imageKey": "user/original/5/image-2.jpg", "imageUrl": "https://cdn.example.com/review/1/image-2.jpg" }
        ],
        "createdAt": "2026-06-13T14:50:00",
        "isMine": true
      },
      {
        "reviewId": 2,
        "writer": { "memberId": 6, "nickname": "아빠", "familyRole": "GRANDPA" },
        "content": "또 가요",
        "starRating": 4,
        "isRejoinIntended": false,
        "photos": [{ "reviewPhotoId": 3, "imageKey": "user/original/6/x.jpg", "imageUrl": "" }],
        "createdAt": "later",
        "isMine": false
      }
    ]
  }
}"""
    }
}
