# KIRI ANDROID

끼리 Android 앱의 Jetpack Compose 멀티모듈 프로젝트입니다.

## 모듈

- `:app`: 앱 진입점과 기능 연결
- `build-logic` (included build): Android 라이브러리 및 Compose 공통 Gradle 설정
- `:core`: 여러 계층에서 공유하는 범용 코드
- `:uikit`: 공통 테마와 UI 컴포넌트
- `:domain`: 비즈니스 모델과 저장소 인터페이스
- `:data`: 저장소 구현, 네트워크 및 로컬 데이터
- `:feature:home`, `auth`, `onboarding`, `detail`, `record`, `place`, `mypage`: 기능별 화면

`feature → domain`, `data → domain`, `app → feature` 방향으로 의존합니다. 현재 로그인·온보딩, 홈, 미션 상세·컨디션 조정, 플레이스 탐색·저장·리뷰, 마이페이지 UI를 제공합니다. 알림함과 가족추억 작성은 대체 데이터 및 화면 내 임시 미리보기로 동작합니다. 실제 서버 인증·알림 전송·사진 업로드·기록 저장은 연결하지 않았습니다.

Android Studio에서 루트 폴더를 열고 JDK 17 및 Android SDK 36을 설정한 뒤 `./gradlew :app:assembleDebug`로 빌드합니다.

PR 검증은 GitHub Actions에서 빌드, 단위 테스트, Android Lint를 실행합니다. 기능별 테스트 작성 기준은 `AGENTS.md`를 참고하세요. 화면 동작을 테스트하려면 해당 화면과 흐름을 구현하면서 Compose UI 테스트를 추가합니다.

## Gradle 기준과 참고 범위

- 버전의 단일 출처는 `gradle/libs.versions.toml`이며 included build인 `build-logic`도 같은 catalog를 읽습니다. Gradle 자체 버전은 wrapper에서 관리합니다.
- 검증 조합: AGP 8.13.2 / Kotlin 및 Compose compiler plugin 2.2.21 / Gradle 8.13 / JDK 17 / compileSdk·targetSdk 36 / minSdk 26 / Build Tools 36.0.0 / Compose BOM 2025.12.00.
- 기존 BOM 2026.09.00은 SDK 37 및 AGP 9.1 이상을 요구하는 Compose를 선택했습니다. BOM을 호환 범위로 낮추고 AAR metadata 검사는 그대로 유지합니다. SDK 37 설치는 필요하지 않습니다.
- DayTodo_FE의 루트 catalog, build-logic의 catalog 공유, Android/Compose/feature convention 분리, feature/home의 `presentation`, `presentation/component`, `model` 구성을 참고했습니다.
- DayTodo 현재 조합(AGP 9.1.0 / Kotlin 2.3.21 / Gradle 9.3.1 / BOM 2026.06.00)은 AGP 9의 내장 Kotlin 설정을 사용합니다. KIRI는 기존 AGP 8과 명시적 Kotlin Android plugin을 유지하여 부분적인 AGP 9 이식을 피합니다.
- KIRI는 작은 precompiled script convention을 유지합니다. DayTodo의 Hilt·Retrofit·Firebase·Kakao·Naver 설정, DTO, API 주소, 화면 디자인 및 리소스는 가져오지 않았습니다. 이 항목은 초기 모듈 설정 당시의 참고 범위이며, 이후 기능 구현에서 navigation 및 ViewModel 의존성이 추가되었습니다.
- `kiri.library`는 Android/JDK/단위 테스트 의존성, `kiri.compose`는 Compose 활성화와 BOM, `kiri.feature`는 library+Compose 및 domain/uikit 의존성을 담당합니다.
- `domain`과 `core`는 기존 Android library 형태를 유지합니다. domain에는 Android UI 또는 data 의존성을 추가하지 않습니다. 필요하면 별도 변경에서 순수 Kotlin JVM 모듈로 전환할 수 있습니다.

## 기능 패키지 규칙

feature의 화면은 `src/main/kotlin/com/kiri/feature/<기능>/presentation/` 아래에 배치하며, 기능 규모에 따라 하위 패키지로 분리합니다. Kotlin package는 디렉터리와 일치하며 앱에서 해당 `presentation` 경로를 import합니다. 아래 파일과 폴더는 실제 동작이 필요할 때 생성하며 빈 클래스는 미리 만들지 않습니다.

