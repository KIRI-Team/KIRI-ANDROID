package com.kiri.data.detail

import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.home.HomeScenario
import com.kiri.domain.detail.Condition
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FakeDetailRepositoryTest {
    @Test fun selectionPersistsOnReloadAndMainMissionDoesNotChange() = runBlocking {
        val repository = FakeDetailRepository(FakeHomeRepository(latencyMillis = 0))
        val before = repository.load("weekly-demo")
        repository.applyConditions("weekly-demo", setOf(Condition.MORE_REST))
        assertEquals(before.copy(conditions = setOf(Condition.MORE_REST)), repository.load("weekly-demo"))
    }
    @Test fun unknownMissionNeverFallsBackToDemo() = runBlocking {
        val result = runCatching { FakeDetailRepository(FakeHomeRepository(latencyMillis = 0)).load("missing") }
        assertTrue(result.isFailure)
    }
    @Test fun demoScheduleAndMissionShareAppliedConditions() = runBlocking {
        val repository = FakeDetailRepository(FakeHomeRepository(latencyMillis = 0))
        repository.applyConditions("weekly-demo", setOf(Condition.LESS_WALKING))
        assertEquals(setOf(Condition.LESS_WALKING), repository.load("schedule-demo").conditions)
    }
    @Test fun browsingAndUnavailableMissionsCannotBeOpened() = runBlocking {
        for (scenario in listOf(HomeScenario.BROWSING, HomeScenario.MISSION_UNAVAILABLE)) {
            assertTrue(runCatching { FakeDetailRepository(FakeHomeRepository(scenario = scenario, latencyMillis = 0)).load("weekly-demo") }.isFailure)
        }
    }
    @Test fun completedMissionCannotBeCustomized() = runBlocking {
        val repository = FakeDetailRepository(FakeHomeRepository(scenario = HomeScenario.COMPLETED_MISSION, latencyMillis = 0))
        assertTrue(repository.load("weekly-demo").completed)
        assertTrue(runCatching { repository.applyConditions("weekly-demo", setOf(Condition.LESS_WALKING)) }.isFailure)
    }
}
