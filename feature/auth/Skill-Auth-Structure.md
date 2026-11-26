# feature/auth 구조와 작업 규칙

스플래시, 로그인, 약관 동의를 담는 모듈이다. 공통 UI 규칙(MVI, Route/Screen 분리)은 [`feature/Skill-UI-Structure.md`](../Skill-UI-Structure.md)를 따르고, 이 문서는 auth에만 해당하는 내용을 적는다.

## 1. 진입점과 흐름

- 진입점: `presentation/AuthScreen.kt`의 `AuthRoute`. app의 Root NavHost(`KiriApp.kt`)가 `auth` 경로에서 호출한다.
- Navigation route가 아니라 **한 Route 안에서 `AuthStep`으로 단계를 바꾼다.** `LOGIN` → `TERMS_AGREEMENT` → 온보딩 또는 메인으로 이동(effect).
- 스플래시(`SplashScreen`)는 app의 `SessionStartup`이 로그인 상태를 확인하는 동안 보여준다.
- 쓰는 domain: `auth.repository.AuthRepository`(로그인), `term.repository.TermRepository`(약관 목록·동의)

## 2. 패키지 구조

```
com.kiri.feature.auth/
├── AuthStep.kt, AuthViewModel.kt   단계 enum, 화면 전체를 관리하는 ViewModel 하나
├── login/                          소셜 로그인 SDK 연동 (Context가 필요한 코드)
│   ├── SocialLogin.kt, SdkSocialLogin.kt, SocialLoginResult.kt   로그인 추상화와 결과
│   ├── KakaoLogin.kt, GoogleLogin.kt, NonceGenerator.kt         SDK별 구현
│   └── SocialLoginModule.kt, SocialLoginInitializer.kt          Hilt 제공, SDK 초기화
├── model/                          AuthUiState·UiEvent·UiEffect, LoginProvider
└── presentation/
    ├── AuthScreen.kt               AuthRoute + 단계 분기
    ├── SplashScreen.kt, LoginScreen.kt, TermsAgreementScreen.kt   단계별 화면
    ├── intro/LoginIntroMotion.kt   로그인 화면 진입 애니메이션
    └── component/                  AuthLoginButton, AuthAgreementRow
```

## 3. 모듈 규칙

- **SDK 로그인은 Activity가 필요해서 Route가 실행한다.** 순서는 다음과 같다.
  1. ViewModel이 `AuthUiEffect.StartSocialLogin(provider, nonce)`를 보낸다.
  2. `AuthRoute`가 `SocialLogin.login(activity, provider, nonce)`를 부른다.
  3. 결과를 `AuthUiEvent.SocialLoginFinished`로 ViewModel에 돌려준다.
  4. ViewModel이 토큰을 `AuthRepository`에 넘긴다.

  ViewModel은 SDK를 모른다.
- SDK 구현은 `login/`에만 둔다. 화면은 `SocialLogin` 인터페이스만 쓰고, `SocialLoginModule`이 `SdkSocialLogin`을 연결한다. UI 테스트는 `@TestInstallIn`으로 이 모듈을 바꾼다.
- 로그인 키(카카오 앱 키, 구글 클라이언트 id)가 비어 있으면 앱이 죽지 않고 `SocialLoginResult.Failure(MissingLoginKeyException)`으로 실패 처리된다.
- 약관 목록을 불러오지 못하면 `termsLoadFailed` 상태(`terms_load_failed`)를 보여준다. 필수 약관에 모두 동의해야 다음으로 간다 (`AuthUiState.canContinue`).

## 4. 새 화면(단계)을 추가할 때

1. `AuthStep`에 단계를 추가하고, `AuthViewModel`의 이동 규칙(next/back)에 넣는다.
2. 필요한 값은 `AuthUiState`에, 사용자 동작은 `AuthUiEvent`에, 일회성 이동은 `AuthUiEffect`에 추가한다.
3. `presentation/`에 단계 화면을 만든다. 원시 값과 람다만 받는다. 루트에 `testTag("xxx_screen")`을 붙인다.
4. `AuthScreen`의 `when (state.currentStep)`에 분기를 추가한다.
5. 단계 이동·검증은 `src/test/.../AuthViewModelTest`에, 화면 상호작용은 `app/src/androidTest/.../auth/`에 테스트를 추가한다.
6. 새 소셜 로그인은 `login/`에 `SocialLogin` 구현을 추가하고 `SocialLoginModule`에 등록한다. `LoginProvider`에도 항목을 추가한다.

## 5. 테스트

- `src/test`: `AuthViewModelTest`, `login/KakaoLoginTest`·`GoogleLoginTest`, `presentation/intro/LoginIntroMotionTest`. `MainDispatcherRule`로 Main 디스패처를 바꾼다.
- `app/src/androidTest/.../auth/`: `AuthScreensTest`, `LoginScreenTest`, `SplashScreenTest`. 서버 대신 `FakeAuthModules`를 쓴다.
