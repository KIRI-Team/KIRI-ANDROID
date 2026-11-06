package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.record.CalendarEntryKind
import com.kiri.domain.record.RecordCollection
import com.kiri.domain.record.RecordRepository
import com.kiri.domain.record.ScheduleRequest
import com.kiri.domain.record.ScheduleRequestChannel
import com.kiri.domain.schedule.DayOccupyingStatuses
import com.kiri.domain.schedule.ONE_SCHEDULE_PER_DAY_MESSAGE
import com.kiri.domain.schedule.occupiedDates
import com.kiri.domain.time.kiriNow
import com.kiri.feature.record.presentation.schedule.ScheduleRegisterDraft
import com.kiri.feature.record.presentation.schedule.scheduleRequestShareText
import com.kiri.feature.record.presentation.sheet.RecordMember
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleRegisterUiState(
    val earliest: LocalDateTime,
    val loading: Boolean = true,
    val bucketFound: Boolean = false,
    val savedBy: String = "",
    val title: String = "",
    val members: List<RecordMember> = emptyList(),
    val draft: ScheduleRegisterDraft = ScheduleRegisterDraft(),
    val sending: Boolean = false,
    val error: String? = null,
    val weekly: Boolean = false,
    // 이미 일정이 있어 고를 수 없는 날 (하루에 일정은 하나만 정할 수 있다). 기기에 저장된 가족 일정으로 정한다
    val occupiedDates: Set<LocalDate> = emptySet(),
)

sealed interface ScheduleRegisterEffect {
    /** 불러오기가 끝났는데 버킷이 없다(삭제·새로고침으로 사라짐). 빈 화면 대신 빠져나간다 */
    data object BucketMissing : ScheduleRegisterEffect

    data object SentByApp : ScheduleRegisterEffect

    data class SentByKakao(val shareText: String) : ScheduleRegisterEffect
}

