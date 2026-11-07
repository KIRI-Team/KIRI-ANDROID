package com.kiri.data.place

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
                override suspend fun getMission(missionId: Long): Result<MissionInfo> = error("No saved missions")
            }, object : BucketItemRepository {
                override suspend fun getBucketItems(memberId: Long?, cursor: Long?, size: Int?) = Result.success(BucketItemPage.Empty)
                override suspend fun deleteBucketItem(bucketItemId: Long): Result<Unit> = error("unused")
            }, kiriJson)
            val result = repo.load(PlaceQuery(keyword = "현대", category = PlaceCategory.CULTURE,
                filters = PlaceFilters(regions = setOf("서울"), distanceKm = 3,
                    dateEpochDay = LocalDate.parse("2026-11-07").toEpochDay(), maxPrice = 0)))
            assertEquals(listOf("129"), result.items.map { it.id })
            assertFalse(result.items.single().canSave)
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

    private fun page(id: Long, title: String, cursor: Long) = MockResponse.Builder().body(
        """{"isSuccess":true,"code":"MISSION200_1","message":"조회","result":{"missions":[{"missionId":$id,"title":"$title","category":"EXHIBITION","price":0}],"nextCursor":$cursor,"hasNext":true}}"""
    ).build()
}
