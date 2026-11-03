package com.kiri.data.home

import com.kiri.data.network.kiriJson
import com.kiri.domain.home.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import mockwebserver3.*
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class UpcomingHomeRepositoryTest {
    private fun api(server: MockWebServer) = Retrofit.Builder().baseUrl(server.url("/"))
        .addConverterFactory(kiriJson.asConverterFactory("application/json".toMediaType())).build().create(HomeApi::class.java)

    @Test fun mapsKoreanTimeAndPrioritizesResponseCardWithoutInventingMissionId() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body(javaClass.getResource("/api/upcoming.json")!!.readText().removePrefix("\uFEFF")).build())
            val clock = Clock.fixed(Instant.parse("2026-11-15T00:00:00Z"), ZoneOffset.UTC)
            val repo = UpcomingHomeRepository(FakeHomeRepository(clock, latencyMillis = 0), api(server), kiriJson)
            val home = repo.load()
            assertEquals(2, home.schedules.size)
            assertEquals(Instant.parse("2026-11-20T09:00:00Z"), home.schedules.first().startsAt)
            assertEquals("120", selectHomeSchedule(home.schedules, clock.instant())!!.schedule.id)
            assertTrue(home.schedules.all { it.missionId.isEmpty() })
            val saved = repo.saveAnswer(home.question!!, "답변")
            assertEquals(home.schedules, saved.schedules)
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun browsingDoesNotRequestFamilyScheduleEndpoint() = runTest {
        MockWebServer().use { server ->
            server.start()
            val repo = UpcomingHomeRepository(FakeHomeRepository(scenario = HomeScenario.BROWSING, latencyMillis = 0), api(server), kiriJson)
            assertTrue(repo.load().browsing)
            assertEquals(0, server.requestCount)
        }
    }
}
