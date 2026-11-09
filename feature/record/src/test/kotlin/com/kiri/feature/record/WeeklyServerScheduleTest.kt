package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.home.*
import com.kiri.domain.model.schedule.*
import com.kiri.domain.record.ScheduleRequestChannel
import com.kiri.domain.repository.CalendarScheduleRepository
import com.kiri.feature.record.presentation.schedule.ScheduleRegisterDraft
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklyServerScheduleTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private val clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneOffset.UTC)
    private class Schedules : CalendarScheduleRepository {
        val calls = mutableListOf<Pair<Long, ScheduleUpdate>>()
        var fails = true
        override suspend fun getMonthlySchedules(month: YearMonth) = Result.success(emptyList<CalendarSchedule>())
        override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> = error("unused")
        override suspend fun updateSchedule(scheduleId: Long, update: ScheduleUpdate): Result<Unit> = error("unused")
        override suspend fun createSchedule(bucketItemId: Long, form: ScheduleUpdate): Result<Long> = error("Weekly missions must not create a bucket schedule directly")
        override suspend fun createWeeklySchedule(missionId: Long, form: ScheduleUpdate): Result<Long> {
            calls += missionId to form
            return if (fails) Result.failure(Exception("연결 실패")) else Result.success(100L)
        }
    }
    private val home = object : HomeRepository {
        override suspend fun load() = FamilyHome(LocalDate.parse("2026-11-08"), "1", "가족",
            listOf(FamilyMember("1", "나", "DAUGHTER"), FamilyMember("2", "엄마", "MOTHER")), null, emptyMap(), emptySet(),
            WeeklyMission("55", "가족 산책", "함께 걸어요", emptyList(), false), serverBacked = true)
        override suspend fun saveAnswer(question: DailyQuestion, answer: String): FamilyHome = error("unused")
        override suspend fun poke(question: DailyQuestion, memberId: String): FamilyHome = error("unused")
    }
    @Test fun weeklyFormUsesMissionRouteRetriesWithoutLosingDraftAndRestoresSuccessfulRequest() = runTest(dispatcher) {
        val schedules = Schedules(); val saved = SavedStateHandle(mapOf("weeklyMissionId" to "55"))
        val vm = BucketScheduleCreateViewModel(saved, schedules, home, clock); advanceUntilIdle()
        assertEquals("가족 산책", vm.uiState.value.title); assertEquals(listOf("2"), vm.uiState.value.members.map { it.id })
        val draft = ScheduleRegisterDraft(LocalDate.parse("2026-11-15"), LocalTime.of(14,0), setOf("2"), "함께 가요")
        vm.onDraftChange(draft); vm.send(ScheduleRequestChannel.APP); vm.send(ScheduleRequestChannel.APP); advanceUntilIdle()
        assertEquals(1, schedules.calls.size); assertEquals(55L, schedules.calls.single().first)
        assertEquals(setOf(2L), schedules.calls.single().second.inviteeIds); assertEquals(draft, vm.uiState.value.draft)
        assertNotNull(vm.uiState.value.error)
        schedules.fails = false; vm.send(ScheduleRequestChannel.APP); advanceUntilIdle()
        assertEquals(BucketScheduleCreateEffect.SentByApp, vm.effect.first())
        assertEquals(100L, saved.get<Long>("createdScheduleId"))
        val restored = BucketScheduleCreateViewModel(saved, schedules, home, clock); advanceUntilIdle()
        assertEquals(BucketScheduleCreateEffect.SentByApp, restored.effect.first())
        restored.send(ScheduleRequestChannel.APP); advanceUntilIdle(); assertEquals(2, schedules.calls.size)
    }
    @Test fun pastTimeDoesNotReachCreateApi() = runTest(dispatcher) {
        val schedules = Schedules(); val vm = BucketScheduleCreateViewModel(SavedStateHandle(mapOf("weeklyMissionId" to "55")), schedules, home, clock)
        advanceUntilIdle(); vm.onDraftChange(ScheduleRegisterDraft(LocalDate.parse("2026-11-08"), LocalTime.of(10,0), setOf("2")))
        vm.send(ScheduleRequestChannel.APP); advanceUntilIdle()
        assertTrue(schedules.calls.isEmpty()); assertNotNull(vm.uiState.value.error)
    }
}
