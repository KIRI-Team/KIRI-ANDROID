package com.kiri.data.place

import com.kiri.domain.place.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FakePlaceRepositoryTest {
    @Test fun detailedFiltersCombineBeforePagingAndUnknownFactsDoNotMatch() = runTest {
        val repo = FakePlaceRepository(pageSize = 1)
        val day = java.time.LocalDate.of(2026, 10, 16).toEpochDay()
        val filters = PlaceFilters(setOf("서울", "인천"), 3, day, 570, 3, 5000, 15000)
        val page = repo.load(PlaceQuery(filters = filters))
        assertEquals(3, page.totalCount)
        assertEquals(1, page.items.size)
        assertEquals(0, repo.load(PlaceQuery(filters = filters.copy(people = 7))).totalCount)
        assertEquals(0, repo.load(PlaceQuery(filters = filters.copy(regions = setOf("부산")))).totalCount)
        assertEquals(0, repo.load(PlaceQuery(filters = filters.copy(minutesOfDay = 60))).totalCount)
        val unknown = PlaceMission("unknown", "활동", "장소", "서울", PlaceCategory.FOOD)
        assertFalse(filters.matches(unknown))
        assertTrue(PlaceFilters().matches(unknown))
    }
    @Test fun searchCategoryAndPagingReturnMatchingCount() = runTest {
        val repo = FakePlaceRepository(pageSize = 2)
        val first = repo.load(PlaceQuery())
        assertEquals(6, first.totalCount)
        assertEquals(2, first.items.size)
        assertTrue(first.items.first().pick)
        val second = repo.load(PlaceQuery(cursor = first.nextCursor))
        assertTrue(first.items.none { one -> second.items.any { it.id == one.id } })
        val food = repo.load(PlaceQuery("  광장  ", PlaceCategory.FOOD))
        assertEquals(1, food.totalCount)
        assertEquals(0, repo.load(PlaceQuery("없는 검색어")).totalCount)
        assertEquals(0, repo.load(PlaceQuery("광장", PlaceCategory.SHOW)).totalCount)
    }
    @Test fun savesAreIdempotentAndSurviveReload() = runTest {
        val repo = FakePlaceRepository()
        repo.save("place-market"); repo.save("place-market")
        assertEquals(1, repo.saved().size)
        assertTrue(repo.load(PlaceQuery()).items.first().saved)
    }
    @Test fun invalidMissionCannotBeSaved() = runTest {
        val repo = FakePlaceRepository()
        try { repo.save("missing"); fail("Expected invalid mission rejection") } catch (_: IllegalArgumentException) { }
        assertTrue(repo.saved().isEmpty())
    }
}
