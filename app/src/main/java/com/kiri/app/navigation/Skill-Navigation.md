# Kiri BottomBar / Navigation 작업 시 참고사항

이 문서는 `com.kiri.app.navigation` 패키지(`KiriApp`, `MainScreen`, `KiriNavHost`, `KiriBottomBar`, `TopLevelDestination`)를 수정하기 전에 반드시 읽어야 하는 설계 규칙을 정리한다.

## 1. 두 단계 NavHost 구조

이 앱은 NavController를 두 계층으로 나눠 쓴다.

- **Root NavHost** (`KiriApp.kt`): `auth` → `onboarding` → `main` 3개 route만 가진다. 로그인/온보딩 통과 여부만 책임지고, 탭이나 바텀바에 대해서는 전혀 모른다. `auth`와 `onboarding`은 하나의 진입 플로우로 취급한다 (6번 참고).
- **Main NavHost** (`MainScreen.kt` → `KiriNavHost.kt`): `main` route 진입 후 별도의 `rememberNavController()`로 만들어지는 탭 전용 컨트롤러다. 바텀바는 이 컨트롤러만 조작한다.

바텀바 관련 로직을 수정할 때 Root NavHost와 Main NavHost의 NavController를 절대 섞지 않는다. `KiriBottomBar`에는 반드시 `MainScreen`이 만든 NavController만 전달한다.

**탭 그래프는 모두 feature 모듈이 소유한다.** `KiriNavHost`는 각 모듈의 `NavGraphBuilder` 확장 함수를 호출하고 콜백만 연결한다.

| 탭 | 그래프 함수 | 그래프 route | 시작 route |
|---|---|---|---|
| 홈 | `feature/home`의 `homeNavHost(...)` | `home_graph` | `home` |
| 플레이스 | `feature/place`의 `placeNavHost(...)` | `place_graph` | `place` |
| 기록 | `feature/record`의 `recordNavHost(...)` | `record_graph` | `record` |
| 마이페이지 | `feature/mypage`의 `mypageGraph(...)` | `mypage_graph` | `mypage` |

- 탭별 그래프는 Main NavController 하나에 등록한다. 별도 컨트롤러를 중첩 생성하지 않아 `saveState` / `restoreState` 다중 백스택을 유지한다.
- 화면 Route/Screen은 컨트롤러 대신 콜백을 받고, 컨트롤러는 각 모듈의 `navigation/` 패키지에서만 사용한다.
- 공유 ViewModel의 이동 effect는 각 Route의 lifecycle이 `RESUMED`일 때만 수집한다. 전환 애니메이션 중에 이전 화면이 다음 화면의 effect를 소비하는 것을 막는다.

## 2. 탭 전환은 `navigateToTab()`으로만 한다

탭 전환 로직은 `KiriBottomBar.kt`의 `NavHostController.navigateToTab(destination)` 하나에 있다. 바텀바 클릭과 코드에서 하는 탭 이동(예: 홈의 `onOpenPlaces`)이 모두 이 함수를 쓴다.

```kotlin
internal fun NavHostController.navigateToTab(destination: TopLevelDestination) {
    navigate(destination.graphRoute) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
```

- `saveState = true` / `restoreState = true`: 탭을 벗어났다가 돌아왔을 때 그 탭이 쌓아 둔 화면을 그대로 복원한다. 빼면 탭을 바꿀 때마다 각 탭이 시작 화면으로 리셋된다.
- `launchSingleTop = true`: 같은 탭을 다시 누를 때 그래프가 중복으로 쌓이는 것을 막는다.
- 이 세 옵션은 "다중 백스택(Multiple back stacks)" 패턴의 필수 조합이다. 하나라도 빠지면 탭별 독립 백스택이 깨진다.
- **다른 탭으로 옮겨 가야 하면 `navigate(graphRoute)`를 직접 부르지 말고 `navigateToTab()`을 쓴다.**
- 탭 시작 화면이 아니라 다른 탭의 하위 화면으로 바로 가는 이동(예: 홈 → `mypage_graph/my_preference`, 홈 → 기록의 일정 상세)은 `KiriNavHost`의 콜백에서 `navController.navigate(route) { launchSingleTop = true }`로 연결한다.

## 3. 상세 route는 탭마다 따로 둔다

상세 화면 경로는 각 탭 그래프의 이름 공간 안에 둔다.

