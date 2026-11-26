# uikit 모듈 구조와 작업 규칙

이 문서는 `:uikit`에 디자인 토큰이나 공용 컴포넌트를 추가하기 전에 읽는다.

## 1. 역할과 의존 방향

- KIRI 디자인 시스템(색, 글꼴, 공용 Compose 컴포넌트)을 담는다. 모든 feature와 `app`이 의존한다.
- 프로젝트 모듈 중 아무것도 의존하지 않는다. **`domain` 모델을 받지 않는다.** 컴포넌트는 문자열·숫자·람다 같은 원시 값만 받는다.
- 이미지 로딩용 Coil을 `api`로 노출한다.

## 2. 패키지 구조

```
com.kiri.uikit/
├── KiriTheme.kt, KiriTypography.kt   테마와 글꼴 스타일
├── LocalKiriClock.kt                 화면에서 쓰는 Clock (테스트에서 바꿔 끼운다)
├── color/
│   ├── KiriPrimitiveColor.kt         원색 팔레트 (직접 쓰지 않는다)
│   ├── KiriColor.kt                  의미 단위 색 (화면에서는 이것을 쓴다)
│   └── KiriComponentColor.kt         컴포넌트별 색 조합
├── component/                        공용 컴포넌트 (KiriButton, KiriTopBar, KiriTitle, KiriToast,
│                                      KiriDetailSheet, KiriEmptyState, KiriLoadingIndicator, KiriAsyncImage,
│                                      KiriInviteCodeCard, KiriInviteCodeLoadingScreen, KiriBottomBarInset,
│                                      KiriHomeComponents, KiriScheduleComponents, KiriDetailComponents …)
├── invite/InviteShare.kt             초대 링크 공유 시트
└── permission/NotificationPermissionPrompt.kt  알림 권한 요청
```

## 3. 규칙

- **색은 `KiriColor`·`KiriComponentColor`로 쓴다.** 화면에서 `Color(0xFF...)`나 `KiriPrimitiveColor`를 직접 쓰지 않는다. Figma에 새 토큰이 생기면 primitive → 의미 색 순서로 추가한다.
- 글꼴은 `KiriTypography` 스타일을 쓴다. `fontSize`를 화면에서 직접 정하지 않는다.
- 컴포넌트 이름은 `Kiri` 접두어를 붙인다. Figma 컴포넌트 이름(KIRI/Button 등)을 주석에 적어 둔다.
- 상태를 갖지 않는다. 선택·열림 여부는 매개변수로 받고 변화는 람다로 알린다. ViewModel·Hilt를 쓰지 않는다.
- 상호작용이 있는 컴포넌트에는 `testTag`를 붙인다 (`kiri_back_button`, `kiri_loading_indicator` 등). 이름을 바꾸면 이 태그를 쓰는 UI 테스트도 함께 고친다.
- 색 조합·위치 계산처럼 분기가 있는 로직은 순수 함수로 빼서 `src/test`에서 검증한다 (`KiriButtonColorsTest`, `KiriTooltipPlacementTest`).

## 4. 언제 uikit으로 옮기는가

- feature 안에서만 쓰는 컴포넌트는 그 feature의 `presentation/component/`에 둔다.
- **두 번째 feature가 같은 컴포넌트를 필요로 할 때 uikit으로 옮긴다.** 복사해서 쓰지 않는다.
- 옮길 때 feature 모델 타입을 원시 값 매개변수로 바꾼다.

## 5. 새 컴포넌트를 추가할 때 체크리스트

1. 이미 있는 컴포넌트로 만들 수 있는지 `component/`를 먼저 확인한다.
2. `component/KiriXxx.kt`에 만든다. 색·글꼴은 토큰을 쓰고, 매개변수 맨 끝에 `modifier: Modifier = Modifier`를 둔다.
3. 계산 로직이 있으면 `src/test/.../component/`에 단위 테스트를 만든다.
4. `./gradlew :uikit:testDebugUnitTest`로 검증하고, 쓰는 화면을 에뮬레이터에서 확인한다.
