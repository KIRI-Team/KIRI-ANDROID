# domain 모듈 구조와 작업 규칙

이 문서는 `:domain` 모듈에 모델, Repository 인터페이스, UseCase를 추가하거나 옮기기 전에 읽는다.

## 1. 역할과 의존 방향

- 앱이 다루는 **업무 모델과 계약(Repository 인터페이스)** 을 정의한다. 구현은 `:data`가 한다.
- `feature:*`와 `app`은 `domain`만 보고 `data`를 모른다. `domain`은 다른 프로젝트 모듈을 의존하지 않는다.
- 의존성은 `kotlinx.coroutines.core`(api) 하나뿐이다. Android, Retrofit, 직렬화, Hilt 같은 구현 기술을 들이지 않는다.
  - 그래서 UseCase에는 `@Inject`를 붙이지 않는다. 주입이 필요하면 data의 Hilt 모듈에서 `@Provides`로 만든다 (`data/family/di/FamilyModule`의 `provideAcceptPendingInviteUseCase` 참고).

## 2. 패키지 구조

**기능 → 역할** 순서로 나눈다. 기능 이름은 `:data`의 기능 패키지와 1:1로 맞춘다.

```
com.kiri.domain/
├── common/                 여러 기능이 함께 쓰는 것 (KiriTime: 한국 시간 기준 오늘·지금)
└── <기능>/
    ├── model/              데이터 클래스, enum, 도메인 예외, 순수 규칙 함수
    ├── repository/         XxxRepository 인터페이스
    └── usecase/            XxxUseCase (필요할 때만)
```

| 기능 | 내용 |
|---|---|
| `auth` | 로그인 결과, 소셜 로그인 종류, `AuthRepository`, `SessionRepository` |
| `member` | 내 정보, 가족 역할, 닉네임 규칙(`NicknameValidator`) |
| `family` | 가족, 가족 주소, 가족 공간 이용 상태(`FamilyAccess`) |
| `invite` | 가족 초대 링크, 대기 중 초대, `AcceptPendingInviteUseCase` |
| `home` | 홈 화면 집계(`HomeRepository`: 가족·오늘의 질문·다가오는 일정), 홈 튜토리얼 |
| `question` | 질문 기록, 답변 길이 규칙 |
| `mission` | 미션 상세·컨디션·주변 장소 순위, 미션 정보 |
| `schedule` | 캘린더 일정, 일정 상세·수정, 다가오는 일정, 일정 초대 링크, 일정 컨디션, `GetScheduleEditAccessUseCase` |
| `review` | 일정 후기, 후기 사진 |
| `place` | 장소 검색 필터와 결과 |
| `bucket`, `taste`, `term`, `suggestion`, `notification`, `push`, `event`, `image` | 이름 그대로의 기능 |

- 역할 패키지(`model/`, `repository/`, `usecase/`)는 파일이 하나뿐이어도 만든다. 어느 기능이든 같은 위치에서 찾을 수 있게 하기 위해서다.
- `home`은 화면 단위로 모아 주는 예외적인 기능이다. 새 데이터는 `home`에 넣지 말고 해당 기능(`family`, `question`, `schedule`)에 만든다.
- 테스트(`src/test`)도 같은 패키지에 둔다. 예: `schedule/model/ScheduleDetailTest.kt`

## 3. 모델 작성 규칙

- 화면 문구나 UI 상태가 아니라 **서버·업무 기준의 값**을 둔다. UI 전용 값은 feature의 `UiState`에서 계산한다.
- 다른 값에서 계산할 수 있는 규칙은 확장 함수나 계산 프로퍼티로 둔다 (`CalendarSchedule.occupiesDay`, `FamilyHome.access`). 규칙은 domain 단위 테스트로 검증한다.
- 실패를 나타내는 도메인 예외는 `model/`에 둔다 (`DuplicateNicknameException`, `NoFamilyException`). 화면은 예외 종류로 분기한다.
- 날짜·시간은 `java.time`을 쓰고, "오늘"은 `Clock.kiriToday()`로 구한다. 기기 시간대를 직접 쓰지 않는다.

## 4. Repository 인터페이스 규칙

- 이름은 `XxxRepository`로 짓고, 구현체는 data에서 `XxxRepositoryImpl`로 짓는다.
- 서버를 부르는 함수는 `suspend`로 만들고, 실패를 화면이 처리해야 하면 `Result<T>`를 돌려준다.
- 구현 방식(Retrofit, DataStore, DTO)이 드러나는 타입을 시그니처에 넣지 않는다.

## 5. UseCase는 필요할 때만 만든다

- 여러 Repository를 조합하거나, 화면 여러 곳이 같은 규칙을 반복할 때만 만든다. Repository 함수 하나를 그대로 감싸는 UseCase는 만들지 않는다.
- 이름은 `동사 + 대상 + UseCase`로 짓고 `operator fun invoke`를 하나만 공개한다.
- 지금 있는 것: `invite/usecase/AcceptPendingInviteUseCase`, `schedule/usecase/GetScheduleEditAccessUseCase`

## 6. 새 기능을 추가할 때 체크리스트

1. `com.kiri.domain.<기능>/model/`에 모델을, `repository/`에 `XxxRepository`를 만든다. 기능 이름은 data와 같게 짓는다.
2. 규칙 함수·계산 프로퍼티가 있으면 `src/test`에 같은 패키지로 단위 테스트를 만든다.
3. `:data`에 같은 이름의 기능 패키지를 만들어 구현한다 ([`data/Skill-Data-Structure.md`](../data/Skill-Data-Structure.md)).
4. feature 테스트에서 쓸 가짜 구현은 각 feature의 `src/test`에 둔다 (`TestFamilyRepository` 등).
5. `./gradlew :domain:testDebugUnitTest`로 검증한다.