| 탭 | 상세 경로 | 화면 |
|---|---|---|
| 홈 | `home_graph/recommendation/{missionId}` | detail의 `MissionPreviewRoute` |
| 플레이스 | `place_graph/detail` | detail의 `MissionPreviewRoute` |
| 기록 | `record_graph/server-schedule/{scheduleId}...` | record의 `ServerScheduleDetailRoute` |

- 같은 Composable을 여러 탭에서 재사용하더라도 **route는 공용으로 묶지 않는다.** `"detail"`처럼 하나의 route를 여러 그래프에서 참조하면 백스택이 합쳐지거나 인자 전달이 꼬인다.
- 다른 모듈에서 들어와야 하는 상세 경로는 소유 모듈이 route 생성 함수를 공개한다 (`serverMissionDetailRoute(missionId)`, `serverScheduleDetailRoute(scheduleId, fromHome, respond)`). route 문자열을 직접 조립하지 않는다.
- `TopLevelDestination.detailRoute()`는 예전 구조에 남아 있는 함수로 지금은 쓰는 곳이 없다. 새 코드에서 쓰지 않는다.

## 4. 바텀바는 탭 시작 화면에서만 보인다

`MainScreen.kt`는 현재 route가 **각 탭의 시작 route(`home`, `place`, `record`, `mypage`)일 때만** `KiriBottomBar`를 띄운다. 상세·설정 같은 하위 화면에서는 바텀바가 사라지고, 뒤로 가서 탭 시작 화면에 돌아오면 다시 나타난다.

```kotlin
private val TAB_START_ROUTES = TopLevelDestination.entries.map { it.startRoute }.toSet()

val route = navController.currentBackStackEntryAsState().value?.destination?.route
val showBottomBar = route in TAB_START_ROUTES && !editingHomeAnswer
```

- **홈에서 답변을 작성하는 동안에도 숨긴다.** 홈 그래프의 `onAnswerEditingChanged` 콜백이 `editingHomeAnswer`를 바꾼다 (`homeNavHost(onAnswerEditingChanged = …)` → `KiriNavHost(onHomeAnswerEditingChanged = …)` → `MainScreen`).
- 하위 화면에서 다른 탭으로 가는 길은 뒤로가기로 탭 시작 화면에 돌아가는 것이다. 각 탭의 하위 화면 백스택은 2번의 `saveState`로 탭마다 보존된다.
- 특정 하위 화면에 바텀바를 다시 띄우려면 `showBottomBar` 조건을 바꾸고, `KiriAppNavigationTest.bottomBarIsHiddenOnDetailAndReturnsOnTab`도 함께 고친다.

### 떠 있는 바와 콘텐츠 여백

바텀바는 화면 위에 떠 있는 둥근 바(Figma 8389:5585)다. 탭 화면의 콘텐츠는 **바 뒤까지 그린다.**

```kotlin
val contentPadding = if (overlayBar) PaddingValues(top = padding.calculateTopPadding()) else padding
CompositionLocalProvider(LocalKiriBottomBarInset provides if (overlayBar) padding.calculateBottomPadding() else 0.dp) {
    Box(Modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(padding).testTag("main_content")) { KiriNavHost(...) }
}
```

- 바가 있으면 아래쪽 padding을 주지 않는다. 대신 바가 가리는 높이(바 높이 + 바깥 16dp 간격 + 시스템 내비게이션 인셋)를 uikit의 `LocalKiriBottomBarInset`으로 내려보낸다.
- **탭 시작 화면의 스크롤 영역은 마지막 항목 아래에 `LocalKiriBottomBarInset.current`만큼 여백을 더한다.** 그래야 끝까지 스크롤했을 때 마지막 항목과 버튼이 바에 가리지 않는다. 지금 홈(`HomeScreen`), 플레이스(`PlaceScreen` 목록), 기록(`RecordCalendar`, `RecordBucketList`), 마이페이지(`MypageScreen`)가 이렇게 한다. 새 탭 화면이나 새 스크롤 목록을 만들면 같은 여백을 넣는다.
- 바가 없는 하위 화면은 전체 `padding`을 적용해 시스템 내비게이션 바 위에서 끝난다. `LocalKiriBottomBarInset`은 0이다.
- `BottomNavigationSpacingTest`가 다음을 확인한다.
  - 모든 탭에서 콘텐츠가 바 뒤까지 그려지는지
  - 플레이스 목록의 마지막 저장 버튼이 바 위에 완전히 보이는지

