# Kiri BottomBar / Navigation 작업 시 참고사항

이 문서는 `com.kiri.app.navigation` 패키지(`KiriApp`, `MainScreen`, `KiriNavHost`, `KiriBottomBar`, `TopLevelDestination`)를 수정하기 전에 반드시 읽어야 하는 설계 규칙을 정리한다.

## 1. 두 단계 NavHost 구조

홈·기록 모듈의 `navigation/*NavHost.kt`가 해당 탭의 `NavGraphBuilder` 등록과 내부 경로를 소유한다. `KiriNavHost`는 `homeNavHost`, `recordNavHost`를 호출하고, 장소·마이페이지 그래프는 기존처럼 앱에서 정의한다. 홈 상세·커스텀·초대 경로는 홈 모듈에서 정의한다.

탭별 그래프는 Main NavController에 등록한다. 별도 컨트롤러를 중첩 생성하지 않아 기존 `saveState` / `restoreState` 다중 백스택과 바텀바 표시를 유지한다. 화면 Route/Screen은 컨트롤러 대신 콜백을 받고, 컨트롤러는 `navigation/`에서만 사용한다.

공유 ViewModel의 이동 effect는 각 Route의 lifecycle이 `RESUMED`일 때만 수집한다. 전환 애니메이션 중 이전 화면이 다음 화면의 effect를 소비하는 것을 막는다.


이 앱은 NavController를 두 계층으로 나눠 쓴다.

- **Root NavHost** (`KiriApp.kt`): `auth` → `onboarding` → `main` 3개 route만 가진다. 로그인/온보딩 통과 여부만 책임지고, 탭이나 바텀바에 대해서는 전혀 모른다. `auth`와 `onboarding`은 하나의 진입 플로우로 취급한다 (5번 참고).
- **Main NavHost** (`MainScreen.kt` → `KiriNavHost.kt`): `main` route 진입 후 별도의 `rememberNavController()`로 만들어지는 탭 전용 컨트롤러다. 바텀바는 이 컨트롤러만 조작한다.

바텀바 관련 로직을 수정할 때 Root NavHost와 Main NavHost의 NavController를 절대 섞지 않는다. `KiriBottomBar`에는 반드시 `MainScreen`이 만든 NavController만 전달한다.

## 2. 탭 전환 시 `popUpTo` 옵션은 지우면 안 된다

`KiriBottomBar.kt`의 탭 클릭 로직:

```kotlin
navController.navigate(destination.graphRoute) {
    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
```

- `saveState = true` / `restoreState = true`: 탭을 벗어났다가 돌아왔을 때 그 탭이 쌓아뒀던 화면(예: Detail까지 들어간 상태)을 그대로 복원해주는 핵심 옵션이다. 이 옵션을 빼면 탭을 전환할 때마다 각 탭이 시작 화면으로 리셋된다.
- `launchSingleTop = true`: 같은 탭을 다시 누를 때 그래프가 중복으로 쌓이는 것을 막는다.
- 이 세 옵션은 구글이 권장하는 "다중 백스택(Multiple back stacks)" 패턴의 필수 조합이다. 하나라도 빠지면 탭별 독립 백스택이 깨진다.

## 3. Detail은 탭마다 별도 route로 선언되어 있다

`TopLevelDestination.detailRoute()`는 `"${graphRoute}/detail"`을 반환한다. 즉 실제 route 문자열은 `home_graph/detail`, `record_graph/detail`, `place_graph/detail`, `mypage_graph/detail`로 전부 다르다.

- `DetailScreen` 컴포저블 자체는 4곳에서 재사용하지만, **route는 절대 공용으로 묶지 않는다.** 만약 `"detail"`처럼 하나의 공용 route로 4개 그래프에서 동시에 참조하면 백스택이 예상과 다르게 합쳐지거나 인자 전달이 꼬일 수 있다.
- 새 탭을 추가하거나 Detail에 파라미터(예: itemId)를 추가할 때도 이 "탭별 namespaced route" 원칙을 유지한다.

## 4. Detail 화면에서도 바텀바는 항상 보여야 한다

