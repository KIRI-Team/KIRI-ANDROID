package com.kiri.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.home.HomeException
import com.kiri.domain.home.HomeFailure
import com.kiri.domain.notification.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ScheduleInvitationUiState(
    val loading: Boolean = true,
    val invitation: ScheduleInvitation? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    val confirmed: Boolean = false,
)

@HiltViewModel
class ScheduleInvitationViewModel @Inject constructor(
    private val repository: ScheduleInvitationRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val id = savedState.get<String>("notificationId").orEmpty()
    private val _state = MutableStateFlow(ScheduleInvitationUiState())
    val uiState = _state.asStateFlow()
    private var loading = false
    init { reload() }
    fun reload() {
        if (loading || _state.value.submitting) return
        loading = true
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try { val invitation = repository.load(id); _state.update { it.copy(loading = false, invitation = invitation) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(loading = false, error = message(e)) } }
            finally { loading = false }
        }
    }
    fun confirm() {
        val current = _state.value
        if (current.loading || current.submitting || current.invitation == null || current.invitation.accepted) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try { val updated = repository.confirm(id); _state.update { it.copy(invitation = updated, submitting = false, confirmed = true) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(submitting = false, error = message(e)) } }
        }
    }
    private fun message(error: Exception): String = when ((error as? HomeException)?.reason) {
        HomeFailure.EXPIRED -> "응답 기한이 지났어요. 최신 일정을 확인해주세요."
        HomeFailure.UNAVAILABLE -> "변경되었거나 더 이상 확인할 수 없는 초대장이에요."
        else -> "처리하지 못했어요. 다시 시도해주세요."
    }
}
