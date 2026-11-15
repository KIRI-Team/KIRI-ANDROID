package com.kiri.domain.usecase.schedule

import com.kiri.domain.home.DailyQuestion
import com.kiri.domain.home.FamilyHome
import com.kiri.domain.home.FamilyMember
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.model.schedule.MyScheduleStatus
import com.kiri.domain.model.schedule.ScheduleParticipant
import com.kiri.domain.model.schedule.ScheduleProposer
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

// 일정 상세와, 서버가 판단한 변경 가능 여부에 따라 가족 정보를 채우는지 확인한다
class GetScheduleEditAccessUseCaseTest {

    private class Schedules(private val detail: Result<ScheduleDetail>) : CalendarScheduleRepository {
        override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> = Result.success(emptyList())
        override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> = detail
        override suspend fun updateSchedule(scheduleId: Long, update: ScheduleUpdate): Result<Unit> = Result.success(Unit)
        override suspend fun createSchedule(bucketItemId: Long, form: ScheduleUpdate): Result<Long> = Result.success(1)
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

    // 맨 앞 이름이 제안자다. canEdit 는 서버가 판단해 주는 값이다
    private fun detail(canEdit: Boolean, vararg names: String) = ScheduleDetail(
        date = LocalDate.of(2026, 11, 17),
        time = LocalTime.of(14, 50),
        status = ScheduleDisplayStatus.CONFIRMED,
        participants = names.map { ScheduleParticipant(it, MyScheduleStatus.ACCEPTED) },
        proposer = names.firstOrNull()?.let { ScheduleProposer(1, it, null) },
        canEdit = canEdit,
    )

    private fun access(detail: ScheduleDetail, home: HomeRepository = Home()) =
        runBlocking { GetScheduleEditAccessUseCase(Schedules(Result.success(detail)), home)(1).getOrThrow() }

    @Test
    fun 서버가_바꿀_수_있다고_하면_가족을_불러와_초대된_가족을_회원_id로_알려준다() {
        val access = access(detail(canEdit = true, "엄마", "아빠", "모르는 사람"))

        assertTrue(access.canEdit)
        assertEquals("1", access.me?.id)
        // 나와 탈퇴한 가족은 초대할 수 없다
        assertEquals(listOf("2", "3"), access.invitableMembers.map { it.id })
        // 제안자와 닉네임이 가족에 없는 참여자는 뺀다
        assertEquals(setOf("3"), access.invitedMemberIds)
    }

    @Test
    fun 제안자가_참여자_맨_앞이_아니어도_제안자를_빼고_초대된_가족을_고른다() {
        val detail = detail(canEdit = true, "아빠", "첫째").copy(proposer = ScheduleProposer(1, "첫째", null))

        assertEquals(setOf("3"), access(detail).invitedMemberIds)
    }

    @Test
    fun 서버가_바꿀_수_없다고_하면_가족을_불러오지_않는다() {
        val home = Home()
        // 내가 제안한 일정이어도 서버 판단(당일·시작·마감 등)을 따른다
        val access = access(detail(canEdit = false, "엄마", "아빠"), home)

        assertFalse(access.canEdit)
        assertNull(access.family)
        assertEquals(0, home.loads)
    }

    @Test
    fun 가족을_불러오지_못하면_상세는_돌려주고_가족은_비워_둔다() {
        val access = access(detail(canEdit = true, "엄마"), Home(fail = true))

        assertNull(access.family)
        assertNull(access.me)
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
