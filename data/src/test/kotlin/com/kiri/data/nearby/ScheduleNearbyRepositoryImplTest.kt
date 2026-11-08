package com.kiri.data.nearby

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.detail.NearbyPlace
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import kotlinx.coroutines.test.runTest
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

// 근처 놀거리 추천 API 응답을 서버 순서 그대로 장소 목록으로 바꾸는지 확인한다
class ScheduleNearbyRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): ScheduleNearbyRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ScheduleNearbyApi::class.java)
        return ScheduleNearbyRepositoryImpl(api, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    private fun success(result: String) =
        """{"isSuccess":true,"code":"SCHEDULE200_8","message":"근처 놀거리 추천 조회 성공","result":$result}"""

    private fun failure(code: String) = """{"isSuccess":false,"code":"$code","message":"실패","result":null}"""

    @Test
    fun 일정_경로로_GET_하고_서버가_정렬한_순서대로_장소를_돌려준다() = runTest {
        enqueue(
            200,
            success(
                """[{"placeName":"세빛섬","region":"서울시 서초구","description":"일정 장소에서 가까운 곳이에요",""" +
                    """"imageUrl":"https://tong.visitkorea.or.kr/cms/resource/00/1234500_image2_1.jpg","price":0},""" +
                    """{"placeName":"반포한강공원","region":"서울시 서초구","description":"잠깐 쉬어가기 좋은 곳이에요",""" +
                    """"imageUrl":null,"price":null}]""",
            ),
        )

        val places = repository().getNearby(7).getOrThrow()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/schedules/7/nearby", request.url.encodedPath)
        assertEquals(
            listOf(
                NearbyPlace(
                    "nearby-0-세빛섬", "세빛섬", "서울시 서초구", "일정 장소에서 가까운 곳이에요",
                    imageUrl = "https://tong.visitkorea.or.kr/cms/resource/00/1234500_image2_1.jpg", priceWon = 0,
                ),
                NearbyPlace("nearby-1-반포한강공원", "반포한강공원", "서울시 서초구", "잠깐 쉬어가기 좋은 곳이에요"),
            ),
            places,
        )
    }

    @Test
    fun 추천할_장소가_없으면_빈_목록이고_빈_값은_비워_둔다() = runTest {
        enqueue(200, success("[]"))
        enqueue(200, success("""[{"placeName":"산책로","imageUrl":" "}]"""))

        assertTrue(repository().getNearby(1).getOrThrow().isEmpty())
        val place = repository().getNearby(1).getOrThrow().single()
        assertEquals("", place.address)
        assertEquals("", place.description)
        assertEquals(null, place.imageUrl)
    }

    @Test
    fun 실패_코드를_도메인_오류로_바꾸고_나머지는_그대로_돌려준다() = runTest {
        enqueue(403, failure("SCHEDULE403_1"))
        enqueue(404, failure("SCHEDULE404_2"))
        enqueue(404, failure("MEMBER404_1"))

        val repo = repository()
        assertTrue(repo.getNearby(1).exceptionOrNull() is NoFamilyException)
        assertTrue(repo.getNearby(1).exceptionOrNull() is ScheduleNotFoundException)
        assertEquals("MEMBER404_1", (repo.getNearby(1).exceptionOrNull() as KiriApiException).code)
    }
}