| 위치 (feature 기본 패키지 기준) | 추가 시점과 역할 |
| --- | --- |
| `<기능>Route.kt` | 상태 수집과 화면 이동 연결이 필요할 때 |
| `<기능>ViewModel.kt` | 상태 변화 및 사용자 이벤트 처리가 필요할 때 |
| `model/<기능>UiState.kt` | 화면 상태와 표시 전용 모델이 필요할 때 |
| `presentation/<기능>Screen.kt` | 화면 렌더링 |
| `presentation/component/` | 해당 feature 내부에서 재사용하는 UI |

- `domain/src/main/kotlin/com/kiri/domain/model/`: 비즈니스 모델.
- `domain/.../repository/`: 저장소 계약. `domain/.../usecase/`: 실제 비즈니스 규칙이 필요할 때 use case.
- `data/src/main/kotlin/com/kiri/data/repository/`: domain 계약 구현. `datasource/local/`, `datasource/remote/`: 데이터 접근. `dto/`, `mapper/`, `di/`: API 확정 및 구현 시 추가.
- `uikit`: 기능과 독립적인 테마·공통 UI. `core`: 범용 코드. 두 모듈에서 feature/data를 참조하지 않습니다.
- 현재 의존성: `app → feature, data, domain, uikit`, `feature → domain, uikit`, `data → domain, core`. domain은 다른 프로젝트 모듈에 의존하지 않습니다. 화면 연결을 위해 home은 detail·record·onboarding, place와 record는 detail을 참조합니다. feature에서 data를 직접 참조하지 않습니다.

## 검증

Windows에서 `.\gradlew.bat :app:assembleDebug testDebugUnitTest :app:lintDebug`를 실행합니다. CI는 JDK 17과 SDK 36 / Build Tools 36.0.0을 설치하고 같은 태스크를 실행합니다. Linux에서는 wrapper 실행 비트에 의존하지 않도록 `bash ./gradlew`로 호출합니다.

초기 모듈 설정은 Gradle 설정과 패키지 이동만 포함해 형식적인 기능 테스트를 추가하지 않았습니다. 현재는 기능별 단위 테스트와 Compose UI 테스트를 포함합니다. `testDebugUnitTest`의 `NO-SOURCE`는 테스트 태스크 실행 성공이지 기능 테스트가 존재하거나 통과했다는 뜻이 아닙니다. 실제 기능 구현 시 `src/test`에서 비즈니스 규칙·상태·오류·경계를, 필요하면 `src/androidTest`에서 대체 데이터로 화면 상호작용을 검증합니다.

### 2026-09-24 로컬 검증 결과

- `.\gradlew.bat :app:assembleDebug testDebugUnitTest :app:lintDebug --rerun-tasks --console=plain`: BUILD SUCCESSFUL (466 tasks executed). app/record의 AAR metadata 검사도 통과.
- 최초 lint 오류는 로컬 `local.properties`의 `sdk.dir` 드라이브 구분자였으며 `C\:/...`로 수정했습니다. lint가 이전 분석 결과를 재사용하여 `--rerun-tasks`로 다시 확인했습니다. 검사 비활성화나 baseline은 사용하지 않았습니다.
- 단위 테스트 태스크는 전 모듈 `NO-SOURCE`: 기능 테스트는 아직 없습니다.
- lint: 오류 0 / 경고 9. 최신 Gradle·AGP·Kotlin·Compose·Activity 안내와 기존 앱 아이콘 미지정 경고를 유지합니다. 호환 조합을 지키기 위해 최신 버전을 무조건 적용하지 않습니다.
- `:app:dependencyInsight --dependency animation-core --configuration debugRuntimeClasspath`에서 animation-core 1.10.0 선택을 확인했습니다.
- 기존 Medium_Phone 에뮬레이터에 APK 설치 후 일곱 화면의 제목·준비 중 문구·목록 복귀를 ADB/UI hierarchy로 확인했습니다. 초기 부팅 중 System UI 응답 지연이 있었고 대기 후 진행했습니다. 실기기 및 다른 OS/화면 크기는 검증하지 않았습니다.
- CI YAML의 JDK/SDK/Build Tools와 실행 태스크를 로컬 설정에 맞췄습니다. 원격 CI 실행은 하지 않았습니다.
- 로컬 산출물: `app/build/outputs/apk/debug/app-debug.apk`, `app/build/reports/lint-results-debug.html`, `build/reports/build-verification.log`, `build/reports/build-verification-dependencies.log` (Git 제외).
