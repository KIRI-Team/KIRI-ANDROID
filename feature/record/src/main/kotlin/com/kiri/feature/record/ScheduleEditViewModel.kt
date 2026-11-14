package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.model.schedule.InvalidScheduleUpdateException
import com.kiri.domain.model.schedule.NotScheduleProposerException
import com.kiri.domain.model.schedule.ScheduleNotEditableException
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.model.schedule.ScheduleUpdate
import com.kiri.domain.model.schedule.canBeEditedBeforeTheDay
import com.kiri.domain.usecase.schedule.GetScheduleEditAccessUseCase
import com.kiri.domain.repository.CalendarScheduleRepository
import com.kiri.domain.time.kiriNow
import com.kiri.feature.record.presentation.schedule.ScheduleRegisterDraft
import com.kiri.feature.record.presentation.sheet.RecordMember
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleEditUiState(
    val earliest: LocalDateTime,
    val loading: Boolean = true,
    // 일정·가족 정보를 불러오지 못했을 때의 문구. 있으면 입력 화면 대신 보여준다
    val loadError: String? = null,
    val title: String = "",
    val subtitle: String = "",
    // 초대할 수 있는 가족 (나는 뺀다)
    val members: List<RecordMember> = emptyList(),
    val draft: ScheduleRegisterDraft = ScheduleRegisterDraft(),
    val saving: Boolean = false,
    val error: String? = null,
    // 이미 다른 일정이 있어 고를 수 없는 날 (하루에 일정은 하나만 정할 수 있다). 바꾸는 일정 자신의 날은 고를 수 있다
    val occupiedDates: Set<LocalDate> = emptySet(),
)

sealed interface ScheduleEditEffect {
    data object Saved : ScheduleEditEffect
    data object Cancelled : ScheduleEditEffect
}

// 제안자가 기존 서버 일정의 날짜·시간·참여자·한 줄을 바꾸는 화면 (일정 등록 화면의 변경 모드)
// 기존 값은 일정 상세로 채운다. 상세의 참여자는 닉네임뿐이라, 닉네임이 같은 가족 구성원을 초대 대상으로 미리 고른다
@HiltViewModel
class ScheduleEditViewModel
@Inject
constructor(
    savedState: SavedStateHandle,
    private val editAccess: GetScheduleEditAccessUseCase,
    private val schedules: CalendarScheduleRepository,
    private val clock: Clock,
) : ViewModel() {
    private val scheduleId = savedState.get<Long>(SCHEDULE_ID_ARG)
    private val state = MutableStateFlow(ScheduleEditUiState(earliest = clock.kiriNow()))
    val uiState = state.asStateFlow()
    private val effects = Channel<ScheduleEditEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()
    private val dayLimit = ScheduleDayLimit(schedules, excludeScheduleId = scheduleId)

    init {
        load()
    }

    // 날짜 시트가 보여주는 달의 가족 일정을 불러와 다른 일정이 있는 날을 고를 수 없게 한다
    // 불러오지 못하면 막지 않고 저장 직전에 다시 확인한다
    fun onMonthShown(month: YearMonth) {
        viewModelScope.launch {
            if (dayLimit.load(month)) state.update { it.copy(occupiedDates = dayLimit.occupiedDates) }
        }
    }

    fun retry() {
        if (!uiState.value.loading) load()
    }

    fun onDraftChange(draft: ScheduleRegisterDraft) {
        if (uiState.value.saving) return
        state.update { current ->
            // 한 줄은 100자까지, 구성원은 초대할 수 있는 가족 안에서만 고른다
            val known = current.members.map { it.id }.toSet()
            current.copy(
                draft = draft.withMessage(draft.message).copy(memberIds = draft.memberIds intersect known),
                error = null,
            )
        }
    }

    fun cancel() {
        val id = scheduleId ?: return
        if (state.value.saving || state.value.loading || state.value.loadError != null) return
        state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            schedules.cancel(id).onSuccess { effects.send(ScheduleEditEffect.Cancelled) }
                .onFailure { error -> state.update { it.copy(error = error.message ?: "취소하지 못했어요.") } }
            state.update { it.copy(saving = false) }
        }
    }

    fun save() {
        val current = uiState.value
        val id = scheduleId ?: return
        val draft = current.draft
        val date = draft.date
        val time = draft.time
        if (current.saving || !draft.canSubmit || date == null || time == null) return
        val inviteeIds = draft.memberIds.mapNotNull { it.toLongOrNull() }.toSet()
        if (inviteeIds.size != draft.memberIds.size) {
            state.update { it.copy(error = "가족 정보를 다시 불러온 뒤 시도해주세요.") }
            return
        }
        state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            // 하루에 일정은 하나만 정할 수 있다. 그 달을 아직 받지 못했으면 지금 받아 확인한다
            dayLimit.load(YearMonth.from(date))
            if (date in dayLimit.occupiedDates) {
                state.update {
                    it.copy(saving = false, occupiedDates = dayLimit.occupiedDates, error = ScheduleDayLimit.OCCUPIED_MESSAGE)
                }
                return@launch
            }
            schedules.updateSchedule(id, ScheduleUpdate(date, time, inviteeIds, draft.message.takeIf { it.isNotBlank() }))
                .onSuccess {
                    state.update { it.copy(saving = false) }
                    effects.send(ScheduleEditEffect.Saved)
                }
                .onFailure { error -> state.update { it.copy(saving = false, error = error.saveMessage()) } }
        }
    }

    private fun load() {
        val id = scheduleId
        if (id == null) {
            state.update { it.copy(loading = false, loadError = "찾을 수 없는 약속이에요.") }
            return
        }
        state.update { it.copy(loading = true, loadError = null, earliest = clock.kiriNow()) }
        viewModelScope.launch {
            val access = editAccess(id).getOrElse {
                state.update { s -> s.copy(loading = false, loadError = "약속 정보를 불러오지 못했어요.") }
                return@launch
            }
            val problem = when {
                !access.detail.canBeEditedBeforeTheDay -> "지금은 변경할 수 없는 약속이에요."
                access.family == null -> "가족 정보를 불러오지 못했어요."
                !access.isMine -> "일정 제안자만 수정할 수 있어요."
                else -> null
            }
            if (problem != null) {
                state.update { it.copy(loading = false, loadError = problem) }
                return@launch
            }
            val detail = access.detail
            val members = access.invitableMembers.map { RecordMember(it.id, it.nickname) }
            // 맨 앞의 제안자(나)를 뺀 참여자를 미리 고른다
            val invited = access.invitedMemberIds
            state.update {
                it.copy(
                    loading = false,
                    title = detail.displayTitle(),
                    subtitle = detail.status.badgeLabel(),
                    members = members,
                    // 기존 한 줄은 상세 응답에 없어 비워 둔다
                    draft = ScheduleRegisterDraft(date = detail.date, time = detail.time, memberIds = invited),
                )
            }
        }
    }

    private fun Throwable.saveMessage() = when (this) {
        is NotScheduleProposerException -> "일정 제안자만 수정할 수 있어요."
        is ScheduleNotEditableException -> "지금은 변경할 수 없는 약속이에요."
        is ScheduleNotFoundException -> "찾을 수 없는 약속이에요."
        is InvalidScheduleUpdateException -> "날짜·시간과 함께할 가족을 다시 확인해주세요."
        else -> "약속을 변경하지 못했어요. 다시 시도해주세요."
    }

    companion object {
        const val SCHEDULE_ID_ARG = "scheduleId"
    }
}
