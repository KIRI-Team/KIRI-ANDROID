package com.kiri.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.detail.DetailParticipant
import com.kiri.domain.detail.DetailSuggestion
import com.kiri.domain.detail.MissionDetail
import com.kiri.domain.model.mission.MissionGuide
import com.kiri.domain.model.mission.MissionInfo
import com.kiri.domain.repository.MissionInfoRepository
import com.kiri.domain.model.family.NoFamilyException
import com.kiri.domain.model.schedule.CancelledScheduleException
import com.kiri.domain.usecase.schedule.GetScheduleEditAccessUseCase
import com.kiri.domain.model.schedule.ScheduleDetail
import com.kiri.domain.model.schedule.ScheduleDisplayStatus
import com.kiri.domain.model.schedule.ScheduleNotFoundException
import com.kiri.feature.detail.model.DetailUiEvent
import com.kiri.feature.detail.model.DetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
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
    // 캘린더에서 넘겨받은 날짜. 상세를 받기 전·받지 못했을 때 앱바 제목에 쓴다
    val requestedDate: LocalDate? = null,
    // 취소·없음·가족 없음처럼 다시 불러와도 결과가 같은 오류면 false
    val retryable: Boolean = true,
) {
    // 받은 상세의 날짜를 먼저 쓰고, 없으면 캘린더에서 고른 날짜, 그것도 없으면 "약속 상세"
    val appBarTitle
        get() = (schedule?.date ?: requestedDate)?.let { "${it.monthValue}월 ${it.dayOfMonth}일" } ?: "약속 상세"

    // 약속 당일에만 컨디션 조정하러가기 버튼을 보여준다
    val canCustomize get() = schedule?.status == ScheduleDisplayStatus.TODAY
}

sealed interface ServerScheduleDetailEffect {
    data object Back : ServerScheduleDetailEffect

    // 약속 당일에만 컨디션 커스텀 화면을 연다
    data object OpenCustom : ServerScheduleDetailEffect

    // 당일이 아닌 일정을 제안자 본인이 열면 상세 대신 일정 변경 화면으로 보낸다
    data class OpenEdit(val scheduleId: Long) : ServerScheduleDetailEffect
}

// 일정 상세 API 로 받은 날짜·시간·상태·참여자로 머리말을 먼저 채우고,
// 일정의 missionId 로 미션 상세 API 를 불러와 제목·장소·걷기량·활동시간·필수 활동·준비물을 채운다.
// 미션을 불러오지 못하면 머리말만 보여주고 나머지는 화면의 "미정·정보 없음" 문구로 둔다
@HiltViewModel
class ServerScheduleDetailViewModel
@Inject
constructor(
    savedState: SavedStateHandle,
    private val editAccess: GetScheduleEditAccessUseCase,
    private val missions: MissionInfoRepository,
) : ViewModel() {
    private var loadJob: Job? = null
    private val scheduleId = savedState.get<Long>(SCHEDULE_ID_ARG)
    private val state = MutableStateFlow(
        ServerScheduleDetailUiState(
            requestedDate = savedState.get<String>(DATE_ARG)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        ),
    )
    val uiState = state.asStateFlow()
    private val effects = Channel<ServerScheduleDetailEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: DetailUiEvent) {
        when (event) {
            DetailUiEvent.BackClicked -> effects.trySend(ServerScheduleDetailEffect.Back)
            DetailUiEvent.RetryClicked -> if (!uiState.value.detail.loading && uiState.value.retryable) load()
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
            state.update {
                it.copy(retryable = false, detail = it.detail.copy(loading = false, error = "찾을 수 없는 약속이에요."))
            }
            return
        }
        state.update { it.copy(detail = it.detail.copy(loading = true, error = null)) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            editAccess(id)
                .onSuccess { access ->
                    // 당일이 아닌 내 일정이면 상세를 그리지 않고 로딩 상태 그대로 변경 화면으로 넘어간다.
                    // 가족 정보를 불러오지 못하면 제안자인지 알 수 없으므로 상세를 보여준다
                    if (access.canEdit) {
                        effects.send(ServerScheduleDetailEffect.OpenEdit(id))
                        return@onSuccess
                    }
                    val schedule = access.detail
                    state.update {
                        it.copy(
                            schedule = schedule,
                            detail = it.detail.copy(loading = false, detail = schedule.toMissionDetail(id)),
                        )
                    }
                    val missionId = schedule.missionId ?: return@onSuccess
                    missions.getMission(missionId).onSuccess { mission ->
                        state.update {
                            it.copy(detail = it.detail.copy(detail = schedule.toMissionDetail(id, mission)))
                        }
                    }
                }
                .onFailure { error ->
                    state.update {
                        it.copy(
                            retryable = error.isRetryable(),
                            detail = it.detail.copy(loading = false, error = error.message()),
                        )
                    }
                }
        }
    }

    private fun Throwable.isRetryable() =
        this !is CancelledScheduleException && this !is ScheduleNotFoundException && this !is NoFamilyException

    private fun Throwable.message() = when (this) {
        is CancelledScheduleException -> "취소된 약속이에요."
        is ScheduleNotFoundException -> "찾을 수 없는 약속이에요."
        is NoFamilyException -> "가족 공간이 없어요."
        else -> "약속 정보를 불러오지 못했어요."
    }

    companion object {
        const val SCHEDULE_ID_ARG = "scheduleId"
        // "2026-11-17" 형식
        const val DATE_ARG = "date"
    }
}

