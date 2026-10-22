package com.kiri.feature.schedule

import androidx.lifecycle.SavedStateHandle
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.record.LocalBucketRepository
import com.kiri.data.schedule.LocalScheduleRepository
import com.kiri.domain.schedule.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {
    @Before fun setup() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun teardown() { Dispatchers.resetMain() }
    @Test fun previewDoesNotCreateResponseRowsDraftRestoresAndRetryDoesNotDuplicate() = runTest {
        val clock = Clock.fixed(Instant.parse("2026-10-18T03:00:00Z"), ZoneOffset.UTC)
        val home = FakeHomeRepository(clock, latencyMillis = 0)
        val missions = FakeMissionRepository()
        val buckets = LocalBucketRepository(home, missions, clock)
        val bucket = buckets.save("place-river")
        val source = LocalScheduleRepository(home, missions, buckets, clock)
        var fail = true
        val repository = object : ScheduleRepository by source {
            override suspend fun create(draft: com.kiri.domain.schedule.ScheduleDraft, requestId: String): com.kiri.domain.schedule.FamilySchedule {
                val result = source.create(draft, requestId)
                if (fail) { fail = false; error("응답 유실") }
                return result
            }
        }
        val saved = SavedStateHandle(mapOf("bucketId" to bucket.id))
        val vm = ScheduleViewModel(repository, buckets, home, missions, saved, clock)
        vm.change(vm.uiState.value.draft!!.copy(startsAt = clock.instant().plusSeconds(86400), inviteeIds = setOf("mother"), message = "함께 가요"))
        vm.preview()
        assertTrue(vm.uiState.value.preview)
        assertEquals(1, source.schedules.value.size) // Existing demo schedule only.
        val restored = ScheduleViewModel(repository, buckets, home, missions, saved, clock)
        assertTrue(restored.uiState.value.preview)
        assertEquals("함께 가요", restored.uiState.value.draft!!.message)
        restored.send()
        assertNotNull(restored.uiState.value.error)
        assertTrue(restored.uiState.value.preview)
        assertEquals("함께 가요", restored.uiState.value.draft!!.message)
        restored.send()
        assertNull(restored.uiState.value.error)
        assertFalse(restored.uiState.value.editing)
        assertEquals(2, source.schedules.value.size)
        assertEquals(1, source.notices.value.size)
    }
}
