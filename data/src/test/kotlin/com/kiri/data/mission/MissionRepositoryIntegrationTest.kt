package com.kiri.data.mission

import com.kiri.data.detail.FakeDetailRepository
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.home.HomeScenario
import com.kiri.data.place.FakePlaceRepository
import com.kiri.domain.place.PlaceQuery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MissionRepositoryIntegrationTest {
    @Test fun discoveryHomeAndScheduledDetailShareCanonicalMission() = runTest {
        val missions = FakeMissionRepository()
        val home = FakeHomeRepository(latencyMillis = 0, missionRepository = missions)
        val detail = FakeDetailRepository(home, missions)
        val feed = FakePlaceRepository(missionRepository = missions).load(PlaceQuery()).items.single { it.id == FakeMissionRepository.RIVER_MISSION_ID }
        val family = home.load()
        val today = detail.load("schedule-demo")
        val preview = detail.preview(feed.id)
        assertEquals(feed.id, family.mission!!.missionId)
        assertEquals(feed.id, today.id)
        assertEquals(feed.id, preview.detail.id)
        assertEquals(feed.title, family.mission!!.title)
        assertEquals(feed.title, today.title)
        assertEquals(feed.title, preview.detail.title)
        assertEquals(today.activities, preview.detail.activities)
        assertEquals(today.nearby, preview.detail.nearby)
        assertEquals(feed.place, today.place)
        assertEquals(3, preview.reviews.size)
    }

    @Test fun discoveryAllowsBrowsingWithoutInventingScheduleOrParticipants() = runTest {
        val preview = FakeDetailRepository(FakeHomeRepository(scenario = HomeScenario.BROWSING, latencyMillis = 0)).preview("place-river")
        assertNull(preview.detail.date)
        assertNull(preview.detail.startTime)
        assertTrue(preview.detail.participants.isEmpty())
        assertEquals("place-river", preview.detail.id)
        assertTrue(runCatching { FakeDetailRepository(FakeHomeRepository(latencyMillis = 0)).preview("missing") }.isFailure)
    }

    @Test fun demoCardsAllHaveRatingAndTwoKeywords() = runTest {
        val catalog = FakeMissionRepository().catalog()
        assertEquals(6, catalog.size)
        catalog.forEach { assertNotNull(it.rating); assertEquals(2, it.tags.size) }
    }
}
