# feature/home 구조와 작업 규칙

홈 탭(오늘의 질문, 주간 미션, 추천, 다가오는 약속)과 알림함, 가족 초대 화면을 담는 모듈이다. 공통 UI 규칙은 [`feature/Skill-UI-Structure.md`](../Skill-UI-Structure.md), 탭 이동 규칙은 [`Skill-Navigation.md`](../../app/src/main/java/com/kiri/app/navigation/Skill-Navigation.md)를 따른다.

## 1. 진입점과 경로

`navigation/HomeNavHost.kt`의 `NavGraphBuilder.homeNavHost(...)`가 홈 탭 그래프(`home_graph`)를 등록한다. app의 `KiriNavHost`가 호출한다.

| 경로 | 화면 | ViewModel |
|---|---|---|
| `home` (시작) | `presentation/HomeRoute.kt` → `HomeScreen` | `HomeViewModel` |
| `home/notifications` | `presentation/NotificationScreen.kt`의 `NotificationRoute` | `NotificationViewModel` |
| `home/invite/{inviteCode}` | `presentation/InviteFamilyScreen.kt` | 없음 |
| `home_graph/recommendation/{missionId}` | feature:detail의 `MissionPreviewRoute` | `MissionPreviewViewModel` (detail) |

- 다른 탭이나 모듈로 가는 이동(장소 탭, 일정 상세, 취향 설정, 가족 만들기/참여)은 `homeNavHost`의 콜백으로 받아 app이 연결한다.
- 쓰는 domain: `home.repository.HomeRepository`(가족·질문·미션·다가오는 일정), `HomeTutorialRepository`, `notification.repository.NotificationRepository`

## 2. 패키지 구조

```
com.kiri.feature.home/
├── HomeViewModel.kt, NotificationViewModel.kt
├── model/          HomeUiState·UiEvent·UiEffect, HomeUiDestination(이동 대상), HomeTutorialStep
├── navigation/     HomeNavHost.kt
└── presentation/
    ├── HomeRoute.kt, HomeScreen.kt          ViewModel 연결과 상태 비보유 화면
    ├── HomeContentSections.kt               상단 바, 취향 배너, 로딩·오류 화면 등 홈 섹션
    ├── HomeQuestionCard.kt, AnswerEditor.kt 오늘의 질문 카드(뒤집기)와 답변 입력
    ├── HomeTutorialOverlay.kt               처음 사용자 튜토리얼
    ├── HomePreviews.kt                      @Preview 모음
    ├── NotificationScreen.kt, InviteFamilyScreen.kt
```

## 3. 모듈 규칙

- 홈 화면 이동은 `HomeUiEffect.Navigate(HomeUiDestination)`로 보낸다. `HomeRoute`는 effect를 받아 콜백으로 대상을 넘기기만 하고, `HomeNavHost`의 `when (destination)`이 실제로 `navController.navigate`하거나 app 콜백을 부른다. Route와 화면은 NavController를 모른다.
- 둘러보기 사용자(`FamilyAccess.BROWSING`)는 가족 기능 대신 가족 만들기/참여 버튼(`home_create_family`, `home_join_family`)을 본다.
- 답변 길이는 domain의 `limitQuestionAnswerLength()`·`hasValidQuestionAnswerLength()`로 제한한다.
- 튜토리얼 표시 여부는 `HomeTutorialRepository`가 기기에 저장한다. 앱을 다시 켜도 다시 나오지 않아야 한다 (`HomeTutorialPersistenceTest`).
- feature:detail·record·onboarding의 화면을 재사용한다. 이 모듈들의 내부 패키지에 의존하지 말고 공개된 Route·route 함수만 쓴다.

## 4. 새 화면을 추가할 때

1. `presentation/`에 `XxxRoute`(ViewModel 연결, effect 처리)와 `XxxScreen(state, onEvent)`를 만든다. 상태가 있으면 `model/`에 `XxxUiState`·`UiEvent`·`UiEffect`를, 모듈 루트에 `XxxViewModel`을 만든다.
2. `HomeNavHost.kt`에 `composable("home/xxx")`를 추가한다. 경로는 `home/` 또는 `home_graph/`로 시작해 탭 이름 공간을 지킨다.
3. 홈 화면에서 들어가는 화면이면 `HomeUiDestination`에 항목을 추가하고, `HomeNavHost`의 `when (destination)`에 `navController.navigate("home/xxx")` 분기를 추가한다. 다른 탭으로 가면 `homeNavHost` 콜백을 늘린다.
4. 루트에 `testTag("xxx_screen")`을 붙인다.
5. ViewModel 테스트는 `src/test`에, 화면 테스트는 `src/androidTest/.../presentation/`에, 탭 이동은 `app/src/androidTest/.../navigation/`에 추가한다.

## 5. 테스트

- `src/test`: `HomeViewModelTest`, `HomeLoadFailureTest`, `HomeTutorialViewModelTest`, `NotificationViewModelTest`
- `src/androidTest`: `HomeScreenTest`, `HomeTutorialScreenTest`, `AnswerEditorTest`, `NotificationScreenTest`, `UiKitSpecTest`
- `app/src/androidTest/.../navigation/`: `WeeklyMissionNavigationTest`, `HomeMemoryNavigationTest`, `HomeTutorialPersistenceTest`, `BrowsingAccessNavigationTest`
