package com.kiri.domain.usecase.schedule

import com.kiri.domain.home.DailyQuestion
import com.kiri.domain.home.FamilyHome
import com.kiri.domain.home.FamilyMember
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.model.schedule.ScheduleParticipant
import com.kiri.domain.model.schedule.ScheduleUpdate
import com.kiri.domain.repository.CalendarScheduleRepository
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// 일정 상세와 "내가 바꿀 수 있는 일정인지" 판단을 확인한다
class GetScheduleEditAccessUseCaseTest {

    private class Schedules(private val detail: Result<ScheduleDetail>) : CalendarScheduleRepository {
        override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> = Result.success(emptyList())
        override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> = detail
        override suspend fun updateSchedule(scheduleId: Long, update: ScheduleUpdate): Result<Unit> = Result.success(Unit)
    }

    // 나는 1(엄마). 2(첫째), 3(아빠), 탈퇴한 4 가 가족이다. fail 이면 불러오기에 실패한다
    private class Home(private val fail: Boolean = false) : HomeRepository {
        var loads = 0
        override suspend fun load(): FamilyHome {
            loads++
            if (fail) throw IOException()
            return FamilyHome(
                date = LocalDate.of(2026, 11, 4),
                currentUserId = "1",
                familyName = "우리 가족",
                members = listOf(
                    FamilyMember("1", "엄마", ""),
                    FamilyMember("2", "첫째", ""),
                    FamilyMember("3", "아빠", ""),
                    FamilyMember("4", "떠난 가족", "", accessible = false),
                ),
                question = null,
                answers = emptyMap(),
                pokedMemberIds = emptySet(),
                mission = null,
            )
        }
        override suspend fun saveAnswer(question: DailyQuestion, answer: String) = load()
        override suspend fun poke(question: DailyQuestion, memberId: String) = load()
    }

    private fun detail(status: ScheduleDisplayStatus?, vararg names: String) = ScheduleDetail(
        date = LocalDate.of(2026, 11, 17),
        time = LocalTime.of(14, 50),
        status = status,
        participants = names.map { ScheduleParticipant(it, true) },
    )

    private fun access(detail: ScheduleDetail, home: HomeRepository = Home()) =
        runBlocking { GetScheduleEditAccessUseCase(Schedules(Result.success(detail)), home)(1).getOrThrow() }

    @Test
    fun 예정이나_확정된_내_일정은_바꿀_수_있고_초대된_가족을_회원_id로_알려준다() {
        listOf(ScheduleDisplayStatus.SCHEDULED, ScheduleDisplayStatus.CONFIRMED).forEach { status ->
            val access = access(detail(status, "엄마", "아빠", "모르는 사람"))

            assertTrue(access.isMine)
            assertTrue(access.canEdit)
            assertEquals("1", access.me?.id)
            // 나와 탈퇴한 가족은 초대할 수 없다
            assertEquals(listOf("2", "3"), access.invitableMembers.map { it.id })
            // 닉네임이 가족에 없는 참여자는 뺀다
            assertEquals(setOf("3"), access.invitedMemberIds)
        }
    }

    @Test
    fun 다른_가족이_제안한_일정은_바꿀_수_없다() {
        val access = access(detail(ScheduleDisplayStatus.CONFIRMED, "아빠", "엄마"))

        assertFalse(access.isMine)
        assertFalse(access.canEdit)
    }

    @Test
    fun 당일_완료_모르는_상태는_가족을_불러오지_않고_바꿀_수_없다() {
        listOf(ScheduleDisplayStatus.TODAY, ScheduleDisplayStatus.COMPLETED, null).forEach { status ->
            val home = Home()
            val access = access(detail(status, "엄마", "아빠"), home)

            assertFalse(access.canEdit)
            assertNull(access.family)
            assertEquals(0, home.loads)
        }
    }

    @Test
    fun 가족을_불러오지_못하면_상세는_돌려주고_바꿀_수_없다고_본다() {
        val access = access(detail(ScheduleDisplayStatus.CONFIRMED, "엄마"), Home(fail = true))

        assertNull(access.family)
        assertFalse(access.isMine)
        assertFalse(access.canEdit)
        assertTrue(access.invitableMembers.isEmpty())
    }

    @Test
    fun 상세를_불러오지_못하면_그_오류로_실패한다() {
        val result = runBlocking {
            GetScheduleEditAccessUseCase(Schedules(Result.failure(ScheduleNotFoundException())), Home())(1)
        }

        assertTrue(result.exceptionOrNull() is ScheduleNotFoundException)
    }
}