// 버킷 하나로 일정 요청을 만드는 화면의 상태. 적던 내용은 SavedStateHandle 에 남겨 프로세스가 다시 떠도 복원한다
@HiltViewModel
class ScheduleRegisterViewModel
@Inject
constructor(
    private val savedState: SavedStateHandle,
    private val repository: RecordRepository,
    private val clock: Clock,
    private val homeRepository: com.kiri.domain.home.HomeRepository? = null,
    private val schedules: com.kiri.domain.schedule.ScheduleRepository? = null,
) : ViewModel() {
    private val bucketId = savedState.get<String>("entryId").orEmpty()
    private val weeklyMissionId = savedState.get<String>("weeklyMissionId")
    private var missionId: String? = null
    private val state =
        MutableStateFlow(ScheduleRegisterUiState(earliest = now(), draft = restoreDraft()))
    val uiState: StateFlow<ScheduleRegisterUiState> = state.asStateFlow()
    private val effects = Channel<ScheduleRegisterEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    private val requestId: String =
        savedState.get<String>("scheduleRequestId")
            ?: java.util.UUID.randomUUID().toString().also { savedState["scheduleRequestId"] = it }
    private var sent = false
    private var missingReported = false

    init {
        if (weeklyMissionId != null) {
            viewModelScope.launch {
                try {
                    val family = requireNotNull(homeRepository).load()
                    schedules?.refresh()
                    val weekly = family.mission?.takeIf {
                        it.id == weeklyMissionId && !it.completed && !it.unavailable && !family.browsing &&
                            schedules?.schedules?.value.orEmpty().none { s ->
                                s.weeklyMissionId == it.id && !s.cancelled
                            }
                    }
                    missionId = weekly?.missionId
                    state.update { it.copy(loading = false, bucketFound = weekly != null, occupiedDates = occupiedDates(),
                        weekly = true, title = weekly?.title.orEmpty(),
                        members = family.members.filter { m -> m.accessible && m.id != family.currentUserId }
                            .map { m -> RecordMember(m.id, m.nickname) }) }
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { state.update { it.copy(loading = false) } }
                reportMissing()
            }
        } else {
            viewModelScope.launch {
                repository.records.collect { records ->
                    apply(records)
                    reportMissing()
                }
            }
            viewModelScope.launch {
                // 기록 탭에서 들어오면 이미 불러와 있다. 앱이 다시 떠서 이 화면부터 열릴 때만 새로 불러온다
                if (!state.value.bucketFound) {
                    try {
                        repository.refresh()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        /* 불러오지 못하면 버킷이 없는 것으로 보고 빠져나간다 */
                    }
                }
                // 위 collect 가 새 값을 받기 전일 수 있으므로 저장소의 지금 값을 직접 반영한 뒤 불러오기를 끝낸다
                apply(repository.records.value)
                state.update { it.copy(loading = false) }
                reportMissing()
            }
        }
    }

    private fun apply(records: RecordCollection) {
        val bucket =
            records.entries.find {
                it.id == bucketId && it.kind == CalendarEntryKind.BUCKET &&
                    !it.completed && it.linkedScheduleIds.isEmpty()
            }
        state.update {
            it.copy(
                bucketFound = bucket != null,
                savedBy =
                    bucket?.let { b -> b.saverId?.let(records.savers::get) ?: "탈퇴한 멤버" }
                        ?: it.savedBy,
                title = bucket?.title ?: it.title,
                occupiedDates = occupiedDates(records),
                members =
                    records.savers
                        .filterKeys { id -> id != records.currentUserId }
                        .map { (id, name) -> RecordMember(id, name) },
            )
        }
    }

    fun onDraftChange(draft: ScheduleRegisterDraft) {
        if (state.value.sending) return
        val normalized = draft.withMessage(draft.message)
        savedState[KEY_DATE] = normalized.date?.toString()
        savedState[KEY_TIME] = normalized.time?.toString()
        savedState[KEY_MEMBERS] = ArrayList(normalized.memberIds)
        savedState[KEY_MESSAGE] = normalized.message
        state.update { it.copy(draft = normalized, error = null) }
    }

    fun send(channel: ScheduleRequestChannel) {
        val current = state.value
        val draft = current.draft
        val date = draft.date ?: return
        val time = draft.time ?: return
        if (current.sending || sent || !current.bucketFound || !draft.canSubmit) return
        if (LocalDateTime.of(date, time).isBefore(now())) {
            state.update { it.copy(error = "지난 시간으로는 요청할 수 없어요. 일정을 다시 골라주세요.") }
            return
        }
        // 하루에는 일정을 하나만 정할 수 있다. 화면을 연 뒤에 생긴 일정도 있을 수 있어 보내기 직전에 다시 확인한다
        val occupied = occupiedDates()
        if (date in occupied) {
            state.update { it.copy(error = ONE_SCHEDULE_PER_DAY_MESSAGE, occupiedDates = occupied) }
            return
        }
        state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            try {
                repository.requestSchedule(
                    ScheduleRequest(
                        bucketId,
                        date,
                        time,
                        draft.memberIds,
                        draft.message,
                        channel,
                        requestId,
                        weeklyMissionId,
                        missionId,
                    )
                )
                sent = true
                val effect =
                    when (channel) {
                        ScheduleRequestChannel.APP -> ScheduleRegisterEffect.SentByApp
                        ScheduleRequestChannel.KAKAO ->
                            ScheduleRegisterEffect.SentByKakao(
                                scheduleRequestShareText(
                                    current.title,
                                    date,
                                    time,
                                    current.members
                                        .filter { it.id in draft.memberIds }
                                        .map { it.name },
                                    draft.message,
                                )
                            )
                    }
                effects.send(effect)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state.update { it.copy(error =
                    if (e.message == ONE_SCHEDULE_PER_DAY_MESSAGE) e.message
                    else if (weeklyMissionId != null && (e is IllegalArgumentException || e is IllegalStateException)) e.message
                    else "요청을 보내지 못했어요. 다시 시도해주세요.") }
            } finally {
                state.update { it.copy(sending = false) }
                // 보내다 실패한 사이에 버킷이 사라졌다면 이제 빠져나간다
                reportMissing()
            }
        }
    }

    // 요청을 보내는 중에는 저장소가 버킷을 먼저 지우고 알려주므로, 보내는 동안과 보낸 뒤에는 "버킷 없음"으로 보지 않는다
    private fun reportMissing() {
        val current = state.value
        if (current.loading || current.bucketFound || current.sending || sent || missingReported)
            return
        missingReported = true
        effects.trySend(ScheduleRegisterEffect.BucketMissing)
    }

    private fun now(): LocalDateTime = clock.kiriNow()

    // 기기에 저장된 가족 일정(대기·재확인·확정·완료) 중 그날을 차지하는 일정의 날짜
    private fun occupiedDates(records: RecordCollection = repository.records.value): Set<LocalDate> =
        schedules?.schedules?.value.orEmpty().occupiedDates(clock.instant()) +
            records.entries
                .filter { it.kind == CalendarEntryKind.SCHEDULE && it.status in DayOccupyingStatuses }
                .map { it.date }

    private fun restoreDraft() =
        ScheduleRegisterDraft(
            date = savedState.get<String>(KEY_DATE)?.let(LocalDate::parse),
            time = savedState.get<String>(KEY_TIME)?.let(LocalTime::parse),
            memberIds = savedState.get<ArrayList<String>>(KEY_MEMBERS).orEmpty().toSet(),
            message = savedState[KEY_MESSAGE] ?: "",
        )

    private companion object {
        const val KEY_DATE = "scheduleDraftDate"
        const val KEY_TIME = "scheduleDraftTime"
        const val KEY_MEMBERS = "scheduleDraftMembers"
        const val KEY_MESSAGE = "scheduleDraftMessage"
    }
}
