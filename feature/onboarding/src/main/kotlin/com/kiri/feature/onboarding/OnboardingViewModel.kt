package com.kiri.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class OnboardingViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<OnboardingUiEffect>(Channel.BUFFERED)
    val effect: Flow<OnboardingUiEffect> = _effect.receiveAsFlow()

    private var selectedPath: OnboardingPath? = null
    private var loadingJob: Job? = null

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

    // 초대하기 버튼을 누르면 임시로 홈화면으로 넘어감 -> 추후 수정 예정
    private fun invite() {
        _effect.trySend(OnboardingUiEffect.NavigateToMainFlow)
    }

    // TODO: 코드 생성 로직이 추가되면 실제 생성 완료 시점에 넘어가도록 교체 (지금은 임시로 1초 대기)
    private fun startLoading() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            delay(LOADING_DURATION_MILLIS)
            if (_uiState.value.currentStep == OnboardingStep.GENERATE_FAMILY_CODE_LOADING) next()
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