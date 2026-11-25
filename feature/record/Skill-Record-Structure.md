# feature/record 구조와 작업 규칙

기록 탭(캘린더, 버킷리스트, 질문·사진 기록)과 일정 등록·요청·상세·수정·후기를 담는 모듈이다. 화면이 가장 많고 흐름이 복잡하니 수정 전에 끝까지 읽는다. 공통 UI 규칙은 [`feature/Skill-UI-Structure.md`](../Skill-UI-Structure.md), 탭 이동 규칙은 [`Skill-Navigation.md`](../../app/src/main/java/com/kiri/app/navigation/Skill-Navigation.md)를 따른다.

## 1. 진입점과 경로

`navigation/RecordNavHost.kt`의 `NavGraphBuilder.recordNavHost(navController, onNearbyMission)`가 기록 탭 그래프(`record_graph`)를 등록한다.

| 경로 | 화면 | ViewModel |
|---|---|---|
| `record` (시작) | `presentation/RecordScreen.kt`의 `RecordRoute` (캘린더 / 버킷 / 기록 탭) | `RecordViewModel` |
| `record_graph/server-schedule/{scheduleId}...` | `presentation/ServerScheduleDetailRoute.kt` (일정 상세·응답) | `ServerScheduleDetailViewModel` |
| `.../{scheduleId}/review` | `presentation/MemoryEditorScreen.kt` (후기 작성) | `ServerScheduleDetailViewModel` |
| `.../{scheduleId}/custom` | feature:detail의 `ConditionCustomScreen` | `ServerScheduleDetailViewModel` |
| `record_graph/schedule-edit/...` | `presentation/schedule/ScheduleEditRoute.kt` | `ScheduleEditViewModel` |
| `record_graph/bucket-schedule/...` | `presentation/schedule/BucketScheduleCreateRoute.kt` (버킷으로 일정 잡기) | `BucketScheduleCreateViewModel` |

- 다른 모듈은 `serverScheduleDetailRoute(scheduleId, fromHome, respond)`로 일정 상세에 들어온다 (홈의 다가오는 약속, 카카오 일정 초대).
- 화면 사이의 결과(보낸 토스트, 수정 완료)는 `SavedStateHandle` 키(`scheduleSentToast`, `scheduleEdited`)로 이전 화면에 돌려준다.
- 쓰는 domain: `schedule.*`(CalendarSchedule, UpcomingSchedule, ScheduleCondition, ScheduleNearby, `GetScheduleEditAccessUseCase`), `bucket`, `review.ScheduleReviewRepository`, `question.QuestionHistoryRepository`, `mission.MissionInfoRepository`, `home.HomeRepository`(가족 구성원)

## 2. 패키지 구조

```
com.kiri.feature.record/
├── RecordViewModel.kt, ServerScheduleDetailViewModel.kt, ScheduleEditViewModel.kt, BucketScheduleCreateViewModel.kt
├── ScheduleDayLimit.kt          하루 일정 개수 제한
├── model/                       RecordUiState·RecordUiEvent
├── navigation/RecordNavHost.kt  경로, 화면 간 결과 전달
└── presentation/
    ├── RecordScreen.kt, RecordMemoryScreen.kt, MemoryEditorScreen.kt, ServerScheduleDetailRoute.kt
    ├── calendar/    캘린더 셀·일정 카드·선택한 날 활동
    ├── bucket/      버킷리스트 목록·카드 묶음·옵션
    ├── schedule/    일정 등록 흐름(ScheduleRegisterFlow/Screen/Draft), 요청, 수락 시트, 수정, 카카오 공유
    └── sheet/       기록 탭이 함께 쓰는 바텀시트 (RecordSheet 틀, 옵션·구성원 선택·일정·보내는 방법, 시트용 달력)
```

## 3. 모듈 규칙

- **일정을 수정·취소·응답할 수 있는지는 서버 값으로 정한다.** 상세 API의 `canRespond`·`canEdit`·`canCancel`과 `GetScheduleEditAccessUseCase`를 쓰고, 화면에서 날짜·작성자로 다시 판단하지 않는다.
- 캘린더 표시(시계=확정 약속, 하트=버킷, 사진=완료 기록, 초록 칸=오늘)는 `calendar/ScheduleCellLook`의 순수 함수로 정하고 단위 테스트로 검증한다.
- 일정 등록은 `ScheduleRegisterDraft`에 초안을 모으고 단계별 시트로 채운다. 보내는 방법(앱 알림 / 카카오)은 domain의 `ScheduleRequestChannel`이다. 카카오 공유는 `schedule/KakaoScheduleInviteShare`에서만 SDK를 부른다.
- 새 바텀시트는 `sheet/RecordSheet.kt`의 `RecordModalSheet`, `RecordSheetTitle`, `RecordSheetBottomAction`으로 만든다.
- 둘러보기 사용자는 기록 탭에 들어올 수 없다 (app의 `MainScreen`이 막는다).
- `README.md`에는 예전 가짜 저장소 기준의 설명이 남아 있다. 이 문서와 다르면 코드와 이 문서를 기준으로 한다.

## 4. 새 화면을 추가할 때

1. 위치를 고른다: 캘린더/버킷/기록 탭의 일부면 `presentation/<탭>/`, 일정 흐름이면 `presentation/schedule/`, 시트면 `presentation/sheet/`.
2. 별도 경로가 필요한 화면은 `XxxRoute`(ViewModel 연결, effect 처리)와 `XxxScreen`을 만들고, ViewModel은 모듈 루트에 둔다. 인자는 `SavedStateHandle`로 받고 `XXX_ARG` 상수를 ViewModel에 둔다.
3. `RecordNavHost.kt`에 `record_graph/xxx` 경로를 추가한다. 다른 모듈에서 들어와야 하면 공개 route 함수(`fun xxxRoute(...)`)를 만든다.
4. 이전 화면에 결과를 돌려줘야 하면 `SavedStateHandle` 키 상수를 `RecordNavHost.kt`에 추가한다.
5. 루트에 `testTag`를 붙인다. ViewModel 테스트는 `src/test`, 화면 테스트는 `src/androidTest/.../presentation/`, 탭 이동은 `app/src/androidTest/.../navigation/RecordBucketScheduleNavigationTest` 등에 추가한다.

## 5. 테스트

- `src/test`: ViewModel별 테스트(`RecordViewModelTest`, `ServerScheduleDetailViewModelTest`, `ScheduleEditViewModelTest`, `BucketScheduleCreateViewModelTest`), 흐름 테스트(`ScheduleActionsTest`, `ReviewEditorTest`, `ReviewUpdateTest`, `WeeklyServerScheduleTest`), 순수 함수 테스트(`presentation/**`). 이 모듈의 테스트만 `:data`의 Fake 저장소를 쓸 수 있다(`testImplementation(project(":data"))`).
- `src/androidTest`: `RecordScreenTest`, `ServerScheduleDetailScreenTest`, `MemoryEditorScreenTest`, `schedule/*ScreenTest`, `sheet/RecordSheetsTest`
- `app/src/androidTest/.../record/FakeXxxModule.kt`: 앱 단위 UI 테스트에서 일정·버킷·후기 저장소를 가짜로 바꾼다.