`MainScreen.kt`는 `main` 그래프 안의 모든 화면(탭 시작 화면 + Detail)에서 예외 없이 `KiriBottomBar`를 표시한다.

```kotlin
Scaffold(bottomBar = { KiriBottomBar(navController) }) { padding ->
    KiriNavHost(navController, Modifier.padding(padding))
}
```

- 처음에는 "Detail 화면에서는 바텀바를 숨긴다"는 조건을 넣었었지만, 이렇게 하면 예를 들어 홈 탭에서 Detail을 보고 있는 도중에는 바텀바가 사라져서 다른 탭으로 전환할 방법 자체가 없어진다. 그 결과 "탭마다 Detail이 독립적으로 쌓인다"는 기능 자체를 사용자가 이용할 수 없게 된다 (`KiriAppNavigationTest`의 탭 전환 테스트가 이 문제를 실제로 잡아냈다).
- 따라서 Detail 화면에서 바텀바를 숨기는 방향으로 되돌리지 않는다. 특정 화면에서만 바텀바를 숨기고 싶다면, 그 화면에서도 다른 탭으로 즉시 전환할 수 있는 대안(예: 상단 바의 탭 전환 UI)이 함께 있어야 한다.

## 5. 로그인과 온보딩은 하나의 진입 플로우다

로그인(`auth`)과 온보딩(`onboarding`)은 다루는 정보가 달라서(계정 인증 / 사용자·가족 정보 설정) route를 나눴을 뿐, 전체적으로는 거의 하나의 플로우로 본다.

- **로그인 → 온보딩**: 로그인을 백스택에서 제거하지 않는다. 온보딩 첫 화면에서 뒤로가기를 누르면 로그인으로 돌아갈 수 있다.
- **온보딩 → 메인**: 로그인과 온보딩을 함께 제거한다(`popUpTo(ROUTE_AUTH) { inclusive = true }`). 그 결과 메인 화면(바텀바)에 진입한 뒤 시스템 뒤로가기를 눌러도 로그인/온보딩 화면으로 돌아가지 않고, 더 이상 갈 곳이 없으면 앱이 종료된다.
- 이 동작을 바꾸려면(예: 로그아웃 후 재로그인 플로우 추가) `popUpTo` 대상과 `inclusive` 값을 함께 검토해야 한다.

## 6. Compose UI 테스트를 위한 testTag 규칙

`HomeScreen`, `RecordScreen`, `PlaceScreen`, `MypageScreen`, `OnboardingScreen`, `AuthScreen`의 최상위 `Column`에는 각각 `"home_screen"`, `"record_screen"`, `"place_screen"`, `"mypage_screen"`, `"onboarding_screen"`, `"auth_screen"` testTag가 붙어 있다. `KiriBottomBar`의 각 탭 아이템에도 `"bottom_nav_${startRoute}"` testTag가 붙어 있다.

이 태그들은 화면 제목 텍스트("홈", "기록" 등)가 바텀바 라벨 텍스트와 문자열이 겹쳐서 `onNodeWithText`가 여러 노드를 찾아 실패하는 문제를 피하기 위해 존재한다. **route 이름이나 탭 구성을 바꿀 때는 `app/src/androidTest/java/com/kiri/app/navigation/KiriAppNavigationTest.kt`의 태그 참조도 함께 갱신한다.**

## 7. 새 탭을 추가할 때 체크리스트

1. `settings.gradle.kts`에 feature 모듈 추가 (필요 시)
2. `TopLevelDestination`에 enum 항목 추가 (graphRoute/startRoute/label/icon)
3. feature 모듈의 `navigation/*NavHost.kt`에 탭 그래프를 만들고 `KiriNavHost.kt`에서 등록 (시작 화면 + 탭 상세 경로)
4. 새 화면에 `onDetailClick` 콜백과 `testTag` 추가
5. `KiriAppNavigationTest.kt`에 해당 탭의 독립 백스택 테스트 추가
6. `:app:assembleDebug`, `:app:lintDebug` 실행 후 결과 기록 (AGENTS.md 규칙)
