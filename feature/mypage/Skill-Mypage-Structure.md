# feature/mypage 구조와 작업 규칙

마이페이지 탭과 그 안의 설정 화면들을 담는 모듈이다. **화면별 파일 위치와 경로 표는 [`README.md`](README.md)에 있다.** 이 문서는 구조 규칙과 새 화면을 만드는 절차만 적는다. 공통 UI 규칙은 [`feature/Skill-UI-Structure.md`](../Skill-UI-Structure.md), 그래프 등록 규칙은 [`Skill-Navigation.md`](../../app/src/main/java/com/kiri/app/navigation/Skill-Navigation.md) 9장을 따른다.

## 1. 진입점과 경로

- `navigation/MypageNavigation.kt`의 `mypageGraph(navController, onNavigateToLogin, ...)`가 마이페이지 그래프(`mypage_graph`)를 등록한다. app의 `KiriNavHost`가 한 줄로 호출한다.
- 메뉴 화면 경로는 `MypageMenu` 이름을 소문자로 바꾼 값이다 (`mypage_graph/notification_setting`). 계정 정보 하위 화면은 `mypage_graph/account_info/xxx`다.
- 쓰는 domain: `member`(내 정보·닉네임), `family`(가족 설정·주소), `invite`(초대 링크), `auth`(로그아웃·탈퇴), `term`(약관), `taste`(취향), `suggestion`(건의), `review.ReviewPhotoRepository`(사진 업로드), `home.HomeRepository`(가족 정보)

## 2. 패키지 구조

`model/`과 `presentation/`을 위에 두고, 그 안을 **화면 이름이 같은 하위 패키지**로 나눈다. ViewModel은 모듈 루트에 둔다.

```
com.kiri.feature.mypage/
├── XxxViewModel.kt                         화면별 ViewModel
├── navigation/MypageNavigation.kt          그래프와 모든 경로
├── model/<화면>/                           XxxUiState·UiEvent·UiEffect와 화면 모델
│   └── common/                             MypageMenu, MypageProfile, LastUpdated
└── presentation/<화면>/                    XxxScreen (+ component/)
    └── common/                             여러 화면이 함께 쓰는 컴포넌트 (MypageDialog, MypageInputBox, MypageMenuGroup, MypageToggle …)
```

- `model/account` ↔ `presentation/account`처럼 하위 패키지 이름을 양쪽에서 똑같이 맞춘다. 상태가 없는 화면은 `presentation/`에만 만든다.
- 닉네임 수정과 가족명 수정은 `nameedit/`의 `NameEditViewModel`·`NameEditScreen`을 함께 쓴다. 비슷한 수정 화면이 생기면 이 공통 틀을 재사용한다.

## 3. 모듈 규칙

- 화면 Composable은 NavController를 모른다. `mypageGraph()` 안에서만 NavController를 쓰고, 화면에는 `onBackClick` 같은 람다만 넘긴다.
- 로그아웃·탈퇴 뒤 로그인 이동처럼 메인 밖으로 나가는 이동은 `mypageGraph(onNavigateToLogin)` 콜백으로 app에 올린다.
- 방장 전용 기능(가족명 수정 등)은 서버가 판단한다. 서버가 `NotFamilyOwnerException`으로 거절하면 방장 전용 안내 문구를 보여준다 (`FamilyNameEditViewModel`).
- 둘러보기 사용자(`FamilySettingUiState.isBrowsing`)가 `requiresFamily` 메뉴를 누르면 이동하지 않고 `BROWSING_BLOCKED_MESSAGE` 토스트를 띄운다 (`MypageScreen`).
- 이 모듈 밖에서도 쓰게 된 컴포넌트는 `presentation/common`에서 uikit으로 옮긴다. uikit에 이미 있는 것(`KiriTopBar`, `KiriButton`, `KiriTitle`, `KiriDetailSheet` 등)은 새로 만들지 않는다.

## 4. 새 화면을 추가할 때

1. 메뉴에서 들어가는 화면이면 `model/common/MypageMenu`에 항목을 추가한다. 경로는 이름에서 자동으로 만들어진다.
2. 상태가 있으면 `model/<화면>/`에 `XxxUiState`·`UiEvent`·`UiEffect`를, 모듈 루트에 `XxxViewModel`을 만든다.
3. `presentation/<화면>/XxxScreen.kt`에 `XxxRoute`(ViewModel 연결, effect 처리)와 `XxxScreen(state, onEvent)`를 함께 둔다. 루트에 `testTag("xxx_screen")`을 붙인다.
4. `MypageNavigation.kt`의 `MypageMenuScreen` `when`에 분기를 추가한다. 메뉴가 아닌 하위 화면이면 `composable("mypage_graph/<상위>/xxx")`를 추가한다. `KiriNavHost.kt`는 고치지 않는다.
5. [`README.md`](README.md)의 화면 표(2장)와 파일 설명(3장)에 한 줄씩 추가한다.
6. `src/test`에 `XxxViewModelTest`를(`MainDispatcherRule`, `TestXxxRepository` 사용), `app/src/androidTest/.../mypage/`에 `XxxScreenTest`를, 메뉴 이동은 `KiriAppNavigationTest`·`MypageFamilyNavigationTest`에 추가한다.

## 5. 테스트

- `src/test`: ViewModel마다 `XxxViewModelTest`, `AddressServerFailureTest`, `presentation/nameedit/NameErrorsTest`. 가짜 저장소는 `TestFamilyRepository`, `TestInviteRepository`, `TestTasteRepository`.
- `app/src/androidTest/.../mypage/`: 화면마다 `XxxScreenTest`
