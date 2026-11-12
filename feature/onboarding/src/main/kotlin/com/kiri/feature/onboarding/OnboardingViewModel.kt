package com.kiri.feature.onboarding

import com.kiri.domain.model.nickname.NicknameValidator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiri.domain.model.family.AlreadyInFamilyException
import com.kiri.domain.model.family.DuplicateFamilyNameException
import com.kiri.domain.model.family.ExpiredInviteCodeException
import com.kiri.domain.model.family.InvalidInviteCodeException
import com.kiri.domain.model.family.Family
import com.kiri.domain.model.invite.Invite
import com.kiri.domain.model.nickname.DuplicateNicknameException
import com.kiri.domain.repository.FamilyRepository
import com.kiri.domain.repository.MemberRepository
import com.kiri.domain.usecase.invite.AcceptPendingInviteUseCase
import com.kiri.domain.usecase.invite.InviteAcceptResult
import com.kiri.domain.usecase.invite.message
import com.kiri.feature.onboarding.model.FamilyRole
import com.kiri.domain.model.member.FamilyRole as MemberFamilyRole
import com.kiri.feature.onboarding.model.OnboardingUiEffect
import com.kiri.feature.onboarding.model.OnboardingUiEvent
import com.kiri.feature.onboarding.model.OnboardingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val familyRepository: FamilyRepository,
    private val memberRepository: MemberRepository,
    private val session: com.kiri.domain.repository.SessionRepository? = null,
    // 초대 링크로 받아 둔 토큰이 있으면 가족 선택 화면에 들어올 때 그 가족에 참여한다. 없으면 확인하지 않는다
    private val acceptInvite: AcceptPendingInviteUseCase? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effect = Channel<OnboardingUiEffect>(Channel.BUFFERED)
    val effect: Flow<OnboardingUiEffect> = _effect.receiveAsFlow()

    private var selectedPath: OnboardingPath? = null
    private var initialPathOpened = false

    fun openInitialFamilyPath(path: OnboardingPath) {
        if (initialPathOpened || _uiState.value.isSubmitting || _uiState.value.currentStep != OnboardingStep.ONBOARDING_MAIN) return
        initialPathOpened = true
        selectPath(path)
    }

    // 서버에 저장한 닉네임·역할. 뒤로 왔다가 바꾸지 않고 다음을 누르면 다시 보내지 않는다
    private var savedNickname: String? = null
    private var savedRole: FamilyRole? = null

    // 로딩 단계에서 만든 가족 공간
    private var createdFamily: Family? = null

    // 가족 공간을 만들 때 발급된 초대. 초대하기를 다시 눌러도 재발급하지 않고 재사용한다
    private var invite: Invite? = null
    private var isSharing = false

    init {
        if (session != null) viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                val progress = session.profile()
                savedNickname = progress.nickname
                savedRole = progress.role?.let { name -> FamilyRole.entries.find { it.toMemberRole().name == name } }
                _uiState.update { it.copy(nickname = progress.nickname.orEmpty(), selectedRole = savedRole,
                    currentStep = if (savedNickname == null) OnboardingStep.INPUT_NICKNAME else if (savedRole == null) OnboardingStep.CHOOSE_ROLE else OnboardingStep.ONBOARDING_MAIN) }
            } finally { _uiState.update { it.copy(isSubmitting = false) } }
            if (_uiState.value.currentStep == OnboardingStep.ONBOARDING_MAIN) acceptPendingInvite()
        }
    }

    fun completeBrowse(onCompleted: () -> Unit) {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch { session?.completeOnboarding(); onCompleted() }
    }

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.NicknameChanged -> _uiState.update {
                it.copy(nickname = event.nickname, nicknameViolations = emptySet(), isNicknameDuplicated = false)
            }
            is OnboardingUiEvent.RoleSelected -> if (!_uiState.value.isSubmitting) {
                _uiState.update { it.copy(selectedRole = event.role) }
            }
            is OnboardingUiEvent.PathSelected -> selectPath(event.path)
            is OnboardingUiEvent.FamilyNameChanged -> _uiState.update {
                it.copy(familyName = event.familyName, familyNameViolations = emptySet(), isFamilyNameDuplicated = false)
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
        if (_uiState.value.isSubmitting || !validateInput(currentStep)) return
        val state = _uiState.value
        // 닉네임·역할은 서버에 저장한 뒤에 넘어간다. 이미 저장한 값으로 돌아온 경우는 다시 보내지 않는다
        // 가족 공간은 로딩 단계에서 만든다. 이미 같은 이름으로 만들었으면 다시 만들지 않고 코드 화면으로 간다
        val created = createdFamily
        when {
            currentStep == OnboardingStep.INPUT_NICKNAME && state.nickname != savedNickname -> submitNickname(state.nickname)
            currentStep == OnboardingStep.CHOOSE_ROLE && state.selectedRole != savedRole -> submitRole(requireNotNull(state.selectedRole))
            currentStep == OnboardingStep.INPUT_FAMILY_NAME && created?.name == state.familyName -> showCreatedFamily(created)
            else -> moveToNextStep(currentStep)
        }
    }

    private fun moveToNextStep(currentStep: OnboardingStep) {
        val steps = stepsOf(selectedPath)
        // 메인에서 경로를 선택하기 전이거나 마지막 단계면 이동하지 않음
        val nextStep = steps.getOrNull(steps.indexOf(currentStep) + 1) ?: return
        _uiState.update { it.copy(currentStep = nextStep) }
        if (nextStep == OnboardingStep.GENERATE_FAMILY_CODE_LOADING) startLoading()
        if (nextStep == OnboardingStep.ONBOARDING_MAIN) acceptPendingInvite()
    }

    // 링크 -> 로그인 -> 온보딩(닉네임·역할) 다음, 가족 선택 화면에 들어오면 받아 둔 초대로 참여한다
    // 참여하면 가족 선택 없이 온보딩을 마치고 메인으로 간다. 실패하면 이유를 알리고 가족 선택 화면에 머문다
    // 확인하는 동안에는 다른 경로를 고르지 못하게 막는다
    private fun acceptPendingInvite() {
        val accept = acceptInvite ?: return
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = try {
                accept()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                // 받아 둔 토큰을 읽지 못하면 초대가 없는 것으로 보고 평소처럼 고르게 한다
                InviteAcceptResult.NoInvite
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
            result.message?.let { _effect.trySend(OnboardingUiEffect.ShowMessage(it)) }
            if (result is InviteAcceptResult.Joined) {
                session?.completeOnboarding()
                _effect.trySend(OnboardingUiEffect.NavigateToMainFlow)
            }
        }
    }

    private fun submitNickname(nickname: String) = submit(
        step = OnboardingStep.INPUT_NICKNAME,
        request = { memberRepository.updateNickname(nickname) },
        onSaved = { savedNickname = nickname },
        onFailure = { error ->
            if (error is DuplicateNicknameException) {
                _uiState.update { it.copy(isNicknameDuplicated = true) }
            } else {
                _effect.trySend(OnboardingUiEffect.ShowNicknameError)
            }
        },
    )

    private fun submitRole(role: FamilyRole) = submit(
        step = OnboardingStep.CHOOSE_ROLE,
        request = { memberRepository.updateFamilyRole(role.toMemberRole()) },
        onSaved = { savedRole = role },
        onFailure = { _effect.trySend(OnboardingUiEffect.ShowRoleError) },
    )

    // 저장하는 동안에는 입력·뒤로가기·다음을 막고, 성공하면 다음 단계로 넘어간다
    private fun submit(
        step: OnboardingStep,
        request: suspend () -> Result<Unit>,
        onSaved: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = request()
            _uiState.update { it.copy(isSubmitting = false) }
            result
                .onSuccess {
                    onSaved()
                    moveToNextStep(step)
                }
                .onFailure(onFailure)
        }
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
                val violations = NicknameValidator.familyNameViolations(state.familyName)
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
            // 가족코드 입력 화면: 입력한 코드로 가족에 가입한다
            OnboardingStep.INPUT_FAMILY_CODE -> joinFamily()
            else -> Unit
        }
    }

    // 6자리를 모두 입력해야 보낸다. 가입하면 온보딩을 마치고 메인으로 간다. 실패하면 입력은 그대로 두고 이유를 알린다
    private fun joinFamily() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val code = state.inputFamilyCode
        if (code.length != FAMILY_CODE_LENGTH) {
            _effect.trySend(OnboardingUiEffect.ShowJoinFamilyError("6자리 가족코드를 모두 입력해주세요."))
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = familyRepository.joinFamily(code)
            result
                .onSuccess {
                    session?.completeOnboarding()
                    _effect.trySend(OnboardingUiEffect.NavigateToMainFlow)
                }
                .onFailure { error ->
                    _effect.trySend(OnboardingUiEffect.ShowJoinFamilyError(when (error) {
                        is InvalidInviteCodeException -> "가족코드를 다시 확인해주세요."
                        is ExpiredInviteCodeException -> "만료된 가족코드예요. 가족에게 새 코드를 받아주세요."
                        // 이미 다른 가족에 있어도 지금은 입력 화면에 머문다 (메인으로 보내지 않는다)
                        is AlreadyInFamilyException -> "이미 다른 가족에 가입되어 있어요."
                        else -> "가족에 참여하지 못했어요. 잠시 후 다시 시도해주세요."
                    }))
                }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    // 공유 시트에서 돌아오면 메인으로 이동한다
    private fun onInviteShared() {
        if (!isSharing) return
        isSharing = false
        _effect.trySend(OnboardingUiEffect.NavigateToMainFlow)
    }

    // 가족 공간을 만들고 함께 발급된 초대 코드를 받는다. 로딩 화면이 깜빡이지 않도록 최소 1초는 보여준다
    private fun startLoading() {
        val familyName = _uiState.value.familyName
        invite = null
        isSharing = false
        viewModelScope.launch {
            val minimumLoading = launch { delay(LOADING_DURATION_MILLIS) }
            val result = familyRepository.createFamily(familyName)
            minimumLoading.join()
            result
                .onSuccess { family ->
                    createdFamily = family
                    showCreatedFamily(family)
                }
                .onFailure { error ->
                    // 실패하면 가족명 입력으로 돌아가 다시 시도할 수 있게 한다
                    _uiState.update {
                        it.copy(
                            currentStep = OnboardingStep.INPUT_FAMILY_NAME,
                            isFamilyNameDuplicated = error is DuplicateFamilyNameException,
                        )
                    }
                    if (error !is DuplicateFamilyNameException) _effect.trySend(OnboardingUiEffect.ShowCreateFamilyError)
                }
        }
    }

    private fun showCreatedFamily(family: Family) {
        invite = family.invite
        _uiState.update { it.copy(familyCode = family.invite.code, currentStep = OnboardingStep.GENERATE_FAMILY_CODE) }
    }

    private fun previous() {
        val currentStep = _uiState.value.currentStep
        // 가족 공간을 만드는 요청은 취소해도 서버에서 만들어질 수 있어 로딩 중에는 뒤로가지 않는다
        if (_uiState.value.isSubmitting || currentStep == OnboardingStep.GENERATE_FAMILY_CODE_LOADING) return
        val steps = stepsOf(selectedPath)
        // 로딩 단계는 뒤로가기로 돌아오지 않도록 건너뜀
        val previousStep = steps
            .take(steps.indexOf(currentStep).coerceAtLeast(0))
            .lastOrNull { it != OnboardingStep.GENERATE_FAMILY_CODE_LOADING }
            ?: return

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

    // 화면의 역할(엄마·아빠·딸·아들)을 서버가 받는 역할 값으로 바꾼다
    private fun FamilyRole.toMemberRole(): MemberFamilyRole = when (this) {
        FamilyRole.MOM -> MemberFamilyRole.MOTHER
        FamilyRole.DAD -> MemberFamilyRole.FATHER
        FamilyRole.DAUGHTER -> MemberFamilyRole.DAUGHTER
        FamilyRole.SON -> MemberFamilyRole.SON
    }

    private companion object {
        const val LOADING_DURATION_MILLIS = 1_000L
        const val FAMILY_CODE_LENGTH = 6

        val COMMON_STEPS = listOf(
            OnboardingStep.INPUT_NICKNAME,
            OnboardingStep.CHOOSE_ROLE,
            OnboardingStep.ONBOARDING_MAIN,
        )
    }
}