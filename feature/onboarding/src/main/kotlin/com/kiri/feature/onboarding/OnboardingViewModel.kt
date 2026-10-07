package com.kiri.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.model.invite.Invite
import com.kiri.domain.repository.InviteRepository
import com.kiri.feature.onboarding.model.OnboardingUiEffect
import com.kiri.feature.onboarding.model.OnboardingUiEvent
import com.kiri.feature.onboarding.model.OnboardingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
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
import javax.inject.Inject

// 온보딩 메인에서 선택하는 이후 진행 경로
enum class OnboardingPath {
    CREATE_FAMILY,
    JOIN_FAMILY,
    BROWSE,
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val inviteRepository: InviteRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<OnboardingUiEffect>(Channel.BUFFERED)
    val effect: Flow<OnboardingUiEffect> = _effect.receiveAsFlow()

    private var selectedPath: OnboardingPath? = null
    private var loadingJob: Job? = null

    // 로딩 단계에서 발급한 초대. 초대하기를 다시 눌러도 재발급하지 않고 재사용한다
    private var invite: Invite? = null
    private var isSharing = false

    fun initOnboarding() {
        selectedPath = null
        _uiState.update { it.copy(currentStep = OnboardingStep.INPUT_NICKNAME) }
    }

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.NicknameChanged -> _uiState.update {
                it.copy(nickname = event.nickname, nicknameViolations = emptySet())
            }
            is OnboardingUiEvent.RoleSelected -> _uiState.update { it.copy(selectedRole = event.role) }
            is OnboardingUiEvent.PathSelected -> selectPath(event.path)
            is OnboardingUiEvent.FamilyNameChanged -> _uiState.update {
                it.copy(familyName = event.familyName, familyNameViolations = emptySet())
            }
            is OnboardingUiEvent.InputFamilyCodeChanged -> _uiState.update { it.copy(inputFamilyCode = event.code) }
            OnboardingUiEvent.NextClicked -> next()
            OnboardingUiEvent.BackClicked -> previous()
            OnboardingUiEvent.InviteClicked -> invite()
            OnboardingUiEvent.InviteShared -> onInviteShared()
        }
    }

    // 온보딩 메인에서 경로를 선택하면 해당 경로의 첫 단계로 이동
    private fun selectPath(path: OnboardingPath) {
        if (_uiState.value.currentStep != OnboardingStep.ONBOARDING_MAIN) return
        selectedPath = path
        next()
    }

    private fun next() {
        val currentStep = _uiState.value.currentStep
        if (!validateInput(currentStep)) return
        val steps = stepsOf(selectedPath)
        // 메인에서 경로를 선택하기 전이거나 마지막 단계면 이동하지 않음
        val nextStep = steps.getOrNull(steps.indexOf(currentStep) + 1) ?: return
        _uiState.update { it.copy(currentStep = nextStep) }
        if (nextStep == OnboardingStep.GENERATE_FAMILY_CODE_LOADING) startLoading()
    }

    // 닉네임·가족명 단계에서는 규칙을 지키지 못하면 오류를 표시하고 넘어가지 않는다
    // 역할 선택 단계는 역할을 골라야 넘어간다
    private fun validateInput(step: OnboardingStep): Boolean {
        val state = _uiState.value
        return when (step) {
            OnboardingStep.INPUT_NICKNAME -> {
                val violations = NicknameValidator.violations(state.nickname)
                _uiState.update { it.copy(nicknameViolations = violations) }
                violations.isEmpty()
            }
            OnboardingStep.INPUT_FAMILY_NAME -> {
                val violations = NicknameValidator.violations(state.familyName)
                _uiState.update { it.copy(familyNameViolations = violations) }
                violations.isEmpty()
            }
            OnboardingStep.CHOOSE_ROLE -> state.selectedRole != null
            else -> true
        }
    }

    private fun invite() {
        when (_uiState.value.currentStep) {
            // 코드 생성 완료 화면: 발급한 초대 링크를 공유한다 (공유 시트가 열려 있는 동안 중복 클릭은 무시)
            OnboardingStep.GENERATE_FAMILY_CODE -> {
                val invite = invite ?: return run { _effect.trySend(OnboardingUiEffect.ShowInviteError) }
                if (isSharing) return
                isSharing = true
                _effect.trySend(OnboardingUiEffect.ShareInvite(invite))
            }
            // TODO: 가족코드 참여 동작이 정해지면 교체 (지금은 임시로 메인으로 이동)
            else -> _effect.trySend(OnboardingUiEffect.NavigateToMainFlow)
        }
    }

    // 공유 시트에서 돌아오면 메인으로 이동한다
    private fun onInviteShared() {
        if (!isSharing) return
        isSharing = false
        _effect.trySend(OnboardingUiEffect.NavigateToMainFlow)
    }

    // 초대 코드를 발급하고, 로딩 화면이 깜빡이지 않도록 최소 1초는 보여준다
    private fun startLoading() {
        loadingJob?.cancel()
        invite = null
        isSharing = false
        loadingJob = viewModelScope.launch {
            val minimumLoading = launch { delay(LOADING_DURATION_MILLIS) }
            val result = inviteRepository.createInvite()
            minimumLoading.join()
            if (_uiState.value.currentStep != OnboardingStep.GENERATE_FAMILY_CODE_LOADING) return@launch
            result
                .onSuccess { created ->
                    invite = created
                    _uiState.update { it.copy(familyCode = created.code) }
                    next()
                }
                .onFailure {
                    // 실패하면 가족명 입력으로 돌아가 다시 시도할 수 있게 한다
                    _uiState.update { it.copy(currentStep = OnboardingStep.INPUT_FAMILY_NAME) }
                    _effect.trySend(OnboardingUiEffect.ShowInviteError)
                }
        }
    }

    private fun previous() {
        val currentStep = _uiState.value.currentStep
        val steps = stepsOf(selectedPath)
        // 로딩 단계는 뒤로가기로 돌아오지 않도록 건너뜀
        val previousStep = steps
            .take(steps.indexOf(currentStep).coerceAtLeast(0))
            .lastOrNull { it != OnboardingStep.GENERATE_FAMILY_CODE_LOADING }
            ?: return

        loadingJob?.cancel()
        // 메인으로 돌아오면 선택한 경로 초기화
        if (previousStep == OnboardingStep.ONBOARDING_MAIN) selectedPath = null
        _uiState.update { it.copy(currentStep = previousStep) }
    }

    private fun stepsOf(path: OnboardingPath?): List<OnboardingStep> = COMMON_STEPS + when (path) {
        OnboardingPath.CREATE_FAMILY -> listOf(
            OnboardingStep.INPUT_FAMILY_NAME,
            OnboardingStep.GENERATE_FAMILY_CODE_LOADING,
            OnboardingStep.GENERATE_FAMILY_CODE,
        )
        OnboardingPath.JOIN_FAMILY -> listOf(OnboardingStep.INPUT_FAMILY_CODE)
        // TODO: 둘러보기 단계가 OnboardingStep에 추가되면 연결
        OnboardingPath.BROWSE, null -> emptyList()
    }

    private companion object {
        const val LOADING_DURATION_MILLIS = 1_000L

        val COMMON_STEPS = listOf(
            OnboardingStep.INPUT_NICKNAME,
            OnboardingStep.CHOOSE_ROLE,
            OnboardingStep.ONBOARDING_MAIN,
        )
    }
}