## 5. 바텀바의 구성과 탭 제한

`KiriBottomBar.kt`가 탭 목록, 선택 표시, 아이콘을 직접 정한다.

- **탭 순서는 홈 → 플레이스 → 기록 → 마이페이지다.** `KiriBottomBar`의 `listOf(HOME, PLACE, RECORD, MY_PAGE)`가 순서를 정한다. `TopLevelDestination` enum 순서(HOME, RECORD, PLACE, MY_PAGE)와 다르니 enum 순서로 바를 그리지 않는다.
- 플레이스 탭의 바 문구는 `"플레이스"`다. `TopLevelDestination.PLACE.label`(`"장소"`)이 아니라 바에서 따로 정한다.
- 아이콘은 uikit의 `R.drawable.kiri_nav_*`(선택)와 `kiri_nav_*_disabled`(비선택)를 쓴다. `TopLevelDestination.icon`(Material 아이콘)은 바에서 쓰지 않는다.
- 선택 여부는 현재 destination의 `hierarchy`에 탭의 `graphRoute`가 있는지로 정한다. 바가 보이는 탭 시작 화면 기준으로 선택이 맞는다.
- **둘러보기 사용자는 가족 탭을 누를 수 없다.** `MainScreen`이 다음 두 콜백을 넘긴다.
  - `isBlocked = { it.requiresFamily && access == FamilyAccess.BROWSING }`
  - `onBlocked`: domain의 `BROWSING_BLOCKED_MESSAGE` 토스트
  - 막힌 탭은 이동하지 않는다. 지금 `requiresFamily`는 기록 탭(`RECORD`)뿐이다.
  - 가족 상태는 `FamilyAccessViewModel`이 화면에 돌아올 때(`ON_RESUME`)마다 다시 확인한다.

## 6. 로그인과 온보딩은 하나의 진입 플로우다

로그인(`auth`)과 온보딩(`onboarding`)은 다루는 정보가 달라서(계정 인증 / 사용자·가족 정보 설정) route를 나눴을 뿐, 전체적으로는 거의 하나의 플로우로 본다.

- **로그인 → 온보딩**: 로그인을 백스택에서 제거하지 않는다. 온보딩 첫 화면에서 뒤로가기를 누르면 로그인으로 돌아갈 수 있다.
- **온보딩 → 메인**: 로그인과 온보딩을 함께 제거한다(`popUpTo(ROUTE_AUTH) { inclusive = true }`). 메인에 진입한 뒤 시스템 뒤로가기를 눌러도 로그인/온보딩 화면으로 돌아가지 않고, 더 이상 갈 곳이 없으면 앱이 종료된다.
- 이 동작을 바꾸려면(예: 로그아웃 후 재로그인 플로우 추가) `popUpTo` 대상과 `inclusive` 값을 함께 검토해야 한다.

**카카오톡 일정 초대장으로 앱이 열린 경우**에는 `MainScreen`이 메인 그래프가 준비된 뒤(`route != null`) `serverScheduleDetailRoute(scheduleId, fromHome = true, respond = true)`로 이동한다. 로그인하지 않았으면 로그인을 마치고 메인에 들어올 때 연다 (`PendingScheduleInvite`).

## 7. Compose UI 테스트를 위한 testTag 규칙

| testTag | 위치 |
|---|---|
| `home_screen`, `record_screen`, `place_screen`, `mypage_screen`, `onboarding_screen`, `auth_screen` | 각 탭·진입 화면의 최상위 컨테이너 |
| `bottom_nav_${startRoute}` (`bottom_nav_home`, `bottom_nav_place`, `bottom_nav_record`, `bottom_nav_mypage`) | 바텀바의 탭 아이템 |
| `floating_bottom_bar` | 떠 있는 바 자체 (보이는지 확인할 때) |
| `bottom_nav_area` | 바와 바깥 간격을 포함한 영역 |
| `main_content` | Main NavHost를 감싸는 콘텐츠 영역 |

