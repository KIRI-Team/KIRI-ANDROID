# feature/onboarding 구조와 작업 규칙

닉네임·역할 입력과 가족 만들기/참여하기/둘러보기를 담는 모듈이다. 공통 UI 규칙은 [`feature/Skill-UI-Structure.md`](../Skill-UI-Structure.md)를 따른다. 단계 흐름 설계(6장)도 그 문서에 있으니 먼저 읽는다.

## 1. 진입점과 흐름

- 진입점: `presentation/OnboardingScreen.kt`의 `OnboardingRoute`. app의 Root NavHost(`KiriApp.kt`)가 `onboarding` 경로에서 호출한다.
- 한 Route 안에서 `OnboardingStep`으로 단계를 바꾼다. 흐름은 선택한 경로(`OnboardingPath`)에 따라 달라진다.
  - 공통: 닉네임 입력 → 역할 선택 → 메인(만들기/참여/둘러보기)
  - 만들기: 가족 이름 입력 → 초대 코드 생성 로딩 → 초대 코드
  - 참여: 초대 코드 입력
- 쓰는 domain: `member.repository.MemberRepository`, `family.repository.FamilyRepository`, `invite.usecase.AcceptPendingInviteUseCase`(초대 링크로 들어온 경우 자동 참여)

## 2. 패키지 구조

```
com.kiri.feature.onboarding/
├── OnboardingStep.kt, OnboardingViewModel.kt   단계 enum과 ViewModel 하나
├── TemporaryFamilyCode.kt                       초대 코드 표시용 값
├── model/                                       OnboardingUiState·UiEvent·UiEffect, FamilyRole
└── presentation/
    ├── OnboardingScreen.kt                      OnboardingRoute + 단계 분기
    ├── OnboardingMainScreen.kt, InputNicknameScreen.kt, ChooseRoleScreen.kt,
    │   InputFamilyNameScreen.kt, GenerateFamilyCode(Loading)Screen.kt, InputFamilyCodeScreen.kt
    ├── NicknameGuides.kt                        닉네임 규칙 안내 문구
    └── component/                               입력칸, 코드 입력, 역할·메뉴 카드와 색 계산
```

## 3. 모듈 규칙

- 닉네임·가족 이름 규칙은 domain의 `member.model.NicknameValidator`를 쓴다. 화면에서 규칙을 다시 만들지 않는다. 오류는 다음을 누를 때 채우고 입력이 바뀌면 비운다.
- 단계 목록은 `stepsOf(path)` 하나로 정의한다. 새 단계는 이 목록에 넣고, `next()`/`previous()`는 목록 안에서만 움직인다.
- 비동기 단계(초대 코드 생성)는 로딩 화면을 최소 1초 보여주고, 끝났을 때 단계가 바뀌었으면 결과를 버린다.
- 입력칸·카드 색은 `component/*Colors.kt`의 순수 함수로 계산하고 단위 테스트로 검증한다.

## 4. 새 화면(단계)을 추가할 때

1. `OnboardingStep`에 단계를 추가하고 `stepsOf()`의 알맞은 경로에 넣는다.
2. 입력 값과 오류는 짝으로 `OnboardingUiState`에 추가한다 (`xxx` + `xxxViolations`).
3. `validateInput(currentStep)`에 검증을 추가한다.
4. `presentation/`에 화면을 만들고(원시 값과 람다만), 루트에 `testTag("xxx_screen")`을 붙인다. `OnboardingScreen`의 분기에 추가한다.
5. 다른 화면에서도 쓰는 입력칸은 `component/`에 두지 말고 uikit 이동을 검토한다.
6. `OnboardingViewModelTest`에 이동·검증·뒤로가기 테스트를, `app/src/androidTest/.../onboarding/OnboardingScreensTest`에 화면 테스트를 추가한다.

## 5. 테스트

- `src/test`: `OnboardingViewModelTest`, `OnboardingInviteLinkTest`, `presentation/NicknameGuidesTest`, `component/*ColorsTest`. 가짜 저장소는 `TestFamilyRepository`, `TestMemberRepository`.
- `app/src/androidTest/.../onboarding/`: `OnboardingScreensTest` (`FakeMemberModules` 사용)
