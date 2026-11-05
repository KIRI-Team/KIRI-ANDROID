package com.kiri.data.mission

import com.kiri.data.network.KiriApiException
import com.kiri.data.network.apiBaseUrl
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.mission.MissionBucketState
import com.kiri.domain.model.mission.MissionGuide
import com.kiri.domain.model.mission.MissionNotFoundException
import com.kiri.domain.model.mission.MissionPlace
import com.kiri.domain.model.mission.MissionStatus
import com.kiri.domain.model.mission.WalkingLevel
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

// 미션 상세 API 응답을 도메인 미션으로 바꾸고, 실패 코드를 도메인 오류로 바꾸는지 확인한다
class MissionInfoRepositoryImplTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.close()

    private fun repository(): MissionInfoRepositoryImpl {
        val api = Retrofit.Builder()
            .baseUrl(apiBaseUrl(server.url("/").toString()))
            .client(OkHttpClient())
            .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MissionApi::class.java)
        return MissionInfoRepositoryImpl(api, kiriJson)
    }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse.Builder().code(code).body(body).build())
    }

    @Test
    fun 미션_id_경로로_GET_요청하고_미션_장소_준비물_단계를_돌려준다() = runTest {
        enqueue(200, SUCCESS)

        val mission = repository().getMission(130).getOrThrow()

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/missions/130", request.url.encodedPath)
        assertEquals(130L, mission.id)
        assertEquals("국립현대미술관 서울 관람하기", mission.title)
        assertEquals("EXHIBITION", mission.category)
        assertEquals(MissionStatus.ACTIVE, mission.status)
        assertEquals("한국관광공사 (공공누리 제1유형)", mission.imageSource)
        assertNull(mission.startDate)
        assertEquals(LocalTime.of(10, 0), mission.openingTime)
        assertEquals(LocalTime.of(18, 0), mission.closingTime)
        assertTrue(mission.isFree)
        assertEquals(60, mission.durationMinMinutes)
        assertEquals(120, mission.durationMaxMinutes)
        assertEquals(WalkingLevel.MEDIUM, mission.walkingLevel)
        assertEquals(MissionPlace(52, "국립현대미술관 서울", "서울특별시 종로구 삼청로 30", 37.5786, 126.98), mission.place)
        assertEquals(listOf(MissionGuide("사전 예약", "일부 전시 예약 필요")), mission.prepareItems)
        assertEquals(listOf(MissionGuide("상설 전시 관람하기", "마음에 드는 작품 고르기")), mission.steps)
        assertEquals(MissionBucketState.NONE, mission.bucketState)
        assertNull(mission.averageRating)
        assertEquals(0, mission.reviewCount)
    }

    @Test
    fun 비어_있거나_앱이_모르는_값은_정보_없음으로_둔다() = runTest {
        enqueue(
            200,
            """{"isSuccess":true,"code":"MISSION200_2","message":"미션 상세 조회 성공","result":{""" +
                """"missionId":7,"title":"산책","status":"PAUSED","imageUrl":null,"imageSource":"출처",""" +
                """"openingTime":"bad","price":null,"walkingLevel":"EXTREME","bucketState":"UNKNOWN"}}""",
        )

        val mission = repository().getMission(7).getOrThrow()

        assertNull(mission.status)
        // 이미지가 없으면 출처도 쓰지 않는다
        assertNull(mission.imageSource)
        assertNull(mission.openingTime)
        assertNull(mission.price)
        assertNull(mission.walkingLevel)
        assertNull(mission.place)
        assertTrue(mission.steps.isEmpty())
        assertEquals(MissionBucketState.NONE, mission.bucketState)
    }

    @Test
    fun 없거나_노출할_수_없는_미션은_MissionNotFoundException_으로_바꾼다() = runTest {
        enqueue(404, """{"isSuccess":false,"code":"MISSION404_1","message":"존재하지 않는 미션입니다.","result":null}""")
        enqueue(400, """{"isSuccess":false,"code":"COMMON400_1","message":"잘못된 요청입니다.","result":null}""")

        assertTrue(repository().getMission(1).exceptionOrNull() is MissionNotFoundException)
        assertEquals("COMMON400_1", (repository().getMission(1).exceptionOrNull() as KiriApiException).code)
    }

    private companion object {
        // 명세의 성공 응답 예시
        const val SUCCESS = """{
  "isSuccess": true,
  "code": "MISSION200_2",
  "message": "미션 상세 조회 성공",
  "result": {
    "missionId": 130,
    "title": "국립현대미술관 서울 관람하기",
    "description": "한국 근현대 미술 작품을 상설·기획 전시로 만날 수 있는 미술관입니다.",
    "category": "EXHIBITION",
    "missionType": "PERMANENT",
    "status": "ACTIVE",
    "indoorOutdoor": "INDOOR",
    "imageUrl": "https://tong.visitkorea.or.kr/cms/resource/00/0000000_image2_1.jpg",
    "imageSource": "한국관광공사 (공공누리 제1유형)",
    "startDate": null,
    "endDate": null,
    "openingTime": "10:00",
    "closingTime": "18:00",
    "price": 0,
    "durationMinMinutes": 60,
    "durationMaxMinutes": 120,
    "walkingLevel": "MEDIUM",
    "sourceUrl": "https://korean.visitkorea.or.kr/detail/ms_detail.do?cotid=...",
    "place": {
      "placeId": 52,
      "name": "국립현대미술관 서울",
      "address": "서울특별시 종로구 삼청로 30",
      "latitude": 37.5786,
      "longitude": 126.98
    },
    "prepareItems": [
      { "title": "사전 예약", "description": "일부 전시 예약 필요" }
    ],
    "steps": [
      { "title": "상설 전시 관람하기", "description": "마음에 드는 작품 고르기" }
    ],
    "bucketSaved": false,
    "bucketState": "NONE",
    "averageRating": null,
    "reviewCount": 0
  }
}"""
    }
}
