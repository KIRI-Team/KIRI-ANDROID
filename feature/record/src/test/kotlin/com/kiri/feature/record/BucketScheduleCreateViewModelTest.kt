package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.domain.model.schedule.BucketNotSchedulableException
import com.kiri.domain.model.schedule.CalendarSchedule
import com.kiri.domain.model.schedule.CalendarScheduleStatus
import com.kiri.domain.model.schedule.InvalidScheduleUpdateException
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.model.schedule.ScheduleUpdate
import com.kiri.domain.record.ScheduleRequestChannel
import com.kiri.domain.repository.CalendarScheduleRepository
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 서버 버킷의 일정 잡기가 가족을 불러오고, 일정 생성 API 로 일정을 만들어 보내는지 확인한다
@OptIn(ExperimentalCoroutinesApi::class)
class BucketScheduleCreateViewModelTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-05T03:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun teardown() = Dispatchers.resetMain()

    private class FakeSchedules(var create: Result<Long> = Result.success(100)) : CalendarScheduleRepository {
        val created = mutableListOf<Pair<Long, ScheduleUpdate>>()
        val monthly = mutableMapOf<YearMonth, Result<List<CalendarSchedule>>>()
        val requestedMonths = mutableListOf<YearMonth>()

        override suspend fun getMonthlySchedules(month: YearMonth): Result<List<CalendarSchedule>> {
            requestedMonths += month
            return monthly[month] ?: Result.success(emptyList())
        }
        override suspend fun getScheduleDetail(scheduleId: Long): Result<ScheduleDetail> = Result.failure(ScheduleNotFoundException())
        override suspend fun updateSchedule(scheduleId: Long, update: ScheduleUpdate): Result<Unit> = Result.success(Unit)
        override suspend fun createSchedule(bucketItemId: Long, form: ScheduleUpdate): Result<Long> {
            created += bucketItemId to form
            return create
        }
    }

    // 나(1, 엄마), 첫째(2), 아빠(3)
    private val family = FakeFamily("엄마", listOf("2" to "첫째", "3" to "아빠"))

    private fun viewModel(repo: CalendarScheduleRepository, home: FakeFamily = family) =
        BucketScheduleCreateViewModel(
            SavedStateHandle(
                mapOf(
                    BucketScheduleCreateViewModel.BUCKET_ITEM_ID_ARG to 42L,
                    BucketScheduleCreateViewModel.TITLE_ARG to "광장시장에서 시장음식 먹어보기",
                    BucketScheduleCreateViewModel.SAVER_ARG to "아빠",
                ),
            ),
            repo,
            home,
            clock,
        )

    private fun BucketScheduleCreateViewModel.fill(members: Set<String> = setOf("2", "3"), message: String = "같이 가요") =
        onDraftChange(
            uiState.value.draft.copy(
                date = LocalDate.of(2026, 11, 15),
                time = LocalTime.of(14, 0),
                memberIds = members,
                message = message,
            ),
        )

    @Test
    fun 열면_버킷_제목과_저장한_사람을_보여주고_나를_뺀_가족을_불러온다() {
        val state = viewModel(FakeSchedules()).uiState.value

        assertFalse(state.loading)
        assertNull(state.loadError)
        assertEquals("광장시장에서 시장음식 먹어보기", state.title)
        assertEquals("아빠", state.savedBy)
        assertEquals(listOf("2", "3"), state.members.map { it.id })
    }

    @Test
    fun 앱으로_보내면_고른_값으로_일정을_만든다() = runTest {
        val repo = FakeSchedules()
        val vm = viewModel(repo)

        vm.fill()
        vm.send(ScheduleRequestChannel.APP)

        assertEquals(BucketScheduleCreateEffect.SentByApp, vm.effect.first())
        assertEquals(
            listOf(42L to ScheduleUpdate(LocalDate.of(2026, 11, 15), LocalTime.of(14, 0), setOf(2L, 3L), "같이 가요")),
            repo.created,
        )
        assertFalse(vm.uiState.value.sending)
    }

    @Test
    fun 카카오톡으로_보내면_일정을_만든_뒤_공유_문구를_넘긴다() = runTest {
        val repo = FakeSchedules()
        val vm = viewModel(repo)

        vm.fill(members = setOf("3"))
        vm.send(ScheduleRequestChannel.KAKAO)

        val effect = vm.effect.first() as BucketScheduleCreateEffect.SentByKakao
        assertTrue(effect.shareText.contains("광장시장에서 시장음식 먹어보기"))
        assertTrue(effect.shareText.contains("아빠"))
        assertEquals(1, repo.created.size)
    }

    @Test
    fun 한_줄이_비어_있으면_보내지_않고_나나_모르는_가족은_고를_수_없다() {
        val repo = FakeSchedules()
        val vm = viewModel(repo)

        vm.fill(members = setOf("1", "99"))
        assertTrue(vm.uiState.value.draft.memberIds.isEmpty())
        vm.send(ScheduleRequestChannel.APP)
        assertTrue(repo.created.isEmpty())

        vm.fill(members = setOf("2"), message = "  ")
        vm.send(ScheduleRequestChannel.APP)
        assertNull(repo.created.single().second.message)
    }

    @Test
    fun 만들지_못하면_이유에_맞는_문구를_보여준다() {
        listOf(
            BucketNotSchedulableException() to "이미 일정이 잡혔거나 일정을 잡을 수 없는 버킷이에요.",
            com.kiri.domain.model.bucket.BucketItemNotFoundException() to "찾을 수 없는 버킷이에요.",
            InvalidScheduleUpdateException("잘못된 요청입니다.") to "날짜·시간과 함께할 가족을 다시 확인해주세요.",
            IOException() to "일정을 만들지 못했어요. 다시 시도해주세요.",
        ).forEach { (error, message) ->
            val vm = viewModel(FakeSchedules(create = Result.failure(error)))

            vm.fill()
            vm.send(ScheduleRequestChannel.APP)

            assertEquals(message, vm.uiState.value.error)
            assertFalse(vm.uiState.value.sending)
        }
    }

    @Test
    fun 가족을_불러오지_못하면_오류를_보여주고_다시_시도할_수_있다() {
        val vm = viewModel(FakeSchedules(), FakeFamily(null))
        assertEquals("가족 정보를 불러오지 못했어요.", vm.uiState.value.loadError)
        assertFalse(vm.uiState.value.loading)
    }

    private fun schedule(id: Long, date: LocalDate, status: CalendarScheduleStatus) =
        CalendarSchedule(id, date, LocalTime.of(10, 0), status, null)

    @Test
    fun 날짜_시트가_보여준_달에서_대기_확정_완료_일정이_있는_날은_고를_수_없다() {
        val repo = FakeSchedules()
        val november = YearMonth.of(2026, 11)
        repo.monthly[november] = Result.success(
            listOf(
                schedule(1, LocalDate.of(2026, 11, 10), CalendarScheduleStatus.PENDING_RESPONSE),
                schedule(2, LocalDate.of(2026, 11, 11), CalendarScheduleStatus.CONFIRMED),
                schedule(3, LocalDate.of(2026, 11, 12), CalendarScheduleStatus.COMPLETED),
                // 취소되었거나 미확정으로 끝난 일정은 그날을 차지하지 않는다
                schedule(4, LocalDate.of(2026, 11, 13), CalendarScheduleStatus.CANCELLED),
                schedule(5, LocalDate.of(2026, 11, 14), CalendarScheduleStatus.NOT_CONFIRMED),
            ),
        )
        val vm = viewModel(repo)

        vm.onMonthShown(november)
        vm.onMonthShown(november)

        assertEquals((10..12).map { LocalDate.of(2026, 11, it) }.toSet(), vm.uiState.value.occupiedDates)
        // 이미 받은 달은 다시 요청하지 않는다
        assertEquals(listOf(november), repo.requestedMonths)
    }

    @Test
    fun 이미_일정이_있는_날로는_보내지_않고_안내한다() = runTest {
        val repo = FakeSchedules()
        // 날짜 시트를 열지 않았어도 보내기 직전에 그 달을 받아 확인한다
        repo.monthly[YearMonth.of(2026, 11)] =
            Result.success(listOf(schedule(1, LocalDate.of(2026, 11, 15), CalendarScheduleStatus.CONFIRMED)))
        val vm = viewModel(repo)

        vm.fill()
        vm.send(ScheduleRequestChannel.APP)

        assertTrue(repo.created.isEmpty())
        assertFalse(vm.uiState.value.sending)
        assertEquals("이미 약속이 있는 날이에요. 다른 날짜를 골라주세요.", vm.uiState.value.error)
        assertTrue(LocalDate.of(2026, 11, 15) in vm.uiState.value.occupiedDates)
    }

    @Test
    fun 그_달의_일정을_불러오지_못하면_막지_않고_서버에_맡긴다() = runTest {
        val repo = FakeSchedules()
        repo.monthly[YearMonth.of(2026, 11)] = Result.failure(IOException())
        val vm = viewModel(repo)

        vm.fill()
        vm.send(ScheduleRequestChannel.APP)

        assertEquals(BucketScheduleCreateEffect.SentByApp, vm.effect.first())
        assertEquals(1, repo.created.size)
    }
}
