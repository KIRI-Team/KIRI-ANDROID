package com.kiri.data.place

import com.kiri.data.mission.MissionResponse
import com.kiri.data.mission.toMissionInfo
import com.kiri.data.mission.MissionApi
import com.kiri.data.network.kiriJson
import com.kiri.domain.model.bucket.BucketItemPage
import com.kiri.domain.model.mission.MissionInfo
import com.kiri.domain.place.*
import com.kiri.domain.repository.BucketItemRepository
import com.kiri.domain.repository.MissionInfoRepository
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ServerPlaceRepositoryTest {
    @Test fun sendsServerFiltersAndFollowsCursorPastNonmatchingSearchPage() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(page(130, "다른 활동", 130))
            server.enqueue(page(129, "현대미술관", 129))
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
                .build().create(MissionApi::class.java)
            val repo = ServerPlaceRepository(api, object : MissionInfoRepository {
                override suspend fun getMission(missionId: Long): Result<MissionInfo> = Result.failure(IllegalStateException("상세 조회 실패"))
            }, object : BucketItemRepository {
                override suspend fun getBucketItems(memberId: Long?, cursor: Long?, size: Int?) = Result.success(BucketItemPage.Empty)
                override suspend fun deleteBucketItem(bucketItemId: Long): Result<Unit> = error("unused")
            }, kiriJson)
            val result = repo.load(PlaceQuery(keyword = "현대", category = PlaceCategory.CULTURE,
                filters = PlaceFilters(regions = setOf("서울"), distanceKm = 3,
                    dateEpochDay = LocalDate.parse("2026-11-07").toEpochDay(), maxPrice = 0)))
            assertEquals(listOf("129"), result.items.map { it.id })
            assertTrue(result.items.single().canSave)
            assertNull(result.totalCount)
            assertNotNull(result.nextCursor)
            val first = server.takeRequest().url
            assertEquals("/api/v1/missions", first.encodedPath)
            assertEquals("EXHIBITION", first.queryParameter("category"))
            assertEquals("11", first.queryParameter("regionCode"))
            assertEquals("3", first.queryParameter("distance"))
            assertEquals("true", first.queryParameter("isFree"))
            assertEquals("2026-11-07", first.queryParameter("date"))
            assertNull(first.queryParameter("cursor"))
            assertEquals("130", server.takeRequest().url.queryParameter("cursor"))
            assertEquals(2, server.requestCount)
        }
    }

    @Test fun feedImageAndDetailRatingReachCardsAndFailedDetailKeepsFeed() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("""{"isSuccess":true,"code":"MISSION200_1","message":"조회","result":{"missions":[
                {"missionId":1,"title":"사진 있는 활동","category":"EXHIBITION","imageUrl":"https://images.example/one.jpg","imageSource":"공식 출처","price":0,"distance":1.2},
                {"missionId":2,"title":"상세 실패 활동","category":"EXHIBITION","imageUrl":"https://images.example/two.jpg"},
                {"missionId":3,"title":"사진 없는 활동","category":"EXHIBITION","imageUrl":""}
            ],"hasNext":false}}""").build())
            val api = Retrofit.Builder().baseUrl(server.url("/"))
                .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType()))
                .build().create(MissionApi::class.java)
            val details = object : MissionInfoRepository {
                override suspend fun getMission(missionId: Long): Result<MissionInfo> = when (missionId) {
                    1L -> Result.success(MissionResponse(1, "사진 있는 활동", category = "EXHIBITION",
                        imageUrl = "https://images.example/detail.jpg", averageRating = 4.6, reviewCount = 3,
                        bucketState = "SAVED").toMissionInfo())
                    3L -> Result.success(MissionResponse(3, "사진 없는 활동", category = "EXHIBITION").toMissionInfo())
                    else -> Result.failure(IllegalStateException("상세 조회 실패"))
                }
            }
            val repo = ServerPlaceRepository(api, details, object : BucketItemRepository {
                override suspend fun getBucketItems(memberId: Long?, cursor: Long?, size: Int?) = Result.success(BucketItemPage.Empty)
                override suspend fun deleteBucketItem(bucketItemId: Long): Result<Unit> = error("unused")
            }, kiriJson)
            val cards = repo.load(PlaceQuery()).items
            assertEquals(listOf("1", "2", "3"), cards.map { it.id })
            assertEquals("https://images.example/one.jpg", cards[0].imageUrl)
            assertEquals("공식 출처", cards[0].imageSource)
            assertEquals("4.6", cards[0].rating)
            assertEquals(listOf("무료", "1.2km"), cards[0].tags)
            assertTrue(cards[0].saved)
            assertEquals("https://images.example/two.jpg", cards[1].imageUrl)
            assertNull(cards[1].rating)
            assertNull(cards[2].imageUrl)
            assertNull(cards[2].rating)
            assertTrue(cards[2].tags.isEmpty())
        }
    }

    private fun page(id: Long, title: String, cursor: Long) = MockResponse.Builder().body(
        """{"isSuccess":true,"code":"MISSION200_1","message":"조회","result":{"missions":[{"missionId":$id,"title":"$title","category":"EXHIBITION","price":0}],"nextCursor":$cursor,"hasNext":true}}"""
    ).build()
}
