package com.kiri.data.record

import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.home.HomeScenario
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.detail.FakeDetailRepository
import com.kiri.domain.detail.Condition
import com.kiri.domain.record.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FakeRecordRepositoryTest {
    @Test fun refreshingConfirmedSchedulesPreservesEditedMemoriesAndRepresentativePhotos() = runTest {
        val source = home()
        var snapshot = source.load()
        val mutableHome = object : com.kiri.domain.home.HomeRepository by source {
            override suspend fun load() = snapshot
        }
        val repo = FakeRecordRepository(mutableHome, FakeMissionRepository())
        repo.refresh()
        repo.changeRepresentativePhoto("record-memory-1", "pixel-1")
        val memory = repo.records.value.memories.first()
        repo.updateMemory(memory.copy(text = "수정한 추억"))
        val schedule = snapshot.schedules.first()
        snapshot = snapshot.copy(schedules = snapshot.schedules + schedule.copy(id = "confirmed-later"))
        repo.refresh()
        assertTrue(repo.records.value.entries.any { it.id == "confirmed-later" })
        assertEquals("수정한 추억", repo.records.value.memories.first().text)
        assertEquals("pixel-1", repo.records.value.entries.first { it.id == "record-memory-1" }.representativePhotoId)
        snapshot = snapshot.copy(schedules = snapshot.schedules.filterNot { it.id == "confirmed-later" })
        repo.refresh()
        assertFalse(repo.records.value.entries.any { it.id == "confirmed-later" })
    }
    private val clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC)
    private fun home(scenario: HomeScenario = HomeScenario.DEFAULT) = FakeHomeRepository(clock, scenario, latencyMillis = 0)

    @Test fun recordAndHomeUseTheSameMissionContentAndConfirmedScheduleId() = runTest {
        val missions = FakeMissionRepository()
        val home = home()
        val sharedDetail = FakeDetailRepository(home, missions)
        val repo = FakeRecordRepository(home, missions, sharedDetail)
        repo.refresh()
        assertEquals(sharedDetail.load("schedule-demo"), repo.detail("schedule-demo"))
        repo.applyConditions("schedule-demo", setOf(Condition.MORE_REST))
        assertEquals(setOf(Condition.MORE_REST), sharedDetail.load("schedule-demo").conditions)
        repo.records.value.entries.forEach { entry ->
            val content = missions.load(entry.missionId)
            assertEquals(content.summary.title, repo.detail(entry.id).title)
            assertEquals(content.activities, repo.detail(entry.id).activities)
        }
        assertEquals(2, repo.records.value.entries.count { it.kind == CalendarEntryKind.SCHEDULE })
    }

    @Test fun representativePhotoBelongsToItsRecordAndSurvivesRefresh() = runTest {
        val repo = FakeRecordRepository(home(), FakeMissionRepository())
        repo.refresh()
        assertTrue(runCatching { repo.changeRepresentativePhoto("record-memory-1", "pixel-2") }.isFailure)
        repo.changeRepresentativePhoto("record-memory-1", "pixel-1")
        repo.refresh()
        assertEquals("pixel-1", repo.records.value.entries.single { it.id == "record-memory-1" }.representativePhotoId)
        assertEquals("pixel-2", repo.records.value.entries.single { it.id == "record-memory-2" }.representativePhotoId)
    }

    @Test fun editingAndDeletingValidateOwnershipAndPreserveOtherFamilyRecords() = runTest {
        val repo = FakeRecordRepository(home(), FakeMissionRepository())
        repo.refresh()
        val original = repo.records.value.memories.first()
        val other = repo.records.value.memories.last()
        assertTrue(runCatching { repo.updateMemory(original.copy(authorId = "father")) }.isFailure)
        assertTrue(runCatching { repo.updateMemory(original.copy(text = " ")) }.isFailure)
        assertTrue(runCatching { repo.updateMemory(original.copy(text = "가".repeat(501))) }.isFailure)
        repo.updateMemory(original.copy(text = "수정된 기록", photos = listOf(RecordPhoto("new", "content://photo/new"))))
        assertEquals("수정된 기록", repo.records.value.memories.first().text)
        assertTrue(repo.records.value.photos[original.entryId].orEmpty().any { it.id == "new" })
        assertEquals(other, repo.records.value.memories.last())
        repo.deleteMemory(original.id)
        assertEquals(listOf(other), repo.records.value.memories)
    }

    @Test fun onlyTodaysScheduleCanApplyConditionsAndBrowsingExposesNoFamilyRecords() = runTest {
        val repo = FakeRecordRepository(home(), FakeMissionRepository())
        repo.refresh()
        assertTrue(runCatching { repo.applyConditions("record-upcoming", setOf(Condition.MORE_REST)) }.isFailure)
        assertTrue(runCatching { repo.applyConditions("record-bucket", setOf(Condition.MORE_REST)) }.isFailure)
        assertEquals(setOf(Condition.MORE_REST), repo.applyConditions("schedule-demo", setOf(Condition.MORE_REST)).conditions)
        val browsing = FakeRecordRepository(home(HomeScenario.BROWSING), FakeMissionRepository())
        browsing.refresh()
        assertTrue(browsing.records.value.entries.isEmpty())
        assertTrue(runCatching { browsing.detail("schedule-demo") }.isFailure)
    }
}