// 일정 상세 API 에는 제목이 없어 "11월 17일 약속" 처럼 날짜로 만든다. 일정 상세·수정 화면의 본문 제목에 쓴다
internal fun ScheduleDetail.displayTitle() = "${date.monthValue}월 ${date.dayOfMonth}일 약속"

// 날짜·시간·참여자·상태는 일정에서, 제목·장소·걷기량·활동시간·필수 활동(단계)·준비물은 미션에서 가져온다.
// 미션이 없으면 제목은 날짜로 만들고 나머지는 비워 둔다. 주변 장소는 아직 API 가 없다
internal fun ScheduleDetail.toMissionDetail(scheduleId: Long, mission: MissionInfo? = null) = MissionDetail(
    id = scheduleId.toString(),
    title = mission?.title ?: displayTitle(),
    familyName = "",
    date = date,
    startTime = time?.format(DateTimeFormatter.ofPattern("a h시 m분", Locale.KOREAN)),
    place = mission?.place?.name,
    expectedSteps = null,
    durationMinutes = mission?.durationMinMinutes,
    participants = participants.map { DetailParticipant(it.nickname, it.accepted) },
    completed = status == ScheduleDisplayStatus.COMPLETED,
    activities = mission?.steps.orEmpty().map { it.toSuggestion() },
    essentials = mission?.prepareItems.orEmpty().map { it.toSuggestion() },
    nearby = emptyList(),
    walkingLevel = mission?.walkingLevel?.label,
    durationMaxMinutes = mission?.durationMaxMinutes,
)

private fun MissionGuide.toSuggestion() = DetailSuggestion(title, description.orEmpty())

// 캘린더와 같이 당일(TODAY)은 확정으로 보여준다. 당일 문구는 홈 화면에서 쓴다
internal fun ScheduleDisplayStatus?.badgeLabel() = when (this) {
    ScheduleDisplayStatus.SCHEDULED -> "약속 예정"
    ScheduleDisplayStatus.CONFIRMED, ScheduleDisplayStatus.TODAY -> "약속 확정"
    ScheduleDisplayStatus.COMPLETED -> "약속 완료"
    null -> "약속"
}

// 약속 예정·완료는 회색 배지, 확정·당일은 초록 배지로 보여준다
internal fun ScheduleDisplayStatus?.usesNeutralBadge() =
    this == ScheduleDisplayStatus.SCHEDULED || this == ScheduleDisplayStatus.COMPLETED
