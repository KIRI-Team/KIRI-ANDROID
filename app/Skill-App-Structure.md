# app 모듈 구조와 작업 규칙

이 문서는 `:app`을 수정하기 전에 읽는다. 바텀바와 화면 이동 규칙은 [`navigation/Skill-Navigation.md`](src/main/java/com/kiri/app/navigation/Skill-Navigation.md)에 따로 있다.

## 1. 역할과 의존 방향

- 모든 모듈을 조립하는 유일한 곳이다. `data`를 아는 모듈은 `app`뿐이다. Hilt가 data의 구현체를 domain 인터페이스로 묶어 feature에 넣어 준다.
- 화면을 직접 만들지 않는다. 화면은 feature 모듈에 만들고, app은 NavHost에 연결만 한다.
- Firebase(Analytics, Messaging)처럼 Android 앱 전체에 걸린 기능을 둔다.

## 2. 패키지 구조

```
com.kiri.app/
├── KiriApplication.kt     @HiltAndroidApp, 앱 시작 시 PushDeviceSync 실행
├── MainActivity.kt        진입 Activity, 딥링크(초대·일정 초대) 수신
├── navigation/
│   ├── KiriApp.kt             Root NavHost (auth → onboarding → main)
│   ├── SessionStartup.kt      스플래시 동안 로그인 상태 확인 후 시작 경로 결정
│   ├── MainScreen.kt          바텀바 Scaffold, 둘러보기 사용자 차단
│   ├── KiriNavHost.kt         Main NavHost, 각 feature 그래프 연결
│   ├── KiriBottomBar.kt, TopLevelDestination.kt   탭 정의와 바텀바
│   ├── FamilyAccessViewModel.kt   OWNER / MEMBER / BROWSING 상태
│   └── PendingScheduleInvite.kt   카카오 일정 초대로 열린 일정 id를 메인 진입 때까지 보관
└── push/
    ├── KiriMessagingService.kt    FCM 수신, 토큰 갱신
    ├── PushModule.kt              PushTokenProvider(Firebase) 제공
    └── PushNotifications.kt       알림 채널, 포그라운드 알림 표시
```

## 3. 규칙

- feature 사이의 화면 이동은 **app이 콜백으로 연결한다.** feature가 다른 feature의 route 문자열을 직접 참조하지 않게, 각 그래프 함수(`homeNavHost(onOpenPlaces = …)`, `mypageGraph(onNavigateToLogin = …)`)의 람다로 받아 `KiriNavHost`에서 잇는다.
- Context가 필요한 앱 전역 처리(알림, 딥링크)는 app에 둔다. feature에는 결과만 콜백이나 이벤트로 넘긴다.
- data의 구현 타입을 화면 코드에서 쓰지 않는다. 예외는 앱 생명주기와 묶인 `PushDeviceSync`·`PushTokenProvider`뿐이다.

## 4. 테스트

- `src/test`: 순수 로직 (`PendingScheduleInviteTest`, `PushContentTest`)
- `src/androidTest`: Hilt를 쓰는 화면·이동 테스트. `HiltTestRunner`가 테스트 앱을 띄운다.
  - 서버 대신 가짜 저장소를 쓴다. `FakeXxxModule`이 `@TestInstallIn(replaces = [XxxRepositoryModule::class])`로 data의 Repository 모듈을 바꾼다.
  - 화면 이동 테스트는 `navigation/`에, 기능별 화면 테스트는 `auth/`, `mypage/`, `onboarding/`, `record/`에 둔다.

## 5. 새 화면·탭을 연결할 때 체크리스트

1. 화면은 해당 feature 모듈에 만든다 (각 feature의 `Skill-*-Structure.md`).
2. feature가 `NavGraphBuilder.xxxNavHost()`/`xxxGraph()`를 제공하면 `KiriNavHost`에서 호출하고, 다른 탭으로 가는 이동은 콜백으로 연결한다.
3. 새 탭이면 [`Skill-Navigation.md`](src/main/java/com/kiri/app/navigation/Skill-Navigation.md)의 8번 체크리스트를 따른다.
4. 새 Repository를 쓰는 화면이면 `src/androidTest`에 `FakeXxxModule`을 만든다.
5. `:app:assembleDebug`, `:app:lintDebug`, 관련 `connectedDebugAndroidTest`를 실행하고, 에뮬레이터에서 화면을 직접 확인한다 (AGENTS.md).
