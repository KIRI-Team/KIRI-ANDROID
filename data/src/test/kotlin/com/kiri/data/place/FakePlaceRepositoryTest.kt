package com.kiri.data.place

import com.kiri.domain.place.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FakePlaceRepositoryTest {
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