이 태그들은 화면 제목 텍스트("홈", "기록" 등)가 바텀바 라벨과 겹쳐 `onNodeWithText`가 여러 노드를 찾는 문제를 피하기 위해 존재한다. **route 이름이나 탭 구성을 바꿀 때는 `app/src/androidTest/.../navigation/`의 테스트(`KiriAppNavigationTest`, `BottomNavigationSpacingTest`, `BrowsingAccessNavigationTest`)의 태그 참조도 함께 갱신한다.**

## 8. 새 탭을 추가할 때 체크리스트

1. `settings.gradle.kts`에 feature 모듈을 추가하고, `app/build.gradle.kts`에 의존성을 추가한다.
2. feature 모듈의 `navigation/`에 `NavGraphBuilder.xxxNavHost(navController, ...)`를 만든다. 그래프 route는 `xxx_graph`, 시작 route는 `xxx`, 하위 경로는 `xxx_graph/...`로 짓는다.
3. `TopLevelDestination`에 항목을 추가한다(graphRoute/startRoute/label). 가족이 필요한 탭이면 `requiresFamily`에 넣는다.
4. `KiriBottomBar`의 탭 `listOf(...)`에 원하는 순서로 넣고, 아이콘 `when`에 선택/비선택 drawable을 추가한다(uikit `kiri_nav_*`).
5. `KiriNavHost.kt`에서 그래프 함수를 호출하고, 다른 탭으로 가는 이동은 콜백으로 연결한다(탭 시작 화면이면 `navigateToTab`).
6. 탭 시작 화면의 최상위에 `testTag("xxx_screen")`을 붙이고, 스크롤 영역 끝에 `LocalKiriBottomBarInset.current` 여백을 넣는다.
7. `KiriAppNavigationTest`에 탭의 독립 백스택 테스트를, `BottomNavigationSpacingTest`의 탭 목록에 새 탭을 추가한다.
8. `:app:assembleDebug`, `:app:lintDebug` 실행 후 결과 기록 (AGENTS.md 규칙)

## 9. 마이페이지 그래프는 feature 모듈이 등록한다

마이페이지는 목록에서 메뉴별 하위 화면으로 들어갔다 나오는 구조라서, 그래프 등록을 `feature/mypage`의 `com.kiri.feature.mypage.navigation.MypageNavigation.kt`가 맡는다. `KiriNavHost.kt`에서는 `mypageGraph(navController, onNavigateToLogin)` 한 줄만 호출한다.

- **route 상수는 feature가 소유한다**: `MYPAGE_GRAPH_ROUTE`(`"mypage_graph"`), `MYPAGE_ROUTE`(`"mypage"`)는 `MypageNavigation.kt`에 있고, `TopLevelDestination.MY_PAGE`는 이 상수를 참조한다. app이 feature를 의존하는 방향만 허용되므로, feature가 `TopLevelDestination`을 참조하도록 바꾸지 않는다.
- **메뉴 화면 추가 방법**: `MypageMenu` enum에 항목을 추가하고 `MypageNavigation.kt`의 `MypageMenuScreen` `when`에 분기를 추가한다. route(`mypage_graph/{메뉴 이름 소문자}`)는 enum 이름으로 자동 생성되므로 `KiriNavHost.kt`는 수정하지 않는다.
- **화면을 고를 때만 `when`을 쓴다**: 온보딩처럼 상태 하나로 화면을 전환하지 않는다. 메뉴 화면마다 별도 route로 백스택에 쌓아야 탭 재선택, 상태 보존(2번), 화면별 ViewModel 범위가 Navigation 기준으로 동작한다.
- **화면 Composable은 NavController를 모른다**: `NavController`는 `mypageGraph()` 안에서만 쓰고, 각 화면에는 `onBackClick` 같은 람다만 넘긴다. 다른 탭으로 이동해야 하면 feature의 route를 직접 참조하지 말고 `mypageGraph()`의 콜백 파라미터로 받아 `KiriNavHost.kt`에서 연결한다.
- **메인 밖으로 나가는 이동은 콜백으로 올린다**: 로그아웃·회원탈퇴 뒤 로그인 화면 이동은 `mypageGraph(onNavigateToLogin)` → `KiriNavHost` → `MainScreen(onNavigateToLogin)` → `KiriApp` 순서로 올려, 루트 NavController에서 `main`을 지우고 `auth`로 이동한다.
- `feature/mypage/build.gradle.kts`에는 이 때문에 `navigation-compose` 의존성이 있다.
