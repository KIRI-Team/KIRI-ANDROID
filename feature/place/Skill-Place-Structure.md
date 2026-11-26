# feature/place 구조와 작업 규칙

장소 탭(장소 검색, 필터, 키리 픽, 버킷 저장)을 담는 모듈이다. 공통 UI 규칙은 [`feature/Skill-UI-Structure.md`](../Skill-UI-Structure.md), 탭 이동 규칙은 [`Skill-Navigation.md`](../../app/src/main/java/com/kiri/app/navigation/Skill-Navigation.md)를 따른다.

## 1. 진입점과 경로

`navigation/PlaceNavHost.kt`의 `NavGraphBuilder.placeNavHost(navController)`가 장소 탭 그래프(`place_graph`)를 등록한다.

| 경로 | 화면 | ViewModel |
|---|---|---|
| `place` (시작) | `presentation/PlaceScreen.kt`의 `PlaceRoute` | `PlaceViewModel` |
| `place_graph/detail` | feature:detail의 `MissionPreviewRoute` | `MissionPreviewViewModel` (detail) |

- 쓰는 domain: `place.repository.PlaceRepository`(검색·저장), `place.model.PlaceFilters`(필터와 `matches`)

## 2. 패키지 구조

```
com.kiri.feature.place/
├── PlaceViewModel.kt
├── model/          PlaceUiState·UiEvent·UiEffect
├── navigation/     PlaceNavHost.kt
└── presentation/
    ├── PlaceScreen.kt              PlaceRoute, 검색창, 목록, 저장 확인 팝업, 저장 토스트
    ├── PlaceKiriPickSection.kt     키리 픽 가로 목록
    ├── PlaceFilterSheet.kt         필터 바텀시트
    ├── PlaceConditionFilters.kt    컨디션 필터 칩
    └── PlaceDatePicker.kt          날짜 선택 달력
```

## 3. 모듈 규칙

- 검색어·카테고리·필터가 바뀌면 `cursor`를 비우고 첫 페이지부터 다시 불러온다(`reload`). 진행 중이던 요청(`loadJob`)은 취소하고, 검색어 입력은 debounce한다. 목록 끝에서는 `nextCursor`로 다음 페이지를 이어 붙인다.
- 결과 개수는 서버가 준 `totalCount`를 보여준다. 불러온 개수로 계산하지 않는다.
- 저장 버튼 문구와 가능 여부는 domain의 `PlaceMission.saveLabel`, `canSave`를 쓴다.
- 필터 시트에서 고르는 값은 시트 안의 `draft`(`rememberSaveable`)에 두고, 적용(`onApply`)을 눌러야 `PlaceUiState`의 필터가 바뀐다.

## 4. 새 화면을 추가할 때

1. `presentation/`에 `XxxRoute`와 `XxxScreen(state, onEvent)`를 만들고, 상태가 있으면 `model/`과 모듈 루트에 UiState·ViewModel을 만든다.
2. `PlaceNavHost.kt`에 `composable("place_graph/xxx")`를 추가한다. 다른 탭으로 가는 이동은 `placeNavHost`에 콜백을 추가해 app에서 연결한다.
3. 필터 항목을 추가하면 domain의 `PlaceFilters`와 `matches()`를 먼저 고치고 domain 테스트를 추가한다.
4. 루트에 `testTag`를 붙이고, `src/test/.../PlaceViewModelTest`와 `src/androidTest/.../presentation/`에 테스트를 추가한다.

## 5. 테스트

- `src/test`: `presentation/PlaceViewModelTest`
- `src/androidTest`: `PlaceScreenTest`, `PlaceFilterSheetTest`
