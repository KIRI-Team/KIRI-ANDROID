package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.HomeRepository
import com.kiri.domain.home.invitableMembers
import com.kiri.domain.model.bucket.BucketItemNotFoundException
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.schedule.BucketNotSchedulableException
import com.kiri.domain.model.schedule.InvalidScheduleUpdateException
import com.kiri.domain.model.schedule.ScheduleUpdate
import com.kiri.domain.record.ScheduleRequestChannel
import com.kiri.domain.repository.CalendarScheduleRepository
import com.kiri.domain.time.kiriNow
import com.kiri.feature.record.presentation.schedule.ScheduleRegisterDraft
import com.kiri.feature.record.presentation.schedule.scheduleRequestShareText
import com.kiri.feature.record.presentation.sheet.RecordMember
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BucketScheduleCreateUiState(
    val earliest: LocalDateTime,
    val title: String,
    // 버킷을 저장한 사람의 닉네임 ("OO가 저장했어요!")
    val savedBy: String,
    val loading: Boolean = true,
    // 가족 정보를 불러오지 못했을 때의 문구. 있으면 입력 화면 대신 보여준다
    val loadError: String? = null,
    // 초대할 수 있는 가족 (나는 뺀다)
    val members: List<RecordMember> = emptyList(),
    val draft: ScheduleRegisterDraft = ScheduleRegisterDraft(),
    val sending: Boolean = false,
    val error: String? = null,
    // 이미 일정이 있어 고를 수 없는 날 (하루에 일정은 하나만 정할 수 있다). 날짜 시트가 보여준 달만 채워진다
    val occupiedDates: Set<LocalDate> = emptySet(),
)

sealed interface BucketScheduleCreateEffect {
    data object SentByApp : BucketScheduleCreateEffect

    data class SentByKakao(val shareText: String) : BucketScheduleCreateEffect
}

