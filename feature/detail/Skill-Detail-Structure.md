# feature/detail 구조와 작업 규칙

미션 상세, 미션 미리보기(후기 포함), 컨디션 맞춤 화면을 담는 모듈이다. 자체 탭이 없고 **home·place·record가 각자의 그래프에서 이 화면들을 재사용한다.** 공통 UI 규칙은 [`feature/Skill-UI-Structure.md`](../Skill-UI-Structure.md)를 따른다.

## 1. 공개 화면과 쓰는 곳

| 화면 | 파일 | 상태 | 쓰는 곳 |
|---|---|---|---|
| 미션 미리보기 | `presentation/MissionPreviewScreen.kt` (`MissionPreviewRoute`) | `MissionPreviewViewModel`, `MissionPreviewUiState` | home 추천, place 상세 |
| 미션 상세 | `presentation/MissionDetailScreen.kt` | `DetailUiState`·`DetailUiEvent` (호출하는 모듈의 ViewModel이 만든다) | record 일정 상세 |
| 컨디션 맞춤 | `presentation/ConditionCustomScreen.kt` | `DetailUiState`·`DetailUiEvent` | record 일정 컨디션 |

- 이 모듈은 navigation을 갖지 않는다. 경로는 쓰는 모듈의 NavHost가 정한다 (예: `home_graph/recommendation/{missionId}`, `place_graph/detail`).
- 쓰는 domain: `mission.repository.MissionInfoRepository`, `MissionNearbyRepository`, `mission.model.*`(MissionDetail, Condition, NearbyPlace, `rankNearby`), `event.repository.AppEventRepository`(조회 기록)

## 2. 패키지 구조

```
com.kiri.feature.detail/
├── MissionPreviewViewModel.kt
├── model/          DetailUiState·DetailUiEvent, MissionPreviewUiState, MemoryDraft(기록 초안)
└── presentation/
    ├── MissionPreviewScreen.kt, MissionDetailScreen.kt, ConditionCustomScreen.kt
    ├── MissionReviewCard.kt     후기 카드, 미리보기 탭
    └── MemoryPreviewCard.kt     기록 사진·글 미리보기 (record도 사용)
src/debug/…/DetailPreviews.kt    @Preview 모음 (릴리스 빌드에 들어가지 않는다)
```

## 3. 모듈 규칙

- **화면을 재사용 가능하게 유지한다.** 화면은 콜백(`onBack`, `onSave`, `onNearbyMission` 등)만 받고 어느 탭에서 열렸는지 모른다. 탭별 차이는 매개변수(`showSaveAction` 등)로 받는다.
- `MissionDetailScreen`·`ConditionCustomScreen`은 ViewModel을 갖지 않는다. 상태는 호출하는 모듈(record)의 ViewModel이 `DetailUiState`로 만들어 넘긴다. 새 필드를 추가하면 호출하는 곳을 모두 확인한다.
- 주변 장소 순위는 domain의 `rankNearby()`를 쓴다. 서버가 순위를 정했으면(`nearbyRankedByServer`) 다시 정렬하지 않는다.
- `@Preview`는 `src/debug`에 둔다.

## 4. 새 화면을 추가할 때

1. 여러 탭에서 재사용할 미션 관련 화면만 이 모듈에 만든다. 한 탭에서만 쓰면 그 탭의 모듈에 만든다.
2. `presentation/XxxScreen.kt`를 만든다. ViewModel이 필요하면 모듈 루트에 `XxxViewModel`과 같은 파일에 `XxxRoute`를 두고, `model/`에 UiState를 만든다.
3. 쓰는 모듈의 NavHost에 `composable(...)`로 연결한다. 그 모듈의 `build.gradle.kts`에 `implementation(project(":feature:detail"))`가 있는지 확인한다.
4. 루트에 `testTag`를 붙이고, `src/androidTest/.../presentation/`에 화면 테스트를, 상태 계산은 `src/test`에 단위 테스트를 만든다.

## 5. 테스트

- `src/test`: `MissionPreviewViewModelTest`, `model/DetailUiStateNearbyTest`
- `src/androidTest`: `DetailScreenTest`, `MissionPreviewScreenTest`, `MemoryPhotoTest`
