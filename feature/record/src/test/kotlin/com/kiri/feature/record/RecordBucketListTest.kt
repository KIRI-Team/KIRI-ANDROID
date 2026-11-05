package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.data.bucket.FakeBucketItemRepository
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.record.FakeRecordRepository
import com.kiri.domain.model.bucket.BucketItemStatus
import com.kiri.feature.record.model.RecordUiEffect
import com.kiri.feature.record.model.RecordUiEvent
import com.kiri.feature.record.presentation.bucket.options
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// 기록 탭의 버킷리스트가 서버의 가족 버킷리스트를 불러오고, 구성원 필터와 다음 페이지를 다루는지 확인한다
@OptIn(ExperimentalCoroutinesApi::class)
class RecordBucketListTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-13T03:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setup() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun records() = FakeRecordRepository(FakeHomeRepository(clock, latencyMillis = 0), FakeMissionRepository(), clock = clock)

    private fun viewModel(buckets: FakeBucketItemRepository, handle: SavedStateHandle = SavedStateHandle()) =
        RecordViewModel(handle, records(), clock, bucketItems = buckets)

    @Test
    fun 처음_열면_전체_버킷과_구성원별_저장_개수를_불러온다() {
        val repo = FakeBucketItemRepository()

        val state = viewModel(repo).uiState.value

        assertEquals(listOf(Triple(null, null, 20)), repo.requests)
        assertEquals(FakeBucketItemRepository.DemoSavers, state.bucketSavers)
        assertEquals(listOf(12L, 11L, 10L), state.bucketItems.map { it.id })
        assertFalse(state.bucketHasNext)
        assertFalse(state.bucketLoading)
        assertNull(state.bucketError)
    }

    @Test
    fun 구성원을_고르면_그_구성원_것만_다시_불러오고_전체를_고르면_필터를_뺀다() {
        val repo = FakeBucketItemRepository()
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.SaverSelected("2"))
        assertEquals("2", vm.uiState.value.selectedSaverId)
        assertEquals(listOf(12L, 10L), vm.uiState.value.bucketItems.map { it.id })
        // 칩 개수는 필터와 상관없이 가족 전체 기준 그대로다
        assertEquals(FakeBucketItemRepository.DemoSavers, vm.uiState.value.bucketSavers)

        vm.onEvent(RecordUiEvent.SaverSelected(null))
        assertEquals(listOf(12L, 11L, 10L), vm.uiState.value.bucketItems.map { it.id })
        assertEquals(listOf<Long?>(null, 2L, null), repo.requests.map { it.first })
    }

    @Test
    fun 가족에_없는_구성원이나_같은_칩은_다시_요청하지_않는다() {
        val repo = FakeBucketItemRepository()
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.SaverSelected("99"))
        vm.onEvent(RecordUiEvent.SaverSelected(null))

        assertNull(vm.uiState.value.selectedSaverId)
        assertEquals(1, repo.requests.size)
    }

    @Test
    fun 끝에_가까워지면_다음_페이지를_이어_붙이고_마지막이면_더_요청하지_않는다() {
        val many = (1L..25L).map { FakeBucketItemRepository.item(it, 1, "엄마") }
        val repo = FakeBucketItemRepository(items = many)
        val vm = viewModel(repo)
        assertEquals(20, vm.uiState.value.bucketItems.size)
        assertTrue(vm.uiState.value.bucketHasNext)

        vm.onEvent(RecordUiEvent.BucketListEndReached)

        assertEquals((25L downTo 1L).toList(), vm.uiState.value.bucketItems.map { it.id })
        assertFalse(vm.uiState.value.bucketHasNext)
        assertEquals(Triple(null, 6L, 20), repo.requests.last())

        vm.onEvent(RecordUiEvent.BucketListEndReached)
        assertEquals(2, repo.requests.size)
    }

    @Test
    fun 불러오지_못하면_오류를_보여주고_다시_시도하면_새로_불러온다() {
        val repo = FakeBucketItemRepository(failure = IOException())
        val vm = viewModel(repo)
        assertEquals("버킷리스트를 불러오지 못했어요.", vm.uiState.value.bucketError)
        assertTrue(vm.uiState.value.bucketItems.isEmpty())

        repo.failure = null
        vm.onEvent(RecordUiEvent.BucketListRetryClicked)

        assertNull(vm.uiState.value.bucketError)
        assertEquals(3, vm.uiState.value.bucketItems.size)
    }

    @Test
    fun 다음_페이지를_불러오지_못해도_받은_항목은_남기고_다시_시도하면_이어서_받는다() {
        val many = (1L..25L).map { FakeBucketItemRepository.item(it, 1, "엄마") }
        val repo = FakeBucketItemRepository(items = many)
        val vm = viewModel(repo)

        repo.failure = IOException()
        vm.onEvent(RecordUiEvent.BucketListEndReached)
        assertEquals(20, vm.uiState.value.bucketItems.size)
        assertEquals("버킷리스트를 불러오지 못했어요.", vm.uiState.value.bucketError)

        repo.failure = null
        vm.onEvent(RecordUiEvent.BucketListRetryClicked)
        assertEquals(25, vm.uiState.value.bucketItems.size)
        assertEquals(6L, repo.requests.last().second)
    }

    @Test
    fun 고른_구성원이_가족에서_빠지면_전체로_돌린다() {
        val repo = FakeBucketItemRepository()
        // 예전에 고른 3번 구성원은 지금 가족(1, 2)에 없다
        val vm = viewModel(repo, SavedStateHandle(mapOf("recordSaver" to "3")))

        assertNull(vm.uiState.value.selectedSaverId)
        assertEquals(listOf<Long?>(3L, null), repo.requests.map { it.first })
        assertEquals(3, vm.uiState.value.bucketItems.size)
    }

    @Test
    fun 예전_기기_저장_구성원_id_는_버린다() {
        val repo = FakeBucketItemRepository()

        val vm = viewModel(repo, SavedStateHandle(mapOf("recordSaver" to "father")))

        assertNull(vm.uiState.value.selectedSaverId)
        assertNull(repo.requests.single().first)
    }

    @Test
    fun 카드를_누르면_그_버킷의_시트를_연다() = runTest {
        val vm = viewModel(FakeBucketItemRepository())

        vm.onEvent(RecordUiEvent.BucketClicked("12"))
        assertEquals(RecordUiEffect.OpenBucket("12"), vm.effect.first())

        vm.onEvent(RecordUiEvent.BucketClicked("99"))
        assertNull(withTimeoutOrNull(100) { vm.effect.first() })
    }

    @Test
    fun 삭제하면_목록에서_빼고_칩_개수를_줄이고_안내를_띄운다() = runTest {
        val repo = FakeBucketItemRepository()
        val vm = viewModel(repo)

        vm.onEvent(RecordUiEvent.BucketDeleteConfirmed("12"))

        assertEquals(RecordUiEffect.ShowNotice("버킷리스트에서 삭제했어요."), vm.effect.first())
        assertEquals(listOf(12L), repo.deleted)
        assertEquals(listOf(11L, 10L), vm.uiState.value.bucketItems.map { it.id })
        assertEquals(1, vm.uiState.value.bucketSavers.first { it.memberId == 2L }.bucketCount)
        assertNull(vm.uiState.value.bucketDeletingId)
        // 다시 불러오지 않고 받은 페이지를 그대로 쓴다
        assertEquals(1, repo.requests.size)
    }

    @Test
    fun 지울_수_없는_버킷이나_목록에_없는_버킷은_요청하지_않는다() {
        val repo = FakeBucketItemRepository()
        val vm = viewModel(repo)

        // 11 은 완료, 10 은 일정이 잡혀 deletable 이 false 다
        vm.onEvent(RecordUiEvent.BucketDeleteConfirmed("11"))
        vm.onEvent(RecordUiEvent.BucketDeleteConfirmed("10"))
        vm.onEvent(RecordUiEvent.BucketDeleteConfirmed("99"))

        assertTrue(repo.deleted.isEmpty())
    }

    @Test
    fun 삭제에_실패하면_이유를_알려주고_상태가_바뀐_버킷이면_목록을_다시_불러온다() = runTest {
        listOf(
            com.kiri.domain.model.bucket.ScheduledBucketItemException() to "일정이 잡힌 버킷은 일정을 취소한 뒤 삭제할 수 있어요.",
            com.kiri.domain.model.bucket.CompletedBucketItemException() to "완료한 버킷은 삭제할 수 없어요.",
            com.kiri.domain.model.bucket.NotBucketItemOwnerException() to "내가 저장한 버킷만 삭제할 수 있어요.",
            com.kiri.domain.model.bucket.BucketItemNotFoundException() to "이미 삭제됐거나 찾을 수 없는 버킷이에요.",
        ).forEach { (error, message) ->
            val repo = FakeBucketItemRepository().apply { deleteFailure = error }
            val vm = viewModel(repo)

            vm.onEvent(RecordUiEvent.BucketDeleteConfirmed("12"))

            assertEquals(RecordUiEffect.ShowNotice(message), vm.effect.first())
            assertNull(vm.uiState.value.bucketDeletingId)
            assertEquals(2, repo.requests.size)
        }

        // 연결 오류는 목록을 그대로 두고 다시 시도하게 한다
        val repo = FakeBucketItemRepository().apply { deleteFailure = IOException() }
        val vm = viewModel(repo)
        vm.onEvent(RecordUiEvent.BucketDeleteConfirmed("12"))
        assertEquals(RecordUiEffect.ShowNotice("버킷을 삭제하지 못했어요. 다시 시도해주세요."), vm.effect.first())
        assertEquals(3, vm.uiState.value.bucketItems.size)
        assertEquals(1, repo.requests.size)
    }

    @Test
    fun 시트_버튼은_서버가_허락한_동작만_켠다() {
        fun item(status: BucketItemStatus, schedulable: Boolean = false, deletable: Boolean = false, scheduleId: Long? = null) =
            FakeBucketItemRepository.item(1, 1, "엄마", status, schedulable = schedulable, deletable = deletable, scheduleId = scheduleId)

        val saved = item(BucketItemStatus.SAVED, schedulable = true, deletable = true).options()
        assertEquals("일정 잡기", saved.scheduleLabel)
        assertTrue(saved.scheduleEnabled)
        assertTrue(saved.deleteEnabled)
        assertNull(saved.linkedScheduleId)

        // 미션이 끝났거나 다른 가족이 저장한 버킷
        val locked = item(BucketItemStatus.SAVED).options()
        assertFalse(locked.scheduleEnabled)
        assertFalse(locked.deleteEnabled)

        val scheduled = item(BucketItemStatus.SCHEDULED, scheduleId = 5).options()
        assertEquals("연결된 약속 보기", scheduled.scheduleLabel)
        assertTrue(scheduled.scheduleEnabled)
        assertEquals(5L, scheduled.linkedScheduleId)
        // 일정 기능이 연결되기 전에는 연결된 일정 id 가 없어 열 수 없다
        assertFalse(item(BucketItemStatus.SCHEDULED).options().scheduleEnabled)

        val completed = item(BucketItemStatus.COMPLETED).options()
        assertEquals("완료한 활동", completed.scheduleLabel)
        assertFalse(completed.scheduleEnabled)
    }
}