// 서버 버킷의 일정 잡기. 일정 등록 화면으로 날짜·시간·가족·한 줄을 고르고, 일정 생성 API 로 일정을 만들어 초대를 보낸다.
// 카카오톡으로 보내기를 고르면 일정을 만든 뒤 공유 시트로 요청 문구도 보낸다
@HiltViewModel
class BucketScheduleCreateViewModel
@Inject
constructor(
    private val savedState: SavedStateHandle,
    private val schedules: CalendarScheduleRepository,
    private val home: HomeRepository,
    private val clock: Clock,
) : ViewModel() {
    private val weeklyMissionId = savedState.get<String>("weeklyMissionId")?.toLongOrNull()
    private var sent = savedState.get<Long>("createdScheduleId") != null
    private val bucketItemId = savedState.get<Long>(BUCKET_ITEM_ID_ARG)
    private val state = MutableStateFlow(
        BucketScheduleCreateUiState(
            earliest = clock.kiriNow(),
            title = savedState.get<String>(TITLE_ARG).orEmpty(),
            savedBy = savedState.get<String>(SAVER_ARG).orEmpty(),
            draft = ScheduleRegisterDraft(
                date = savedState.get<String>("serverDraftDate")?.let(LocalDate::parse),
                time = savedState.get<String>("serverDraftTime")?.let(LocalTime::parse),
                memberIds = savedState.get<ArrayList<String>>("serverDraftMembers").orEmpty().toSet(),
                message = savedState["serverDraftMessage"] ?: ""),
        ),
    )
    val uiState = state.asStateFlow()
    private val effects = Channel<BucketScheduleCreateEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()
    private val dayLimit = ScheduleDayLimit(schedules)

    init {
        load()
    }

    // 날짜 시트가 보여주는 달의 가족 일정을 불러와 일정이 있는 날을 고를 수 없게 한다
    // 불러오지 못하면 막지 않고 보내기 직전에 다시 확인한다
    fun onMonthShown(month: YearMonth) {
        viewModelScope.launch {
            if (dayLimit.load(month)) state.update { it.copy(occupiedDates = dayLimit.occupiedDates) }
        }
    }

    fun retry() {
        if (!uiState.value.loading) load()
    }

    fun onDraftChange(draft: ScheduleRegisterDraft) {
        if (uiState.value.sending || sent) return
        state.update { current ->
            // 한 줄은 100자까지, 구성원은 초대할 수 있는 가족 안에서만 고른다
            val known = current.members.map { it.id }.toSet()
            val normalized = draft.withMessage(draft.message).copy(memberIds = draft.memberIds intersect known)
            savedState["serverDraftDate"] = normalized.date?.toString()
            savedState["serverDraftTime"] = normalized.time?.toString()
            savedState["serverDraftMembers"] = ArrayList(normalized.memberIds)
            savedState["serverDraftMessage"] = normalized.message
            current.copy(draft = normalized, error = null)
        }
    }

    fun send(channel: ScheduleRequestChannel) {
        val current = uiState.value
        val id = weeklyMissionId ?: bucketItemId ?: return
        val draft = current.draft
        val date = draft.date
        val time = draft.time
        if (current.sending || sent || !draft.canSubmit || date == null || time == null) return
        if (!LocalDateTime.of(date, time).isAfter(clock.kiriNow())) {
            state.update { it.copy(error = "지난 시각으로는 일정을 만들 수 없어요.") }; return
        }
        val inviteeIds = draft.memberIds.mapNotNull { it.toLongOrNull() }.toSet()
        if (inviteeIds.size != draft.memberIds.size) {
            state.update { it.copy(error = "가족 정보를 다시 불러온 뒤 시도해주세요.") }
            return
        }
        state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            // 하루에 일정은 하나만 정할 수 있다. 그 달을 아직 받지 못했으면 지금 받아 확인한다
            dayLimit.load(YearMonth.from(date))
            if (date in dayLimit.occupiedDates) {
                state.update {
                    it.copy(sending = false, occupiedDates = dayLimit.occupiedDates, error = ScheduleDayLimit.OCCUPIED_MESSAGE)
                }
                return@launch
            }
            val form = ScheduleUpdate(date, time, inviteeIds, draft.message.takeIf { it.isNotBlank() })
            val result = if (weeklyMissionId != null) schedules.createWeeklySchedule(id, form) else schedules.createSchedule(id, form)
            result
                .onSuccess {
                    savedState["createdScheduleId"] = it
                    sent = true
                    state.update { it.copy(sending = false) }
                    effects.send(
                        when (channel) {
                            ScheduleRequestChannel.APP -> BucketScheduleCreateEffect.SentByApp
                            ScheduleRequestChannel.KAKAO -> BucketScheduleCreateEffect.SentByKakao(
                                scheduleRequestShareText(
                                    current.title,
                                    date,
                                    time,
                                    current.members.filter { it.id in draft.memberIds }.map { it.name },
                                    draft.message,
                                ),
                            )
                        },
                    )
                }
                .onFailure { error -> state.update { it.copy(sending = false, error = error.createMessage()) } }
        }
    }

    private fun load() {
        if (sent) {
            state.update { it.copy(loading = false) }
            effects.trySend(BucketScheduleCreateEffect.SentByApp)
            return
        }
        if (bucketItemId == null && weeklyMissionId == null) {
            state.update { it.copy(loading = false, loadError = "찾을 수 없는 버킷이에요.") }
            return
        }
        state.update { it.copy(loading = true, loadError = null, earliest = clock.kiriNow()) }
        viewModelScope.launch {
            try {
                val family = home.load()
                check(!family.browsing) { "가족 연결 후 사용할 수 있어요." }
                if (weeklyMissionId != null) {
                    val weekly = requireNotNull(family.mission?.takeIf { it.missionId == weeklyMissionId.toString() && !it.unavailable && !it.completed })
                    state.update { it.copy(title = weekly.title) }
                }
                state.update {
                    it.copy(loading = false, members = family.invitableMembers().map { m -> RecordMember(m.id, m.nickname) })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                state.update { it.copy(loading = false, loadError = "가족 정보를 불러오지 못했어요.") }
            }
        }
    }

    private fun Throwable.createMessage() = when (this) {
        is BucketNotSchedulableException -> "이미 일정이 잡혔거나 일정을 잡을 수 없는 버킷이에요."
        is BucketItemNotFoundException -> "찾을 수 없는 버킷이에요."
        is NoFamilyException -> "가족 공간이 없어요."
        is InvalidScheduleUpdateException -> "날짜·시간과 함께할 가족을 다시 확인해주세요."
        else -> "일정을 만들지 못했어요. 다시 시도해주세요."
    }

    companion object {
        const val BUCKET_ITEM_ID_ARG = "bucketItemId"
        const val TITLE_ARG = "title"
        const val SAVER_ARG = "saver"
    }
}
