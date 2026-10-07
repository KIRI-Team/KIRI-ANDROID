package com.kiri.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.kiri.domain.home.*
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Navigation is owned by the app; absent destinations remain explicit host events. */
sealed interface HomeDestination {
    data object Explore : HomeDestination
    data object CreateFamily : HomeDestination
    data object JoinFamily : HomeDestination
    data object InviteFamily : HomeDestination
    data object AnswerHistory : HomeDestination
    data class Mission(val id: String) : HomeDestination
}

data class HomeUiState(
    val loading: Boolean = true,
    val home: FamilyHome? = null,
    val loadError: Boolean = false,
    val selectedMemberId: String? = null,
    val editing: Boolean = false,
    val draft: String = "",
    val draftQuestionId: String? = null,
    val showingMission: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val answerSaved: Boolean = false,
) {
    val canSave: Boolean get() = editing && draft.isNotBlank() && !busy && home?.question != null
}

sealed interface HomeAction {
    data object Retry : HomeAction
    data object RefreshDate : HomeAction
    data class SelectMember(val id: String) : HomeAction
    data object EditAnswer : HomeAction
    data class ChangeAnswer(val text: String) : HomeAction
    data object SaveAnswer : HomeAction
    data object Poke : HomeAction
    data object Flip : HomeAction
    data object Back : HomeAction
    data object DismissNotice : HomeAction
    data object DismissMember : HomeAction
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState())
    val state = mutableState.asStateFlow()
    private var loadingInProgress = false
    init { load() }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Retry -> load()
            HomeAction.RefreshDate -> {
                val date = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))
                if (state.value.home?.date != date && !state.value.loading && !state.value.busy) load()
            }
            is HomeAction.SelectMember -> {
                if (state.value.home?.canRead(action.id) == true && !state.value.busy)
                    mutableState.update { it.copy(selectedMemberId = action.id, error = null) }
            }
            HomeAction.EditAnswer -> {
                val s = state.value
                val home = s.home ?: return
                val question = home.question ?: return
                if (!home.canRead(home.currentUserId) || s.busy) return
                mutableState.update { it.copy(
                    editing = true, selectedMemberId = null, error = null,
                    draft = if (s.draftQuestionId == question.id) s.draft else home.answers[home.currentUserId].orEmpty(),
                    draftQuestionId = question.id,
                ) }
            }
            is HomeAction.ChangeAnswer -> if (!state.value.busy) mutableState.update { it.copy(draft = action.text, error = null) }
            HomeAction.SaveAnswer -> save()
            HomeAction.Poke -> poke()
            HomeAction.Flip -> if (state.value.home?.browsing == false && !state.value.busy)
                mutableState.update { it.copy(showingMission = !it.showingMission) }
            HomeAction.Back -> if (!state.value.busy) mutableState.update {
                it.copy(editing = false, selectedMemberId = null, error = null, answerSaved = false)
            }
            HomeAction.DismissNotice -> mutableState.update { it.copy(notice = null, editing = if (it.answerSaved) false else it.editing, answerSaved = false) }
            HomeAction.DismissMember -> mutableState.update { it.copy(selectedMemberId = null) }
        }
    }

    private fun load() {
        if (state.value.busy || loadingInProgress) return
        loadingInProgress = true
        mutableState.update { it.copy(loading = true, loadError = false) }
        viewModelScope.launch {
            try {
                val home = repository.load()
                mutableState.update { old ->
                    val changed = old.home != null && old.home.date != home.date
                    old.copy(
                        loading = false, home = home, loadError = false, error = if (changed) null else old.error,
                        editing = if (changed) false else old.editing,
                        selectedMemberId = if (changed) null else old.selectedMemberId,
                        draft = if (changed) "" else old.draft,
                        draftQuestionId = if (changed) null else old.draftQuestionId,
                        notice = if (changed) "날짜가 바뀌어 오늘의 질문을 불러왔어요." else old.notice,
                    )
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableState.update { it.copy(loading = false, loadError = true) } }
            finally { loadingInProgress = false }
        }
    }

    private fun save() {
        val s = state.value
        if (!s.canSave) return
        val question = s.home?.question ?: return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val home = repository.saveAnswer(question, s.draft)
                mutableState.update { it.copy(home = home, busy = false, editing = true, answerSaved = true) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { failed(error) }
        }
    }

    private fun poke() {
        val s = state.value
        val home = s.home ?: return
        val member = s.selectedMemberId ?: return
        val question = home.question ?: return
        if (s.busy || !home.canPoke(member)) return
        mutableState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val updated = repository.poke(question, member)
                val nickname = home.members.firstOrNull { it.id == member }?.nickname ?: "가족"
                mutableState.update { it.copy(
                    home = updated, busy = false, selectedMemberId = null,
                    notice = "${nickname}를 콕 찔렀습니다!",
                ) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { failed(error) }
        }
    }

    private fun failed(error: Exception) {
        val reason = (error as? HomeException)?.reason
        mutableState.update { it.copy(busy = false, error = when (reason) {
            HomeFailure.EMPTY_ANSWER -> "답변을 입력해주세요."
            HomeFailure.EXPIRED -> "날짜가 바뀌었어요. 오늘의 질문을 다시 불러옵니다."
            HomeFailure.ALREADY_ANSWERED -> "이미 답변한 가족이에요."
            HomeFailure.ALREADY_POKED -> "오늘은 이미 콕을 보냈어요."
            HomeFailure.UNAVAILABLE -> "지금은 이 질문이나 구성원에게 접근할 수 없어요."
            else -> "처리하지 못했어요. 입력은 유지됐으니 다시 시도해주세요."
        }) }
        if (reason == HomeFailure.EXPIRED) load()
    }
}
