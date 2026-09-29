# AGP 9.x 업그레이드 체크리스트

현재 AGP 8.13.2에 맞춰 버전을 낮춰 둔 항목과, AGP 9로 올릴 때 함께 바꿔야 할 것들을 정리한다.

## 1. 버전 올리기 (`gradle/libs.versions.toml`)

| 항목 | 현재 | 올릴 버전 | 이유 |
|---|---|---|---|
| `agp` | 8.13.2 | 9.1.0 이상 | |
| Gradle Wrapper | 8.13 | AGP 9가 요구하는 버전 (9.x) | `gradle-wrapper.properties` 수정 |
| `hilt` | 2.57.2 | 2.59 이상 | AGP 9 지원은 2.59부터 |
| `androidxHilt` | 1.3.0 | 1.4.0 이상 | 1.4.0은 AGP 9.1과 compileSdk 37 필요 (lifecycle 2.11 때문) |
| `compileSdk` | 36 | 37 | 위 lifecycle 2.11 요구사항 |
| `ksp` | 2.2.21-2.0.5 | AGP 9의 내장 Kotlin을 지원하는 KSP 2.3.x 이상 | |

## 2. 빌드 스크립트 수정

- **내장 Kotlin**: AGP 9는 Kotlin을 직접 지원하므로 `org.jetbrains.kotlin.android` 플러그인을 제거한다.
  - 대상: 루트 `build.gradle.kts`, `app/build.gradle.kts`, `build-logic/.../kiri.library.gradle.kts`
  - 한 번에 전환하기 어려우면 `gradle.properties`에 `android.builtInKotlin=false`를 두고 나중에 옮긴다.
- **javapoet 고정 해제 검토**: 루트 `build.gradle.kts`의 `buildscript { classpath(libs.javapoet) }`는 AGP 8이 끌어오는 javapoet 1.10과의 충돌을 피하려고 넣은 것이다. 업그레이드한 뒤 이 블록을 지워 보고, `:app:hiltAggregateDepsDebug`가 통과하면 카탈로그의 `javapoet` 항목까지 삭제한다.
- **새 DSL**: AGP 9에서 deprecated/제거된 DSL 경고가 나오면 `kiri.library`, `kiri.compose` 컨벤션 플러그인부터 고친다.

## 3. 검증

```
./gradlew :app:assembleDebug :app:lintDebug
```

- Hilt 생성 코드(`Hilt_*`)가 만들어지고 `hiltAggregateDeps` 태스크가 통과하는지 확인한다.
- 앱을 실행해서 `hiltViewModel()`을 쓰는 화면이 정상 동작하는지 확인한다.
