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
    savedState: SavedStateHandle,
    private val schedules: CalendarScheduleRepository,
    private val home: HomeRepository,
    private val clock: Clock,
) : ViewModel() {
    private val bucketItemId = savedState.get<Long>(BUCKET_ITEM_ID_ARG)
    private val state = MutableStateFlow(
        BucketScheduleCreateUiState(
            earliest = clock.kiriNow(),
            title = savedState.get<String>(TITLE_ARG).orEmpty(),
            savedBy = savedState.get<String>(SAVER_ARG).orEmpty(),
        ),
    )
    val uiState = state.asStateFlow()
    private val effects = Channel<BucketScheduleCreateEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    init {
        load()
    }

    fun retry() {
        if (!uiState.value.loading) load()
    }

    fun onDraftChange(draft: ScheduleRegisterDraft) {
        if (uiState.value.sending) return
        state.update { current ->
            // 한 줄은 100자까지, 구성원은 초대할 수 있는 가족 안에서만 고른다
            val known = current.members.map { it.id }.toSet()
            current.copy(draft = draft.withMessage(draft.message).copy(memberIds = draft.memberIds intersect known), error = null)
        }
    }

    fun send(channel: ScheduleRequestChannel) {
        val current = uiState.value
        val id = bucketItemId ?: return
        val draft = current.draft
        val date = draft.date
        val time = draft.time
        if (current.sending || !draft.canSubmit || date == null || time == null) return
        val inviteeIds = draft.memberIds.mapNotNull { it.toLongOrNull() }.toSet()
        if (inviteeIds.size != draft.memberIds.size) {
            state.update { it.copy(error = "가족 정보를 다시 불러온 뒤 시도해주세요.") }
            return
        }
        state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            schedules.createSchedule(id, ScheduleUpdate(date, time, inviteeIds, draft.message.takeIf { it.isNotBlank() }))
                .onSuccess {
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
        if (bucketItemId == null) {
            state.update { it.copy(loading = false, loadError = "찾을 수 없는 버킷이에요.") }
            return
        }
        state.update { it.copy(loading = true, loadError = null, earliest = clock.kiriNow()) }
        viewModelScope.launch {
            try {
                val family = home.load()
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
