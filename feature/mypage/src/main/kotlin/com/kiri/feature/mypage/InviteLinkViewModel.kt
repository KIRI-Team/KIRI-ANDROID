package com.kiri.feature.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.model.invite.Invite
import com.kiri.domain.repository.InviteRepository
import com.kiri.feature.mypage.model.invite.InviteLinkUiEffect
import com.kiri.feature.mypage.model.invite.InviteLinkUiEvent
import com.kiri.feature.mypage.model.invite.InviteLinkUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 공용 초대 링크. 서버에서 내 가족 공간의 지금 초대를 불러오고, 리셋하면 새로 발급한다
@HiltViewModel
class InviteLinkViewModel @Inject constructor(
    private val inviteRepository: InviteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InviteLinkUiState())
    val uiState: StateFlow<InviteLinkUiState> = _uiState.asStateFlow()

    // 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
    private val _effect = Channel<InviteLinkUiEffect>(Channel.BUFFERED)
    val effect: Flow<InviteLinkUiEffect> = _effect.receiveAsFlow()

    private var issueJob: Job? = null
    private var isSharing = false

    init {
        // 서버에서 지금 초대를 불러온다. 불러오지 못하면 초대 없이 두고 오류를 알린다 (리셋하면 새로 발급할 수 있다)
        viewModelScope.launch {
            inviteRepository.getCurrentInvite()
                .onSuccess { current -> _uiState.update { it.copy(invite = current) } }
                .onFailure { _effect.trySend(InviteLinkUiEffect.ShowLoadError) }
        }
    }

    fun onEvent(event: InviteLinkUiEvent) {
        when (event) {
            InviteLinkUiEvent.CodeCopyClicked -> copy(label = "가족코드") { it.code }
            InviteLinkUiEvent.LinkCopyClicked -> copy(label = "초대링크") { it.url }
            InviteLinkUiEvent.ResetClicked -> issue()
            InviteLinkUiEvent.ShareClicked -> share()
            InviteLinkUiEvent.InviteShared -> isSharing = false
            InviteLinkUiEvent.BackClicked -> {
                // 발급 중에는 로딩 화면의 뒤로가기를 막는다
                if (_uiState.value.isIssuing) return
                _effect.trySend(InviteLinkUiEffect.NavigateBack)
            }
        }
    }

    private fun copy(label: String, text: (Invite) -> String) {
        val invite = _uiState.value.invite ?: return
        _effect.trySend(InviteLinkUiEffect.CopyToClipboard(label, text(invite)))
    }

    // 공유 시트가 열려 있는 동안 중복 클릭은 무시한다
    private fun share() {
        val state = _uiState.value
        val invite = state.invite ?: return
        if (!state.canShare || isSharing) return
        isSharing = true
        _effect.trySend(InviteLinkUiEffect.ShareInvite(url = invite.url, code = invite.code))
    }

    // 새 초대를 발급하고, 로딩 화면이 깜빡이지 않도록 최소 1초는 보여준다
    // 발급에 실패하면 기존 초대를 그대로 두고 오류를 알린다
    private fun issue() {
        if (_uiState.value.isIssuing) return
        issueJob?.cancel()
        _uiState.update { it.copy(isIssuing = true) }
        issueJob = viewModelScope.launch {
            val minimumLoading = launch { delay(LOADING_DURATION_MILLIS) }
            val result = inviteRepository.createInvite()
            minimumLoading.join()
            result
                .onSuccess { created -> _uiState.update { it.copy(invite = created, isIssuing = false) } }
                .onFailure {
                    _uiState.update { it.copy(isIssuing = false) }
                    _effect.trySend(InviteLinkUiEffect.ShowInviteError)
                }
        }
    }

    private companion object {
        const val LOADING_DURATION_MILLIS = 1_000L
    }
}
