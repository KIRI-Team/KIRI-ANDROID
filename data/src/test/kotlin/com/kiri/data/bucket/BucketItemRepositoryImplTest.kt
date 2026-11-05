package com.kiri.data.bucket

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.bucket.BucketItemNotFoundException
import com.kiri.domain.model.bucket.BucketItemPage
import com.kiri.domain.model.bucket.CompletedBucketItemException
import com.kiri.domain.model.bucket.NotBucketItemOwnerException
import com.kiri.domain.model.bucket.ScheduledBucketItemException
import com.kiri.domain.model.bucket.BucketItemStatus
import com.kiri.domain.model.bucket.BucketSaver
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
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

class BucketItemRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): BucketItemRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BucketItemApi::class.java)
        return BucketItemRepositoryImpl(api, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun 필터와_페이지_값을_쿼리로_보내고_없는_값은_빼고_보낸다() = runTest {
        enqueue(200, SUCCESS)
        enqueue(200, SUCCESS)

        repository().getBucketItems(memberId = 2, cursor = 31, size = 20)
        repository().getBucketItems()

        val filtered = server.takeRequest()
        assertEquals("GET", filtered.method)
        assertEquals("/api/v1/bucket-items", filtered.url.encodedPath)
        assertEquals("2", filtered.url.queryParameter("memberId"))
        assertEquals("31", filtered.url.queryParameter("cursor"))
        assertEquals("20", filtered.url.queryParameter("size"))
        val all = server.takeRequest()
        assertNull(all.url.queryParameter("memberId"))
        assertNull(all.url.queryParameter("cursor"))
        assertNull(all.url.queryParameter("size"))
    }

    @Test
    fun 응답을_구성원_칩과_버킷_항목_다음_페이지_정보로_바꾼다() = runTest {
        enqueue(200, SUCCESS)

        val page = repository().getBucketItems().getOrThrow()

        assertEquals(listOf(BucketSaver(1, "끼리엄마", 1), BucketSaver(2, "끼리아빠", 2)), page.savers)
        assertEquals(31L, page.nextCursor)
        assertTrue(page.hasNext)
        val saved = page.items[0]
        assertEquals(42L, saved.id)
        assertEquals(BucketItemStatus.SAVED, saved.status)
        assertEquals(LocalDateTime.of(2026, 10, 27, 19, 40, 12, 345_000_000), saved.savedAt)
        assertEquals(130L, saved.missionId)
        assertEquals("광장시장에서 시장음식 먹어보기", saved.title)
        assertEquals("ACTIVE", saved.missionStatus)
        assertEquals(2L, saved.saverId)
        assertEquals("끼리아빠", saved.saverNickname)
        assertTrue(saved.schedulable)
        assertTrue(saved.deletable)
        assertNull(saved.scheduleId)
        // 탈퇴한 회원이 저장한 종료된 미션도 그대로 온다
        val withdrawn = page.items[1]
        assertTrue(withdrawn.saverWithdrawn)
        assertEquals("탈퇴한 사용자", withdrawn.saverNickname)
        assertEquals("ENDED", withdrawn.missionStatus)
        assertFalse(withdrawn.schedulable)
        assertFalse(withdrawn.deletable)
    }

    @Test
    fun 앱이_모르는_상태나_날짜는_비워_두고_항목은_보여준다() = runTest {
        enqueue(
            200,
            """{"isSuccess":true,"code":"BUCKET200_1","message":"","result":{"savers":[],"bucketItems":[""" +
                """{"bucketItemId":7,"status":"ARCHIVED","savedAt":"어제","mission":{"missionId":1,"title":"제목"},""" +
                """"saver":{"memberId":1,"nickname":"엄마"},"scheduleId":5,"scheduledDate":"2026-11-20"}""" +
                """],"nextCursor":null,"hasNext":false}}""",
        )

        val item = repository().getBucketItems().getOrThrow().items.single()

        assertNull(item.status)
        assertNull(item.savedAt)
        assertEquals(5L, item.scheduleId)
        assertEquals(java.time.LocalDate.of(2026, 11, 20), item.scheduledDate)
    }

    @Test
    fun 가족_공간이_없으면_빈_페이지를_돌려준다() = runTest {
        enqueue(403, """{"isSuccess":false,"code":"BUCKET403_1","message":"가족공간에 소속된 회원만 이용할 수 있습니다.","result":null}""")

        assertEquals(BucketItemPage.Empty, repository().getBucketItems().getOrThrow())
    }

    @Test
    fun 그_밖의_오류는_그대로_실패한다() = runTest {
        enqueue(400, """{"isSuccess":false,"code":"COMMON400_1","message":"잘못된 요청입니다.","result":{"size":"size는 1 이상 50 이하여야 합니다."}}""")

        val error = repository().getBucketItems(size = 100).exceptionOrNull()

        assertEquals("COMMON400_1", (error as KiriApiException).code)
    }

    @Test
    fun 버킷_삭제는_DELETE_로_보내고_결과가_없어도_성공이다() = runTest {
        enqueue(200, """{"isSuccess":true,"code":"BUCKET200_2","message":"버킷리스트에서 삭제했습니다.","result":null}""")

        val result = repository().deleteBucketItem(42)

        assertTrue(result.isSuccess)
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/bucket-items/42", request.url.encodedPath)
    }

    @Test
    fun 버킷_삭제_실패_코드를_도메인_오류로_바꾼다() = runTest {
        fun failure(status: Int, code: String) =
            enqueue(status, """{"isSuccess":false,"code":"$code","message":"실패","result":null}""")

        failure(404, "BUCKET404_1")
        assertTrue(repository().deleteBucketItem(1).exceptionOrNull() is BucketItemNotFoundException)
        failure(403, "BUCKET403_2")
        assertTrue(repository().deleteBucketItem(1).exceptionOrNull() is NotBucketItemOwnerException)
        failure(409, "BUCKET409_3")
        assertTrue(repository().deleteBucketItem(1).exceptionOrNull() is ScheduledBucketItemException)
        failure(409, "BUCKET409_4")
        assertTrue(repository().deleteBucketItem(1).exceptionOrNull() is CompletedBucketItemException)
        failure(404, "MEMBER404_1")
        assertEquals("MEMBER404_1", (repository().deleteBucketItem(1).exceptionOrNull() as KiriApiException).code)
    }

    private companion object {
        val SUCCESS = """
            {"isSuccess":true,"code":"BUCKET200_1","message":"가족 버킷리스트를 조회했습니다.","result":{
              "savers":[
                {"memberId":1,"nickname":"끼리엄마","familyRole":"MOTHER","bucketCount":1},
                {"memberId":2,"nickname":"끼리아빠","familyRole":"FATHER","bucketCount":2}],
              "bucketItems":[
                {"bucketItemId":42,"status":"SAVED","savedAt":"2026-10-27T19:40:12.345","completedAt":null,
                 "mission":{"missionId":130,"title":"광장시장에서 시장음식 먹어보기","imageUrl":"https://example.com/a.jpg","imageSource":"한국관광공사","status":"ACTIVE"},
                 "saver":{"memberId":2,"nickname":"끼리아빠","familyRole":"FATHER","withdrawn":false},
                 "schedulable":true,"deletable":true,"scheduleId":null,"scheduledDate":null},
                {"bucketItemId":31,"status":"SAVED","savedAt":"2026-10-20T10:05:00","completedAt":null,
                 "mission":{"missionId":98,"title":"한강 뚜벅뚜벅 축제 방문하기","imageUrl":null,"imageSource":null,"status":"ENDED"},
                 "saver":{"memberId":4,"nickname":"탈퇴한 사용자","familyRole":null,"withdrawn":true},
                 "schedulable":false,"deletable":false,"scheduleId":null,"scheduledDate":null}],
              "nextCursor":31,"hasNext":true}}
        """.trimIndent()
    }
}
