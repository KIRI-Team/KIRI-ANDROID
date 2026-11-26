# data 모듈 구조와 작업 규칙

이 문서는 `:data` 모듈에 API, 저장소, Hilt 바인딩을 추가하거나 옮기기 전에 읽는다.

## 1. 역할과 의존 방향

- `:domain`의 Repository 인터페이스를 서버(Retrofit)와 기기 저장소(DataStore)로 구현한다.
- `app`만 `data`를 의존한다. `feature:*`는 `data`를 모른다. 예외는 `feature:record`의 테스트 소스뿐이다.
- 서버 주소 `KIRI_API_BASE_URL`은 `local.properties` → Gradle 속성 → 환경 변수 순서로 읽어 `BuildConfig.API_BASE_URL`에 넣는다.

## 2. 패키지 구조

**기능 → 역할** 순서로 나눈다. 기능 이름은 `:domain`과 1:1로 맞춘다 (`domain/schedule` ↔ `data/schedule`).

```
com.kiri.data/
├── common/
│   ├── network/   NetworkModule(Retrofit·OkHttp·kiriJson), ApiResponse, apiResult/apiCommand, KiriApiException, ApiBaseUrl
│   └── local/     LocalStateStore (JSON 스냅샷 저장)
└── <기능>/
    ├── remote/      XxxApi(Retrofit 인터페이스), 요청·응답 DTO, DTO → 도메인 변환 함수
    ├── local/       DataStore 기반 Store (기기에 남길 값이 있을 때만)
    ├── repository/  XxxRepositoryImpl, 테스트용 FakeXxxRepository
    └── di/          Hilt 모듈 (Api 제공, Repository 바인딩)
```

| 기능 | 특이사항 |
|---|---|
| `auth` | `remote/`에 `AuthInterceptor`(토큰 첨부, `@NoAuth`)와 `TokenAuthenticator`(401 → 재발급), `local/`에 `TokenStore` |
| `push` | 기기 FCM 토큰 등록. `PushDeviceSync`는 역할 패키지 없이 `push/`에 둔다 (app이 앱 시작 시 실행한다) |
| `invite` | 초대 링크(`InviteRepositoryImpl`)와 대기 중 초대(`PendingInviteRepositoryImpl`) |
| `schedule` | 캘린더·다가오는 일정, 일정 컨디션, 주변 장소 |
| `home` | `RemoteHomeRepository`를 `UpcomingHomeRepository`가 감싼다. 구현체가 둘이라 접두어로 구분한다 |

- 역할 패키지는 필요한 것만 만든다. 서버만 쓰는 기능에는 `local/`이 없다.
- 한 파일에 여러 역할이 섞여 있는 곳이 남아 있다 (`event/di/AppEventModule`, `suggestion/repository/SuggestionRepositoryImpl`, `family/di/FamilyModule`). 수정할 때 역할별 파일로 나눈다.
- 테스트(`src/test`, `src/androidTest`)도 대상 클래스와 같은 패키지에 둔다.

## 3. 가시성 규칙

- **모듈 밖에 공개하는 것은 Hilt Repository 모듈과 app이 직접 쓰는 타입뿐이다.** 나머지는 모두 `internal`로 둔다.
  - `internal`: Api, DTO, Store, `XxxRepositoryImpl`, Api 제공 모듈, `common/network`의 대부분
  - 공개: `XxxRepositoryModule`(app UI 테스트가 `@TestInstallIn`으로 바꿔야 함), `PushDeviceSync`·`PushTokenProvider`(app이 사용), `KiriApiException`, `FakeXxxRepository`
- 공개 클래스의 생성자가 internal 타입을 받으면 생성자에 `internal constructor`를 붙인다 (`PushDeviceSync` 참고).

## 4. 구현 패턴

**Api** (`remote/`)
```kotlin
internal interface TermApi {
    @GET("api/v1/terms")
    suspend fun getTerms(): ApiResponse<List<TermResponse>>
}

@Serializable
internal data class TermResponse(val termId: Long, val title: String /* ... */)

internal fun TermResponse.toTerm() = Term(id = termId, title = title /* ... */)
```
- 서버 응답은 모두 `ApiResponse<T>`로 받는다. 결과가 없는 명령은 `ApiResponse<JsonElement>`로 받는다.
- DTO는 서버 필드 이름을 그대로 쓰고, 도메인 변환 함수(`toXxx()`)를 같은 파일에 둔다. 화면에서 쓰지 않는 필드는 읽지 않는다.
- 로그인 토큰이 필요 없는 API에는 `@NoAuth`를 붙인다.

**RepositoryImpl** (`repository/`)
```kotlin
internal class TermRepositoryImpl @Inject constructor(
    private val api: TermApi,
    private val json: Json,
) : TermRepository {
    override suspend fun getTerms(): Result<List<Term>> =
        apiResult(json) { api.getTerms() }.map { terms -> terms.map { it.toTerm() } }
}
```
- 값을 받는 호출은 `apiResult`, 결과가 없는 호출은 `apiCommand`로 감싼다. 서버 오류는 `KiriApiException(code, message)`가 담긴 `Result.failure`가 된다.
- 구현체 이름은 `XxxRepositoryImpl`로 짓는다. 같은 인터페이스의 구현체가 여럿일 때만 접두어로 구분한다 (`RemoteHomeRepository`).

**Hilt 모듈** (`di/`)
```kotlin
@Module @InstallIn(SingletonComponent::class)
internal object TermApiModule {
    @Provides @Singleton
    fun provideTermApi(retrofit: Retrofit): TermApi = retrofit.create(TermApi::class.java)
}

// UI 테스트에서 가짜 저장소로 바꿀 수 있도록(@TestInstallIn) 따로 두고 공개한다
@Module @InstallIn(SingletonComponent::class)
abstract class TermRepositoryModule {
    @Binds internal abstract fun bindTermRepository(impl: TermRepositoryImpl): TermRepository
}
```
- Api 제공 모듈과 Repository 바인딩 모듈을 나눈다. UI 테스트는 Repository 모듈만 바꾼다 (`app/src/androidTest/.../FakeXxxModule.kt`).

## 5. 새 기능을 추가할 때 체크리스트

1. `:domain`에 모델과 `XxxRepository`가 있는지 확인한다 ([`domain/Skill-Domain-Structure.md`](../domain/Skill-Domain-Structure.md)).
2. `com.kiri.data.<기능>/remote/`에 `XxxApi`, DTO, 변환 함수를 `internal`로 만든다.
3. `repository/`에 `XxxRepositoryImpl`을 `internal`로 만든다. `apiResult`·`apiCommand`를 쓴다.
4. `di/`에 Api 제공 모듈(internal)과 Repository 바인딩 모듈(공개)을 만든다.
5. `src/test`에 같은 패키지로 `XxxRepositoryImplTest`를 만든다. MockWebServer로 요청 경로·본문, 성공 변환, 서버 오류 코드를 검증한다.
6. 화면 UI 테스트가 이 저장소를 쓰면 `app/src/androidTest`에 `FakeXxxModule`(`@TestInstallIn(replaces = [XxxRepositoryModule::class])`)을 만든다.
7. `./gradlew :data:testDebugUnitTest`로 검증한다.
