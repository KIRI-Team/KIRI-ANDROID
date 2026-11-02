package com.kiri.feature.home

import com.kiri.feature.home.model.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val tutorialRepository: HomeTutorialRepository = NoHomeTutorial,
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val scheduleRepository: com.kiri.domain.schedule.ScheduleRepository? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(
        tutorialStep = savedState.get<String>("homeTutorialStep")?.let { value -> HomeTutorialStep.entries.firstOrNull { it.name == value } },
        showingMission = savedState["homeShowingMission"] ?: false,
    ))
    val uiState = _uiState.asStateFlow()
    private val _effect = kotlinx.coroutines.channels.Channel<HomeUiEffect>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    private var loadingInProgress = false
    init {
        load()
        scheduleRepository?.let { schedules -> viewModelScope.launch { schedules.schedules.collect {
            if (!loadingInProgress) _uiState.update { state -> state.copy(home = state.home?.withSchedules()) }
        } } }
    }

    fun refreshOnResume() { if (!uiState.value.editing && !uiState.value.busy) load() }

    private fun FamilyHome.withSchedules(): FamilyHome {
        val shared = scheduleRepository?.schedules?.value ?: return this
        if (browsing) return this
        return copy(schedules = (if (serverBacked) schedules.filter { old -> shared.none { it.id == old.id } } else emptyList()) + shared.map { schedule ->
            val status = schedule.status(clock.instant())
            val response = schedule.responses[currentUserId]
            HomeSchedule(schedule.id, schedule.title, schedule.startsAt,
                if (schedule.canRespond(currentUserId, clock.instant()) && response == com.kiri.domain.schedule.MemberResponse.RECONFIRM_REQUIRED) ScheduleStatus.RECONFIRM_REQUIRED
                else if (schedule.canRespond(currentUserId, clock.instant()) && response == com.kiri.domain.schedule.MemberResponse.PENDING) ScheduleStatus.PENDING_RESPONSE else status,
                currentUserId in schedule.acceptedIds, schedule.responseDeadline, schedule.missionId, schedule.weeklyMissionId)
        }, mission = mission?.let { weekly -> weekly.copy(completed = weekly.completed || shared.any { it.weeklyMissionId == weekly.id && it.completed }) })
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.TutorialNext -> advanceTutorial()
            HomeUiEvent.TutorialDismissed -> setTutorial(null)
            is HomeUiEvent.DestinationClicked -> _effect.trySend(HomeUiEffect.Navigate(event.destination))
            is HomeUiEvent.MissionClicked -> _effect.trySend(HomeUiEffect.OpenMission(event.id))
            HomeUiEvent.WeeklyScheduleClicked -> {
                val home = uiState.value.home ?: return
                val weekly = home.mission ?: return
                if (home.browsing || weekly.unavailable || weekly.completed) return
                val existing = home.schedules.firstOrNull {
                    it.weeklyMissionId == weekly.id && it.status != ScheduleStatus.CANCELLED
                }
                _effect.trySend(if (existing == null) HomeUiEffect.CreateWeeklySchedule(weekly.id)
                    else HomeUiEffect.OpenSchedule(existing.id))
            }
            is HomeUiEvent.ScheduleClicked -> _effect.trySend(HomeUiEffect.OpenSchedule(event.id))
            HomeUiEvent.InviteFamilyClicked -> uiState.value.home?.inviteCode?.let { _effect.trySend(HomeUiEffect.InviteFamily(it)) }

            HomeUiEvent.RetryClicked -> load()
            HomeUiEvent.DateRefreshRequested -> {
                val date = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")))
                if (uiState.value.home?.date != date && !uiState.value.loading && !uiState.value.busy) load()
            }
            is HomeUiEvent.MemberSelected -> {
                if (uiState.value.home?.canRead(event.id) == true && !uiState.value.busy)
                    _uiState.update { it.copy(selectedMemberId = event.id, error = null) }
            }
            HomeUiEvent.AnswerEditClicked -> {
                val s = uiState.value
                val home = s.home ?: return
                val question = home.question ?: return
                if (!home.canRead(home.currentUserId) || s.busy) return
                _uiState.update { it.copy(
                    editing = true, selectedMemberId = null, error = null,
                    draft = if (s.draftQuestionId == question.id) s.draft else home.answers[home.currentUserId].orEmpty(),
                    draftQuestionId = question.id,
                ) }
            }
            is HomeUiEvent.AnswerChanged -> if (!uiState.value.busy) _uiState.update { it.copy(draft = event.text, error = null) }
            HomeUiEvent.AnswerSaveClicked -> save()
            HomeUiEvent.PokeClicked -> poke()
            HomeUiEvent.CardFlipped -> if (uiState.value.home?.browsing == false && !uiState.value.busy)
                setShowingMission(!uiState.value.showingMission)
            HomeUiEvent.BackClicked -> if (!uiState.value.busy) _uiState.update {
                it.copy(editing = false, selectedMemberId = null, error = null, answerSaved = false)
            }
            HomeUiEvent.NoticeDismissed -> _uiState.update { it.copy(notice = null, editing = if (it.answerSaved) false else it.editing, answerSaved = false) }
            HomeUiEvent.MemberDismissed -> _uiState.update { it.copy(selectedMemberId = null) }
        }
    }

    private fun setShowingMission(showing: Boolean) {
        savedState["homeShowingMission"] = showing
        _uiState.update { it.copy(showingMission = showing) }
    }

    private fun setTutorial(step: HomeTutorialStep?) {
        savedState["homeTutorialStep"] = step?.name
        _uiState.update { it.copy(tutorialStep = step) }
    }

    private fun advanceTutorial() {
        when (uiState.value.tutorialStep) {
            HomeTutorialStep.INITIAL -> { setShowingMission(true); setTutorial(HomeTutorialStep.MISSION) }
            HomeTutorialStep.MISSION -> setTutorial(if (uiState.value.home?.recommendations?.isNotEmpty() == true) HomeTutorialStep.RECOMMENDATIONS else null)
            HomeTutorialStep.RECOMMENDATIONS -> setTutorial(null)
            null -> Unit
        }
    }


    private fun load() {
        if (uiState.value.busy || loadingInProgress) return
        loadingInProgress = true
        _uiState.update { it.copy(loading = it.home == null, loadError = false) }
        viewModelScope.launch {
            try {
                val loaded = repository.load()
                scheduleRepository?.refresh()
                val home = loaded.withSchedules()
                if (!home.browsing && savedState.get<Boolean>("homeTutorialChecked") != true) {
                    // Mark the visit before showing it: returning home or restarting cannot replay it.
                    val firstVisit = runCatching { tutorialRepository.claimFirstVisit(home.currentUserId) }
                        .getOrElse { error -> if (error is CancellationException) throw error else false }
                    savedState["homeTutorialChecked"] = true
                    if (uiState.value.tutorialStep == null && firstVisit) {
                        setShowingMission(false)
                        setTutorial(HomeTutorialStep.INITIAL)
                    }
                }
                _uiState.update { old ->
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
            catch (_: Exception) { _uiState.update { it.copy(loading = false, loadError = true) } }
            finally { loadingInProgress = false }
        }
    }

    private fun save() {
        val s = uiState.value
        if (!s.canSave) return
        val question = s.home?.question ?: return
        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val home = repository.saveAnswer(question, s.draft).withSchedules()
                _uiState.update { it.copy(home = home, busy = false, editing = true, answerSaved = true) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { failed(error) }
        }
    }

    private fun poke() {
        val s = uiState.value
        val home = s.home ?: return
        val member = s.selectedMemberId ?: return
        val question = home.question ?: return
        if (s.busy || !home.canPoke(member)) return
        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val updated = repository.poke(question, member).withSchedules()
                val nickname = home.members.firstOrNull { it.id == member }?.nickname ?: "가족"
                _uiState.update { it.copy(
                    home = updated, busy = false, selectedMemberId = null,
                    notice = "${nickname}를 콕 찔렀습니다!",
                ) }
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { failed(error) }
        }
    }

    private fun failed(error: Exception) {
        val reason = (error as? HomeException)?.reason
        _uiState.update { it.copy(busy = false, error = when (reason) {
            HomeFailure.ANSWER_TOO_LONG -> "답변은 500자 이내로 작성해주세요."
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
