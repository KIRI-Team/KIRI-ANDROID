package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import com.kiri.data.home.FakeHomeRepository
import com.kiri.data.mission.FakeMissionRepository
import com.kiri.data.record.FakeRecordRepository
import com.kiri.domain.home.ScheduleStatus
import com.kiri.domain.record.CalendarEntry
import com.kiri.domain.record.RecordRepository
import com.kiri.domain.record.ScheduleRequest
import com.kiri.domain.record.ScheduleRequestChannel
import com.kiri.feature.record.presentation.schedule.ScheduleRegisterDraft
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleRegisterViewModelTest {
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // 서울 시각 2026-10-13 21:00
    private val clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC)

    private fun repository() =
        FakeRecordRepository(
            FakeHomeRepository(clock, latencyMillis = 0),
            FakeMissionRepository(),
            clock = clock,
        )

    private fun handle(entryId: String = "record-bucket") =
        SavedStateHandle(mapOf("entryId" to entryId))

    private val filled =
        ScheduleRegisterDraft(
            LocalDate.of(2026, 10, 20),
            LocalTime.of(14, 0),
            setOf("mother", "father"),
            "같이 가요",
        )

    @Test
    fun loadsBucketSaverAndMembersFromRepository() = runTest {
        val repo = repository()
        val vm = ScheduleRegisterViewModel(handle(), repo, clock)
        val state = vm.uiState.value
        assertFalse(state.loading)
        assertTrue(state.bucketFound)
        assertEquals(repo.records.value.savers["father"], state.savedBy)
        assertEquals("한강 뚜벅뚜벅 축제 가보기", state.title)
        assertTrue(
            state.members.any {
                it.id == "father" && it.name == repo.records.value.savers["father"]
            }
        )
        assertFalse(state.members.any { it.id == repo.records.value.currentUserId })
        assertEquals(LocalDateTime.of(2026, 10, 13, 21, 0), state.earliest)
    }

    @Test
    fun draftIsNormalizedAndRestoredAfterRecreation() {
        val handle = handle()
        val vm = ScheduleRegisterViewModel(handle, repository(), clock)
        vm.onDraftChange(filled.copy(message = "가".repeat(120)))
        assertEquals(100, vm.uiState.value.draft.message.length)

        val restored = ScheduleRegisterViewModel(handle, repository(), clock)
        assertEquals(vm.uiState.value.draft, restored.uiState.value.draft)
    }

    @Test
    fun appSendCreatesPendingScheduleAndSignalsOnce() = runTest {
        val repo = repository()
        val vm = ScheduleRegisterViewModel(handle(), repo, clock)
        vm.onDraftChange(filled)

        vm.send(ScheduleRequestChannel.APP)

        assertEquals(ScheduleRegisterEffect.SentByApp, vm.effect.first())
        val created =
            repo.records.value.entries.single { it.status == ScheduleStatus.PENDING_RESPONSE }
        assertEquals(LocalDate.of(2026, 10, 20), created.date)
        // 버킷이 목록에서 빠져도 이미 보냈으므로 "버킷 없음"으로 빠져나가지 않고, 다시 보내지도 않는다
        assertFalse(repo.records.value.entries.any { it.id == "record-bucket" })
        vm.send(ScheduleRequestChannel.APP)
        assertEquals(
            1,
            repo.records.value.entries.count { it.status == ScheduleStatus.PENDING_RESPONSE },
        )
        assertNull(vm.effect.tryReceiveFirst())
    }

    @Test
    fun kakaoSendAlsoCreatesScheduleAndCarriesShareText() = runTest {
        val repo = repository()
        val vm = ScheduleRegisterViewModel(handle(), repo, clock)
        vm.onDraftChange(filled)

        vm.send(ScheduleRequestChannel.KAKAO)

        val effect = vm.effect.first() as ScheduleRegisterEffect.SentByKakao
        assertTrue(effect.shareText.contains("한강 뚜벅뚜벅 축제 가보기"))
        assertTrue(effect.shareText.contains("같이 가요"))
        assertTrue(repo.records.value.entries.any { it.status == ScheduleStatus.PENDING_RESPONSE })
    }

    @Test
    fun sendIsIgnoredUntilDraftIsCompleteAndRejectsPastTime() {
        val repo = repository()
        val vm = ScheduleRegisterViewModel(handle(), repo, clock)
        vm.onDraftChange(filled.copy(memberIds = emptySet()))
        vm.send(ScheduleRequestChannel.APP)
        assertTrue(repo.records.value.entries.any { it.id == "record-bucket" })

        // 오늘 21:00 보다 이른 시각
        vm.onDraftChange(
            filled.copy(date = LocalDate.of(2026, 10, 13), time = LocalTime.of(20, 30))
        )
        vm.send(ScheduleRequestChannel.APP)
        assertNotNull(vm.uiState.value.error)
        assertTrue(repo.records.value.entries.any { it.id == "record-bucket" })
        // 다시 고르면 오류가 지워진다
        vm.onDraftChange(filled)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun failedSendShowsErrorAndAllowsRetry() = runTest {
        val real = repository()
        var fail = true
        val flaky =
            object : RecordRepository by real {
                override suspend fun requestSchedule(request: ScheduleRequest): CalendarEntry =
                    if (fail) error("network") else real.requestSchedule(request)
            }
        val vm = ScheduleRegisterViewModel(handle(), flaky, clock)
        vm.onDraftChange(filled)

        vm.send(ScheduleRequestChannel.APP)
        assertEquals("요청을 보내지 못했어요. 다시 시도해주세요.", vm.uiState.value.error)
        assertFalse(vm.uiState.value.sending)

        fail = false
        vm.send(ScheduleRequestChannel.APP)
        assertEquals(ScheduleRegisterEffect.SentByApp, vm.effect.first())
    }

    @Test
    fun ignoresDoubleSendWhileRequestIsInFlight() = runTest {
        val real = repository()
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        val slow =
            object : RecordRepository by real {
                override suspend fun requestSchedule(request: ScheduleRequest): CalendarEntry {
                    calls++
                    gate.await()
                    return real.requestSchedule(request)
                }
            }
        val vm = ScheduleRegisterViewModel(handle(), slow, clock)
        vm.onDraftChange(filled)

        vm.send(ScheduleRequestChannel.APP)
        assertTrue(vm.uiState.value.sending)
        vm.send(ScheduleRequestChannel.KAKAO)
        // 보내는 중에는 적던 내용도 바뀌지 않는다
        vm.onDraftChange(filled.copy(message = "바뀐 말"))
        assertEquals("같이 가요", vm.uiState.value.draft.message)

        gate.complete(Unit)
        assertEquals(ScheduleRegisterEffect.SentByApp, vm.effect.first())
        assertEquals(1, calls)
    }

    @Test
    fun missingBucketSignalsLeaveOnlyAfterLoading() = runTest {
        val vm = ScheduleRegisterViewModel(handle("no-such-bucket"), repository(), clock)
        assertFalse(vm.uiState.value.bucketFound)
        assertEquals(ScheduleRegisterEffect.BucketMissing, vm.effect.first())
        assertNull(vm.effect.tryReceiveFirst())
    }

    @Test
    fun completedOrLinkedBucketCannotBeRegisteredThroughDirectNavigation() = runTest {
        val real = repository()
        real.refresh()
        for (completed in listOf(true, false)) {
            var calls = 0
            val entries = real.records.value.entries.map {
                if (it.id == "record-bucket") it.copy(completed = completed,
                    linkedScheduleIds = if (completed) emptySet() else setOf("active")) else it
            }
            val records = kotlinx.coroutines.flow.MutableStateFlow(real.records.value.copy(entries = entries))
            val guarded = object : RecordRepository by real {
                override val records = records
                override suspend fun refresh() {}
                override suspend fun requestSchedule(request: ScheduleRequest): CalendarEntry {
                    calls++
                    error("must not submit")
                }
            }
            val vm = ScheduleRegisterViewModel(handle(), guarded, clock)
            assertFalse(vm.uiState.value.bucketFound)
            assertEquals(ScheduleRegisterEffect.BucketMissing, vm.effect.first())
            vm.onDraftChange(filled)
            vm.send(ScheduleRequestChannel.APP)
            assertEquals(0, calls)
        }
    }

    @Test
    fun bucketRemovedWhileOpenSignalsLeaveOnce() = runTest {
        val repo = repository()
        val vm = ScheduleRegisterViewModel(handle(), repo, clock)
        assertTrue(vm.uiState.value.bucketFound)

        // 다른 화면(같은 가족의 다른 기기 등)에서 이 버킷으로 먼저 일정을 잡았다
        repo.requestSchedule(
            ScheduleRequest(
                "record-bucket",
                LocalDate.of(2026, 10, 20),
                LocalTime.of(14, 0),
                setOf("mother"),
                "",
                ScheduleRequestChannel.APP,
            )
        )

        assertFalse(vm.uiState.value.bucketFound)
        assertEquals(ScheduleRegisterEffect.BucketMissing, vm.effect.first())
        assertNull(vm.effect.tryReceiveFirst())
    }

    // 이미 보낸 신호 말고 더 쌓인 신호가 없는지 확인한다
    private suspend fun kotlinx.coroutines.flow.Flow<ScheduleRegisterEffect>.tryReceiveFirst():
        ScheduleRegisterEffect? = kotlinx.coroutines.withTimeoutOrNull(50) { first() }
}
