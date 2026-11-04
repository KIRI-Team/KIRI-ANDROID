package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.detail.DetailParticipant
import com.kiri.domain.detail.MissionDetail
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.schedule.CancelledScheduleException
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.domain.repository.CalendarScheduleRepository
import com.kiri.feature.detail.model.DetailUiEvent
import com.kiri.feature.detail.model.DetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 캘린더에서 고른 서버 일정의 상세 화면 상태
data class ServerScheduleDetailUiState(
    val detail: DetailUiState = DetailUiState(),
    // 받은 상세. 화면 제목·배지를 정하는 데 쓴다
    val schedule: ScheduleDetail? = null,
) {
    // 약속 당일에만 컨디션 조정하러가기 버튼을 보여준다
    val canCustomize get() = schedule?.status == ScheduleDisplayStatus.TODAY
}

sealed interface ServerScheduleDetailEffect {
    data object Back : ServerScheduleDetailEffect

    // 약속 당일에만 컨디션 커스텀 화면을 연다
    data object OpenCustom : ServerScheduleDetailEffect
}

// 일정 상세 API 로 받은 날짜·시간·상태·참여자를 기존 상세 화면 형식에 채운다
// API 가 주지 않는 제목·장소·걸음수·추천 활동은 비워 두고 화면의 "미정·정보 없음" 문구로 보여준다
@HiltViewModel
class ServerScheduleDetailViewModel
@Inject
constructor(
    savedState: SavedStateHandle,
    private val repository: CalendarScheduleRepository,
) : ViewModel() {
    private val scheduleId = savedState.get<Long>(SCHEDULE_ID_ARG)
    private val state = MutableStateFlow(ServerScheduleDetailUiState())
    val uiState = state.asStateFlow()
    private val effects = Channel<ServerScheduleDetailEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: DetailUiEvent) {
        when (event) {
            DetailUiEvent.BackClicked -> effects.trySend(ServerScheduleDetailEffect.Back)
            DetailUiEvent.RetryClicked -> if (!uiState.value.detail.loading) load()
            is DetailUiEvent.MemoriesSelected ->
                state.update { it.copy(detail = it.detail.copy(memoriesSelected = event.selected)) }
            DetailUiEvent.CustomClicked ->
                if (uiState.value.canCustomize) {
                    // 들어갈 때마다 고른 조건을 비우고 시작한다
                    state.update { it.copy(detail = it.detail.copy(draft = emptySet())) }
                    effects.trySend(ServerScheduleDetailEffect.OpenCustom)
                }
            is DetailUiEvent.ConditionToggled ->
                state.update {
                    val draft = it.detail.draft
                    it.copy(
                        detail = it.detail.copy(
                            draft = if (event.condition in draft) draft - event.condition else draft + event.condition,
                        ),
                    )
                }
            DetailUiEvent.CustomCancelled -> effects.trySend(ServerScheduleDetailEffect.Back)
            // TODO: 서버 일정에 컨디션을 적용하는 API 가 아직 없어 적용하기는 막아 둔다 (화면에서도 버튼을 끈다)
            DetailUiEvent.ApplyClicked -> Unit
            // 서버 일정 상세에는 기록 남기기가 아직 없다
            DetailUiEvent.RecordClicked -> Unit
        }
    }

    private fun load() {
        val id = scheduleId
        if (id == null) {
            state.update { it.copy(detail = it.detail.copy(loading = false, error = "찾을 수 없는 약속이에요.")) }
            return
        }
        state.update { it.copy(detail = it.detail.copy(loading = true, error = null)) }
        viewModelScope.launch {
            repository.getScheduleDetail(id)
                .onSuccess { schedule ->
                    state.update {
                        it.copy(
                            schedule = schedule,
                            detail = it.detail.copy(loading = false, detail = schedule.toMissionDetail(id)),
                        )
                    }
                }
                .onFailure { error ->
                    state.update { it.copy(detail = it.detail.copy(loading = false, error = error.message())) }
                }
        }
    }

    private fun Throwable.message() = when (this) {
        is CancelledScheduleException -> "취소된 약속이에요."
        is ScheduleNotFoundException -> "찾을 수 없는 약속이에요."
        is NoFamilyException -> "가족 공간이 없어요."
        else -> "약속 정보를 불러오지 못했어요."
    }

    companion object {
        const val SCHEDULE_ID_ARG = "scheduleId"
    }
}

// 일정 상세 API 에는 제목이 없어 날짜로 제목을 만든다
internal fun ScheduleDetail.toMissionDetail(scheduleId: Long) = MissionDetail(
    id = scheduleId.toString(),
    title = "${date.monthValue}월 ${date.dayOfMonth}일 약속",
    familyName = "",
    date = date,
    startTime = time?.format(DateTimeFormatter.ofPattern("a h시 m분", Locale.KOREAN)),
    place = null,
    expectedSteps = null,
    durationMinutes = null,
    participants = participants.map { DetailParticipant(it.nickname, it.accepted) },
    completed = status == ScheduleDisplayStatus.COMPLETED,
    activities = emptyList(),
    essentials = emptyList(),
    nearby = emptyList(),
)

// 캘린더와 같이 당일(TODAY)은 확정으로 보여준다. 당일 문구는 홈 화면에서 쓴다
internal fun ScheduleDisplayStatus?.badgeLabel() = when (this) {
    ScheduleDisplayStatus.SCHEDULED -> "약속 예정"
    ScheduleDisplayStatus.CONFIRMED, ScheduleDisplayStatus.TODAY -> "약속 확정"
    ScheduleDisplayStatus.COMPLETED -> "약속 완료"
    null -> "약속"
}
