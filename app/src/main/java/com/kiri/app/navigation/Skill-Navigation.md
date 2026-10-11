# Kiri BottomBar / Navigation 작업 시 참고사항

이 문서는 `com.kiri.app.navigation` 패키지(`KiriApp`, `MainScreen`, `KiriNavHost`, `KiriBottomBar`, `TopLevelDestination`)를 수정하기 전에 반드시 읽어야 하는 설계 규칙을 정리한다.

## 1. 두 단계 NavHost 구조

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

`TopLevelDestination.detailRoute()`는 `"${graphRoute}/detail"`을 반환한다. 즉 실제 route 문자열은 `home_graph/detail`, `record_graph/detail`, `place_graph/detail`로 전부 다르다. 마이페이지 탭에는 Detail이 없고, 대신 메뉴별 하위 화면이 `mypage_graph/{메뉴 이름 소문자}`(예: `mypage_graph/notification_setting`) route로 등록되어 있다 (8번 참고).

- `DetailScreen` 컴포저블 자체는 3곳에서 재사용하지만, **route는 절대 공용으로 묶지 않는다.** 만약 `"detail"`처럼 하나의 공용 route로 여러 그래프에서 동시에 참조하면 백스택이 예상과 다르게 합쳐지거나 인자 전달이 꼬일 수 있다.
- 새 탭을 추가하거나 Detail에 파라미터(예: itemId)를 추가할 때도 이 "탭별 namespaced route" 원칙을 유지한다.

## 4. Detail 화면에서도 바텀바는 항상 보여야 한다

`MainScreen.kt`는 `main` 그래프 안의 모든 화면(탭 시작 화면 + Detail + 마이페이지 메뉴 화면)에서 예외 없이 `KiriBottomBar`를 표시한다.

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
3. `KiriNavHost.kt`에 `navigation { ... }` 블록 추가 (시작 화면 + `detailRoute()` composable). 탭 안에 하위 화면이 여러 개라면 8번처럼 feature 모듈의 `NavGraphBuilder` 확장 함수로 등록한다
4. 새 화면에 `onDetailClick` 콜백과 `testTag` 추가
5. `KiriAppNavigationTest.kt`에 해당 탭의 독립 백스택 테스트 추가
6. `:app:assembleDebug`, `:app:lintDebug` 실행 후 결과 기록 (AGENTS.md 규칙)

## 8. 마이페이지 그래프는 feature 모듈이 등록한다

마이페이지는 목록에서 메뉴별 하위 화면으로 들어갔다 나오는 구조라서, 그래프 등록을 `feature/mypage`의 `com.kiri.feature.mypage.navigation.MypageNavigation.kt`가 맡는다. `KiriNavHost.kt`에서는 `mypageGraph(navController, onNavigateToLogin)` 한 줄만 호출한다.

- **route 상수는 feature가 소유한다**: `MYPAGE_GRAPH_ROUTE`(`"mypage_graph"`), `MYPAGE_ROUTE`(`"mypage"`)는 `MypageNavigation.kt`에 있고, `TopLevelDestination.MY_PAGE`는 이 상수를 참조한다. app이 feature를 의존하는 방향만 허용되므로, feature가 `TopLevelDestination`을 참조하도록 바꾸지 않는다.
- **메뉴 화면 추가 방법**: `MypageMenu` enum에 항목을 추가하고 `MypageNavigation.kt`의 `MypageMenuScreen` `when`에 분기를 추가한다. route(`mypage_graph/{메뉴 이름 소문자}`)는 enum 이름으로 자동 생성되므로 `KiriNavHost.kt`는 수정하지 않는다.
- **화면을 고를 때만 `when`을 쓴다**: 온보딩처럼 상태 하나로 화면을 전환하지 않는다. 메뉴 화면마다 별도 route로 백스택에 쌓아야 탭 재선택, 상태 보존(2번), 화면별 ViewModel 범위가 Navigation 기준으로 동작한다.
- **화면 Composable은 NavController를 모른다**: `NavController`는 `mypageGraph()` 안에서만 쓰고, 각 화면에는 `onBackClick` 같은 람다만 넘긴다. 다른 탭으로 이동해야 하면 feature의 route를 직접 참조하지 말고 `mypageGraph()`의 콜백 파라미터로 받아 `KiriNavHost.kt`에서 연결한다.
- **메인 밖으로 나가는 이동은 콜백으로 올린다**: 로그아웃·회원탈퇴 뒤 로그인 화면 이동은 `mypageGraph(onNavigateToLogin)` → `KiriNavHost` → `MainScreen(onNavigateToLogin)` → `KiriApp` 순서로 올려, 루트 NavController에서 `main`을 지우고 `auth`로 이동한다.
- `feature/mypage/build.gradle.kts`에는 이 때문에 `navigation-compose` 의존성이 있다.
