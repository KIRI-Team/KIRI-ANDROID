# Kiri Feature UI 구조 (auth / onboarding) 참고사항

이 문서는 `feature 하위 모듈이 따르는 UI 구조(MVI: `UiState` / `UiEvent` / `UiEffect`)를 정리한다. 새 feature 화면에 상태가 생기면 이 구조를 그대로 따른다.

## 1. 패키지 구조
규칙:

- 모듈 루트: `XxxStep`(단계 enum), `XxxViewModel`, 순수 로직(`NicknameValidator` 등)
- `model/`: `XxxUiState`, `XxxUiEvent`, `XxxUiEffect`와 화면에서 쓰는 enum/data class
- `presentation/`: Route/Screen과 단계별 화면
- `presentation/component/`: 해당 모듈 안에서만 재사용하는 UI 조각
- Context가 필요한 보조 코드(Intent 생성 등)는 별도 패키지(`invite/`)로 분리한다

## 2. Route / Screen 분리

```kotlin
@Composable
fun AuthRoute(
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateToOnboarding by rememberUpdatedState(onNavigateToOnboarding)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AuthUiEffect.NavigateToOnboarding -> currentOnNavigateToOnboarding()
            }
        }
    }

    AuthScreen(modifier = modifier, state = state, onEvent = viewModel::onEvent)
}
```

| 계층 | 역할 |
|---|---|
| `XxxRoute` | `hiltViewModel()` 주입, `uiState.collectAsStateWithLifecycle()`, `LaunchedEffect(viewModel)`에서 `effect` 수집, 외부 콜백은 `rememberUpdatedState`로 감싼다 |
| `XxxScreen(state, onEvent)` | 상태 비보유. `when (state.currentStep)`으로 단계별 화면을 고른다. `BackHandler`는 첫 단계에서 비활성화해 시스템 뒤로가기로 앱을 종료할 수 있게 한다 |
| 단계별 화면 | 원시 값과 람다만 받는다 (`LoginScreen(onLoginClick = …)`). `UiEvent`를 직접 알지 않는다 |

- 화면 전환은 Navigation route가 아니라 **한 Route 안에서 `currentStep`을 바꾸는 방식**이다.
- 앱의 Root NavHost(`KiriApp.kt`)가 `AuthRoute` / `OnboardingRoute`를 호출한다.
- `XxxScreen`은 state만으로 그려지므로 `@Preview`와 Compose UI 테스트에서 ViewModel 없이 쓸 수 있다.
- 루트 컨테이너에는 `testTag("auth_screen")`, `testTag("onboarding_screen")`을 붙인다.

## 3. UiState

`data class`로 만들고 모든 필드에 기본값을 둔다. 다른 값에서 계산할 수 있는 값은 저장하지 않고 계산 프로퍼티로 둔다.

```kotlin
data class AuthUiState(
    val currentStep: AuthStep = AuthStep.LOGIN,
    val agreedTerms: Set<AgreementTerm> = emptySet(),
    // 세부 내용 창에서 보고 있는 약관 (null이면 닫힘)
    val viewingTerm: AgreementTerm? = null,
) {
    val allAgreed: Boolean
        get() = agreedTerms.containsAll(AgreementTerm.entries)

    // 필수 약관에 모두 동의해야 계속할 수 있다
    val canContinue: Boolean
        get() = agreedTerms.containsAll(AgreementTerm.entries.filter { it.required })
}
```

```kotlin
data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.INPUT_NICKNAME,
    val nickname: String = "",
    // 다음 버튼을 눌렀을 때 지키지 못한 규칙 (입력을 바꾸면 비운다)
    val nicknameViolations: Set<NicknameRule> = emptySet(),
    val selectedRole: FamilyRole? = null,
    val familyName: String = "",
    val familyNameViolations: Set<NicknameRule> = emptySet(),
    val familyCode: String = "",
    val inputFamilyCode: String = "",
)
```

- `currentStep`이 현재 보여줄 단계를 정한다.
- 입력 값과 검증 오류를 짝으로 둔다 (`nickname` + `nicknameViolations`). 오류는 "다음"을 누를 때 채우고, 입력이 바뀌면 비운다.
- 바텀시트·다이얼로그를 열지 닫을지는 nullable 필드 하나로 표현한다 (`viewingTerm`).
- 화면에 보이지 않는 내부 상태는 UiState에 넣지 않고 ViewModel의 private 변수로 둔다 (`selectedPath`, `invite`, `isSharing`, `loadingJob`).

## 4. UiEvent (화면 → ViewModel)

```kotlin
// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface AuthUiEvent {
    data class LoginClicked(val provider: LoginProvider) : AuthUiEvent
    data object AllTermsToggled : AuthUiEvent
    data class TermToggled(val term: AgreementTerm) : AuthUiEvent
    data class TermDetailClicked(val term: AgreementTerm) : AuthUiEvent
    data object TermDetailDismissed : AuthUiEvent
    data class TermDetailAgreed(val term: AgreementTerm) : AuthUiEvent
    data object AgreeClicked : AuthUiEvent
    data object BackClicked : AuthUiEvent
}
```

- `sealed interface`로 만든다. 값이 있으면 `data class`, 없으면 `data object`를 쓴다.
- 이름은 사용자가 한 동작을 과거형으로 짓는다 (`~Clicked`, `~Changed`, `~Toggled`, `~Selected`, `~Dismissed`).
- ViewModel의 진입점은 `fun onEvent(event)` 하나이고, `when`으로 private 함수에 넘긴다.
- 현재 단계에서 처리하면 안 되는 이벤트는 각 함수 안에서 무시한다 (`if (currentStep != TERMS_AGREEMENT) return`).
- 상태 변경은 항상 `_uiState.update { it.copy(...) }`로 한다.
- 시스템 결과도 이벤트로 돌려보낸다. 예: 공유 시트에서 돌아오면 Route가 `OnboardingUiEvent.InviteShared`를 보낸다.

## 5. UiEffect (ViewModel → 화면, 일회성)

```kotlin
// 화면 이동처럼 한 번만 처리해야 하는 동작은 구독 전에 보내도 사라지지 않도록 Channel 로 전달한다
private val _effect = Channel<AuthUiEffect>(Channel.BUFFERED)
val effect: Flow<AuthUiEffect> = _effect.receiveAsFlow()
```

```kotlin
// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동, 토스트 등)
sealed interface OnboardingUiEffect {
    data object NavigateToMainFlow : OnboardingUiEffect
    data class ShareInvite(val invite: Invite) : OnboardingUiEffect
    data object ShowInviteError : OnboardingUiEffect
}
```

- `SharedFlow` 대신 `Channel(BUFFERED)` + `receiveAsFlow()`를 써서 화면이 구독하기 전에 보낸 effect도 사라지지 않게 한다.
- 화면 이동, 토스트, 공유 시트처럼 **한 번만 실행해야 하는 동작**만 effect로 보낸다. 다시 그려도 유지되어야 하는 값은 UiState에 둔다.
- Context가 필요한 처리(Toast, `rememberLauncherForActivityResult`, Intent)는 Route에서만 한다. 그래서 ViewModel은 Android에 의존하지 않는다.

## 6. 단계 흐름 (onboarding)

```kotlin
private fun stepsOf(path: OnboardingPath?): List<OnboardingStep> = COMMON_STEPS + when (path) {
    OnboardingPath.CREATE_FAMILY -> listOf(INPUT_FAMILY_NAME, GENERATE_FAMILY_CODE_LOADING, GENERATE_FAMILY_CODE)
    OnboardingPath.JOIN_FAMILY -> listOf(INPUT_FAMILY_CODE)
    OnboardingPath.BROWSE, null -> emptyList()
}
```

- 흐름은 단계 목록 하나로 정의하고, `next()` / `previous()`는 그 목록 안에서 인덱스만 옮긴다.
- `next()`는 `validateInput(currentStep)`을 통과해야 이동한다. 닉네임과 가족명은 `NicknameValidator`, 역할 선택은 역할을 골랐는지로 검사한다.
- `previous()`는 로딩 단계를 건너뛰고, 메인으로 돌아오면 `selectedPath`를 초기화한다.
- 비동기 작업(`inviteRepository.createInvite()`)은 `viewModelScope`에서 실행한다.
  - 로딩 화면이 깜빡이지 않도록 최소 1초는 보여준다.
  - 작업이 끝났을 때 단계가 이미 바뀌었으면 결과를 버린다.
  - 실패하면 가족명 입력 단계로 돌아가고 `ShowInviteError`를 보낸다.
- 공유 시트가 열려 있는 동안 중복 클릭은 `isSharing` 플래그로 막는다.

## 7. 테스트

- ViewModel과 순수 로직은 `src/test` 단위 테스트로 검증한다.
  - `AuthViewModelTest`
  - `OnboardingViewModelTest`
  - `NicknameValidatorTest`
  - `NicknameGuidesTest`
  - `component/*ColorsTest`
- Repository는 테스트용 구현(`TestInviteRepository`)으로 바꾸고, `MainDispatcherRule`로 Main 디스패처를 교체한다.
- 화면 상호작용은 `XxxScreen(state, onEvent)`에 state를 직접 넣어 Compose UI 테스트로 검증한다.

## 8. 새 화면에 적용할 때 체크리스트

1. `model/`에 `XxxUiState`(기본값 + 계산 프로퍼티), `XxxUiEvent`, `XxxUiEffect`(sealed interface)를 만든다.
2. `XxxViewModel`은 `StateFlow<XxxUiState>`, `Flow<XxxUiEffect>`(Channel), `onEvent()` 하나만 공개한다.
3. `presentation/`에 `XxxRoute`(ViewModel 연결과 effect 처리)와 `XxxScreen(state, onEvent)`(상태 비보유)를 나눠 만든다.
4. 하위 화면과 컴포넌트는 UiEvent 대신 값과 람다만 받는다.
5. 상태 변화와 경계 조건은 ViewModel 단위 테스트로 검증한다.
