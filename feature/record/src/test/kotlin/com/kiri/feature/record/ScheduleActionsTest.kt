package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.detail.*
import com.kiri.domain.home.*
import com.kiri.domain.model.mission.MissionInfo
import com.kiri.domain.model.schedule.*
import com.kiri.domain.repository.*
import com.kiri.domain.usecase.schedule.GetScheduleEditAccessUseCase
import com.kiri.feature.detail.model.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleActionsTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private val clock = Clock.fixed(Instant.parse("2026-11-08T03:00:00Z"), ZoneOffset.UTC)
    private class Home : HomeRepository {
        override suspend fun load() = FamilyHome(LocalDate.parse("2026-11-08"), "1", "가족",
            listOf(FamilyMember("1", "나", "DAUGHTER"), FamilyMember("2", "엄마", "MOTHER")), null, emptyMap(), emptySet(), null)
        override suspend fun saveAnswer(question: DailyQuestion, answer: String): FamilyHome = error("unused")
        override suspend fun poke(question: DailyQuestion, memberId: String): FamilyHome = error("unused")
    }
    private class Schedules : CalendarScheduleRepository {
        var mine = MyScheduleStatus.ACCEPTED
        var date = LocalDate.parse("2026-11-08")
        var proposer = "엄마"
        var cancelled = false
        var cancelFails = true
        var accepted: Boolean? = null
        var responseFails = false
        // 월간 일정에 이 일정이 없으면 참여 상태를 몰라 canCancel 이 false 가 된다
        var listed = true
        override suspend fun getMonthlySchedules(month: YearMonth) =
            Result.success(if (listed) listOf(CalendarSchedule(7, date, LocalTime.of(10,0), CalendarScheduleStatus.CONFIRMED, mine)) else emptyList())
        override suspend fun getScheduleDetail(scheduleId: Long) = Result.success(ScheduleDetail(date, LocalTime.of(10,0),
            if (date == LocalDate.parse("2026-11-08")) ScheduleDisplayStatus.TODAY else ScheduleDisplayStatus.CONFIRMED,
            listOf(ScheduleParticipant(proposer, true), ScheduleParticipant("나", mine == MyScheduleStatus.ACCEPTED))))
        override suspend fun updateSchedule(scheduleId: Long, update: ScheduleUpdate) = Result.success(Unit)
        override suspend fun createSchedule(bucketItemId: Long, form: ScheduleUpdate) = Result.success(8L)
        override suspend fun cancel(scheduleId: Long): Result<Unit> {
            if (cancelFails) return Result.failure(Exception("취소 실패"))
            cancelled = true; return Result.success(Unit)
        }
        override suspend fun respond(scheduleId: Long, accept: Boolean): Result<Unit> {
            if (responseFails) return Result.failure(Exception("응답 실패"))
            accepted = accept; mine = if (accept) MyScheduleStatus.ACCEPTED else MyScheduleStatus.DECLINED
            return Result.success(Unit)
        }
    }
    private fun vm(schedules: Schedules): ServerScheduleDetailViewModel {
        val home = Home()
        val missions = object : MissionInfoRepository { override suspend fun getMission(missionId: Long): Result<MissionInfo> = Result.failure(Exception("none")) }
        return ServerScheduleDetailViewModel(SavedStateHandle(mapOf("scheduleId" to 7L)), GetScheduleEditAccessUseCase(schedules, home), missions, home = home, schedules = schedules, clock = clock)
    }
    @Test fun proposerCanCancelActiveScheduleAfterStartAndFailureDoesNotNavigateAway() = runTest(dispatcher) {
        val schedules = Schedules().apply { proposer = "나" }; val vm = vm(schedules); advanceUntilIdle()
        assertTrue(vm.uiState.value.canCancel)
        vm.cancel(); advanceUntilIdle(); assertFalse(schedules.cancelled); assertNotNull(vm.uiState.value.detail.error)
        schedules.cancelFails = false; vm.cancel(); advanceUntilIdle()
        assertTrue(schedules.cancelled); assertEquals(ServerScheduleDetailEffect.Back, vm.effect.first())
    }
    // 일정 취소하기 버튼은 일정 변경하기와 같은 조건(canEdit)으로 보이므로, canCancel 을 모를 때도 취소할 수 있어야 한다
    @Test fun proposerCanCancelFromScheduleButtonsEvenWhenMonthlyStatusIsUnknown() = runTest(dispatcher) {
        val schedules = Schedules().apply { proposer = "나"; date = LocalDate.parse("2026-11-10"); listed = false; cancelFails = false }
        val vm = vm(schedules); advanceUntilIdle()
        assertTrue(vm.uiState.value.canEdit)
        assertFalse(vm.uiState.value.canCancel)

        vm.cancel(); advanceUntilIdle()

        assertTrue(schedules.cancelled)
        assertEquals(ServerScheduleDetailEffect.Back, vm.effect.first())
    }
    @Test fun futureInviteChangesOnlyAfterSuccessfulServerResponse() = runTest(dispatcher) {
        val schedules = Schedules().apply { date = LocalDate.parse("2026-11-10"); mine = MyScheduleStatus.PENDING; responseFails = true }
        val vm = vm(schedules); advanceUntilIdle()
        assertTrue(vm.uiState.value.canRespond)
        vm.respond(true); advanceUntilIdle(); assertNull(schedules.accepted); assertEquals("미응답", vm.uiState.value.myResponse)
        schedules.responseFails = false; vm.respond(true); advanceUntilIdle()
        assertEquals(true, schedules.accepted); assertEquals("수락", vm.uiState.value.myResponse)
    }
    @Test fun messagesAreClearedAfterTheScreenShowsThem() = runTest(dispatcher) {
        val schedules = Schedules().apply { date = LocalDate.parse("2026-11-10"); mine = MyScheduleStatus.PENDING; responseFails = true }
        val vm = vm(schedules); advanceUntilIdle()

        // 작업 실패 오류는 토스트로 띄운 뒤 지운다
        vm.respond(true); advanceUntilIdle()
        assertNotNull(vm.uiState.value.detail.error)
        vm.messageShown()
        assertNull(vm.uiState.value.detail.error)

        // 작업 결과 안내도 띄운 뒤 지운다
        schedules.responseFails = false
        vm.respond(true); advanceUntilIdle()
        assertEquals("수락했어요.", vm.uiState.value.notice)
        vm.messageShown()
        assertNull(vm.uiState.value.notice)
    }
    @Test fun respondingFromTheResponseScreenGoesBackOnlyAfterSuccess() = runTest(dispatcher) {
        val schedules = Schedules().apply { date = LocalDate.parse("2026-11-10"); mine = MyScheduleStatus.PENDING; responseFails = true }
        val vm = vm(schedules); advanceUntilIdle()

        // 실패하면 응답 화면에 남아 오류를 보여준다
        vm.respond(false, leave = true); advanceUntilIdle()
        assertNull(schedules.accepted)
        assertNotNull(vm.uiState.value.detail.error)

        schedules.responseFails = false
        vm.respond(false, leave = true); advanceUntilIdle()
        assertEquals(false, schedules.accepted)
        assertNull(vm.uiState.value.notice)
        assertEquals(ServerScheduleDetailEffect.Back, vm.effect.first())
    }
}
