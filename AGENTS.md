# KIRI ANDROID 개발 규칙

- 기능 또는 버그를 수정하는 PR에는 해당 동작을 검증하는 자동 테스트를 함께 작성한다.
- 비즈니스 규칙, 상태 변화, 입력 오류와 경계 조건은 해당 모듈의 `src/test` 단위 테스트로 검증한다.
- 화면 상호작용과 이동은 필요할 때 `src/androidTest` Compose UI 테스트로 검증한다. 실제 서버에 의존하지 않도록 대체 데이터와 테스트용 의존성을 사용한다.
- 순수한 폴더 구조 변경이나 자리표시자 화면처럼 검증할 동작이 없는 변경에는 형식적인 테스트를 만들지 않는다. PR 설명에 테스트가 불필요한 이유를 적는다.
- PR을 완료하기 전에 변경 영역의 테스트와 `:app:assembleDebug`, `:app:lintDebug`를 실행하고 결과를 PR에 적는다. 실행하지 못했거나 실패한 항목은 이유와 영향을 명시한다.
- CI가 통과해도 실제 기기 또는 에뮬레이터에서 핵심 화면을 확인한다. 테스트는 모든 기기와 상황에서 오류가 없음을 보증하지 않는다.


# KIRI Android Agent Instructions

## Project

KIRI is an Android project using Kotlin and Jetpack Compose.

Follow the existing project architecture and conventions before introducing new patterns.

## Project memory — specification location

Updated by the user on 2026-10-08:

- All local KIRI specification documents and implementation reports have moved to `C:/Users/PC/AndroidStudioProjects/KIRI-SPECS`.
- Use that root for local documentation references; the documents currently live under its `docs/` directory, including `docs/spec/` and `docs/reports/`.
- Read the relevant document from this root before subsequent work. Prefer the latest dated specification when several local versions exist.
- Do not assume `KIRI-ANDROID/docs/`, the old ChatGPT project `sources/` mirror, or old conversation paths contain the current local documents.
- Keep specification documents in `KIRI-SPECS`; do not copy them back into the Android repository unless the user explicitly requests it.
- The Notion source-of-truth and Figma rules below remain applicable. This memory records the current location of local specification documents.

## Architecture

- Follow MVVM and Clean Architecture.
- Preserve the existing multi-module structure.
- Reuse existing patterns from well-implemented feature modules where appropriate.
- Do not introduce unnecessary abstractions or dependencies.
- Prefer modifying existing architecture consistently over creating parallel patterns.

## Sources of truth

### Product requirements

The source of truth for product requirements is the KIRI feature specification in Notion.

Before implementing or modifying a feature:

1. Use the Notion MCP server to read the relevant feature specification.
2. Re-read the relevant specification before implementation instead of relying on previous conversation context.
3. Do not use locally exported CSV files when Notion MCP is available.
4. Never modify the Notion specification unless explicitly instructed.
5. If Notion MCP cannot access the specification, report the access problem instead of guessing requirements.

### UI design

Figma is the source of truth for UI design.

Before implementing UI:

1. Use the configured Figma MCP tools to inspect the relevant design.
2. Match layout, spacing, typography, components, and states to the design.
3. Reuse existing UIKit/design-system components whenever possible.
4. Do not modify the Figma source unless explicitly instructed.

## Implementation workflow

Before changing code:

1. Read the relevant specification.
2. Inspect the relevant Figma design when UI is involved.
3. Inspect the existing implementation and nearby feature modules.
4. Make the smallest consistent implementation.
5. Run the relevant build, lint, and tests.

Do not claim that an implementation is complete if the build or required tests fail.

## Git

- Do not push.
- Do not merge.
- Do not create or modify remote branches unless explicitly instructed.
- The user performs pushes and merges manually.

- Android source root: `C:/Users/PC/AndroidStudioProjects/KIRI-ANDROID`. Always inspect and edit the app in this root; projectless chat folders are not the app repository.
