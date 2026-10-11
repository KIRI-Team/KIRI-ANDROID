# feature/mypage

마이페이지 탭과 그 안의 설정 화면들을 담는 모듈이다. 화면이 많으니 수정하기 전에 아래 표에서 **어느 화면이 어느 파일인지** 먼저 확인한다.

UI 구조 규칙은 [`feature/Skill-UI-Structure.md`](../Skill-UI-Structure.md)를 따른다.

## 1. 패키지 구조

`model/`과 `presentation/`을 위에 두고, 그 안을 화면별 하위 패키지로 나눈다. ViewModel은 모듈 최상위에 둔다.

```
com/kiri/feature/mypage/
├── *ViewModel.kt, HomeAddressStep.kt   ← 화면별 ViewModel (아래 표 참고)
├── navigation/
│   └── MypageNavigation.kt             ← 마이페이지 그래프와 모든 경로
├── model/
│   ├── common/        MypageMenu(메뉴·섹션 목록), MypageProfile(프로필), LastUpdated(최종 업데이트 문구)
│   ├── nameedit/      NameEditUiState·UiEvent·UiEffect   (닉네임·가족명 수정 공통)
│   ├── address/       HomeAddress, HomeAddressUiState·UiEvent·UiEffect
│   ├── invite/        InviteLinkUiState·UiEvent·UiEffect
│   ├── notification/  NotificationSettingUiState·UiEvent·UiEffect
│   ├── family/        FamilyMember
│   ├── account/       AccountInfoUiState·UiEvent·UiEffect (로그아웃 팝업)
│   ├── terms/         TermsUiState·UiEvent·UiEffect (약관 목록은 domain의 AgreementTerm)
│   ├── license/       OpenLicense(라이브러리 목록), OpenLicenseUiState·UiEvent·UiEffect
│   └── suggestion/    SuggestionUiState·UiEvent·UiEffect
└── presentation/
    ├── common/        여러 화면이 함께 쓰는 컴포넌트
    ├── main/          마이페이지 첫 화면 (+ component/)
    ├── nameedit/      닉네임 수정, 가족명 수정
    ├── family/        가족 기본 설정 (+ component/)
    ├── address/       우리집 주소 설정 (+ component/)
    ├── invite/        공용초대링크 관리
    ├── notification/  알림 설정
    ├── account/       계정 정보 (+ 로그아웃 팝업)
    ├── terms/         이용 약관
    ├── license/       오픈라이선스
    ├── withdraw/      회원탈퇴
    ├── preference/    내 취향 설정 (자리표시자)
    └── suggestion/    건의함 (+ component/)
```

새 화면을 추가할 때도 같은 이름의 하위 패키지를 `model/`과 `presentation/`에 하나씩 만든다. 상태가 없는 화면은 `presentation/`에만 만든다.

## 2. 화면 한눈에 보기

| 화면 | 들어가는 곳 | 경로 (route) | 화면 파일 (`presentation/`) | ViewModel | 모델 (`model/`) | 루트 testTag |
|---|---|---|---|---|---|---|
| 마이페이지 | 바텀바 마이페이지 탭 | `mypage` | `main/MypageScreen.kt` | 없음 | `common/` | `mypage_screen` |
| 닉네임 수정 | 프로필 펜 아이콘, 가족 설정의 내 카드 펜 | `mypage_graph/nickname_edit` | `nameedit/NicknameEditScreen.kt` | `NicknameEditViewModel` | `nameedit/` | `nickname_edit_screen` |
| 가족명 수정 | 가족 설정의 가족 이름 펜 | `mypage_graph/family_name_edit` | `nameedit/FamilyNameEditScreen.kt` | `FamilyNameEditViewModel` | `nameedit/` | `family_name_edit_screen` |
| 내 취향 설정 | 메뉴 | `mypage_graph/my_preference` | `preference/MyPreferenceScreen.kt` | 없음 (자리표시자) | - | `my_preference_screen` |
| 알림 설정 | 메뉴 | `mypage_graph/notification_setting` | `notification/NotificationSettingScreen.kt` | `NotificationSettingViewModel` | `notification/` | `notification_setting_screen` |
| 가족 기본 설정 | 메뉴 | `mypage_graph/family_setting` | `family/FamilySettingScreen.kt` | 없음 | `family/`, `common/` | `family_setting_screen` |
| 우리집 주소 설정 | 메뉴 | `mypage_graph/home_address` | `address/HomeAddressScreen.kt` | `HomeAddressViewModel` | `address/` | `home_address_screen` |
| 공용초대링크 관리 | 메뉴 | `mypage_graph/invite_link` | `invite/InviteLinkScreen.kt` | `InviteLinkViewModel` | `invite/` | `invite_link_screen` |
| 계정 정보 | 메뉴 | `mypage_graph/account_info` | `account/AccountInfoScreen.kt` | `AccountInfoViewModel` | `account/` | `account_info_screen` |
| 이용 약관 | 계정 정보 | `mypage_graph/account_info/terms` | `terms/TermsScreen.kt` | `TermsViewModel` | `terms/` | `terms_screen` |
| 오픈라이선스 | 계정 정보 | `mypage_graph/account_info/open_license` | `license/OpenLicenseScreen.kt` | `OpenLicenseViewModel` | `license/` | `open_license_screen` |
| 회원탈퇴 | 계정 정보 | `mypage_graph/account_info/withdraw` | `withdraw/WithdrawScreen.kt` | 없음 | - | `withdraw_screen` |
| 건의함 | 메뉴 | `mypage_graph/suggestion` | `suggestion/SuggestionScreen.kt` | `SuggestionViewModel` | `suggestion/` | `suggestion_screen` |

- 메뉴 화면의 경로는 `MypageMenu` 이름을 소문자로 바꾼 값이다 (`mypage_graph/${menu.name.lowercase()}`). 메뉴를 추가하면 `MypageMenu`에 한 줄, `MypageNavigation.kt`의 `MypageMenuScreen`에 분기 하나를 늘린다.
- ViewModel이 있는 화면은 같은 파일 안에 `XxxRoute`(ViewModel 연결)와 `XxxScreen(state, onEvent)`(상태 비보유)가 함께 있다. 네비게이션에는 Route를 연결한다.

## 3. 화면별 파일 설명

### 닉네임·가족명 수정 (`nameedit`)
두 화면은 문구만 다르고 동작이 같아서 공통 코드를 함께 쓴다.
- `NameEditViewModel.kt`: 공통 동작(입력, 닉네임 규칙 검사, 이전 화면으로 돌아가기). 규칙은 `domain`의 `NicknameValidator`를 쓴다.
- `NicknameEditViewModel.kt`, `FamilyNameEditViewModel.kt`: 처음 보여줄 이름만 정하는 하위 클래스
- `presentation/nameedit/NameEditScreen.kt`: 공통 화면 틀과 effect 처리(`NameEditEffects`), 오류 문구
- `presentation/nameedit/NicknameEditScreen.kt`, `FamilyNameEditScreen.kt`: 제목·안내 문구만 넘기는 얇은 화면

### 우리집 주소 설정 (`address`)
한 Route 안에서 `HomeAddressStep`(주소 보기 / 등록 / 수정 / 검색)에 따라 화면을 바꾼다.
- `HomeAddressScreen.kt`: Route, 단계별 상단 바, 단계 분기
- `HomeAddressSavedContent.kt`: 등록한 주소 보기 (`우리집위치/O`)
- `HomeAddressFormContent.kt`: 주소 등록·수정 (`우리집 위치/X`, `우리집위치/O/수정`)
- `AddressSearchContent.kt`: 우편번호 찾기 (`주소검색화면`)
- `component/HomeAddressTitle.kt`: 위쪽 제목과 안내 문구

### 공용초대링크 관리 (`invite`)
- `InviteLinkViewModel.kt`: 온보딩과 같은 `InviteRepository`에서 지금 초대를 불러오고, 리셋하기를 누르면 새로 발급한다 (최소 1초 로딩).
- `InviteLinkScreen.kt`: 링크코드·초대링크 카드, 리셋하기, 공유. 발급 중에는 uikit의 `KiriInviteCodeLoadingScreen`(온보딩과 같은 로딩 화면)을 보여준다.

### 알림 설정 (`notification`)
- `NotificationSettingScreen.kt`: 전체 알림·정기 알림 토글과 발송 시각 행. 전체 알림을 끄면 정기 알림은 바꿀 수 없다.

### 가족 기본 설정 (`family`)
- `FamilySettingScreen.kt`: 가족 이름 카드와 구성원 3열 목록
- `component/FamilyMemberCard.kt`: 구성원 카드 (내 카드에만 닉네임 수정 펜)

### 마이페이지 첫 화면 (`main`)
- `MypageScreen.kt`: 프로필과 섹션별 메뉴 목록
- `component/MypageProfileRow.kt`: 프로필 사진·닉네임·가족명과 수정 펜

### 건의함 (`suggestion`)
- `SuggestionViewModel.kt`: 내용(최대 500자, 넘으면 잘라냄)과 사진(최대 5장, 중복 제외)을 보관한다. 내용이 있어야 보낼 수 있다.
- `SuggestionScreen.kt`: Route(시스템 사진 선택기 연결), 내용 입력칸과 글자 수, 사진 줄, 의견 보내기 버튼(내용이 비면 Neutral, 적으면 Accent)
- `component/SuggestionPhotoTile.kt`: 사진 추가 칸(KIRI/Photo Upload)과 고른 사진 미리보기·빼기 버튼

### 계정 정보 (`account`, `terms`, `license`, `withdraw`)
- `AccountInfoScreen.kt`: 이용 약관·오픈 라이선스·앱 버전, 로그아웃·회원탈퇴 메뉴. 로그아웃을 누르면 확인 팝업을 띄우고, 확인하면 로그인 화면으로 간다.
- `TermsScreen.kt`: 회원가입과 같은 약관 목록(`domain`의 `AgreementTerm`). 누르면 uikit의 `KiriDetailSheet`(회원가입 약관 창과 같음)를 띄우고, 동의하기 대신 확인으로 닫는다.
- `OpenLicenseScreen.kt`: 앱이 쓰는 라이브러리 목록. 누르면 라이선스 전문을 팝업으로 보여주고 닫기로 닫는다.
- `WithdrawScreen.kt`: 탈퇴 안내. 탈퇴할게요를 누르면 로그인 화면으로 간다.
- 로그인 화면 이동은 `mypageGraph(onNavigateToLogin)` → `MainScreen` → `KiriApp`에서 연결한다 (메인을 백스택에서 지운다).

### 함께 쓰는 컴포넌트 (`presentation/common`)
| 파일 | 내용 | 쓰는 화면 |
|---|---|---|
| `MypageMenuGroup.kt` | 둥근 메뉴 묶음(`MypageMenuCard`)과 메뉴 줄(`MypageMenuRow`) | 마이페이지, 계정 정보 |
| `MypageInputBox.kt` | KIRI/Input 한 줄 입력창 (비활성화·읽기 전용·오류·오른쪽 아이콘) | 닉네임·가족명 수정, 주소 |
| `MypageDialog.kt` | 가운데 팝업과 팝업 버튼(KIRI/Button Sheet) | 로그아웃, 오픈라이선스 |
| `MypageToggle.kt` | 42×24 토글, 비활성화 투명도 `DISABLED_ALPHA` | 알림 설정 |
| `MypagePlaceholderScreen.kt` | 디자인이 나오기 전 화면 | 내 취향 설정 |

마이페이지 밖에서도 쓰게 되면 `uikit`으로 옮긴다. 이미 `uikit`에 있는 것(`KiriTopBar`, `KiriButton`, `KiriTitle`, `KiriInviteCodeCard`, `KiriInviteCodeLoadingScreen`)은 새로 만들지 않는다.

## 4. 데이터 상태 (아직 가짜인 곳)

| 데이터 | 지금 | 바꿀 곳 |
|---|---|---|
| 프로필, 가족명, 구성원 | Figma 예시 값 (`MypageProfile.Sample`, `FamilyMember.Samples`) | 사용자·가족 저장소가 생기면 교체 |
| 닉네임·가족명 저장 | 저장하지 않고 돌아가기만 함 | `NameEditViewModel`의 TODO |
| 우리집 주소 | ViewModel 안에만 보관, 검색 결과 없음 | `HomeAddressViewModel`의 TODO |
| 알림 설정 | ViewModel 안에만 보관, 시간 선택 없음 | `NotificationSettingViewModel`의 TODO |
| 초대 | `data`의 `FakeInviteRepository` (앱 전체에서 하나, 7일 만료) | 초대 API가 생기면 data 모듈에서 교체 |
| 건의 보내기 | 보내지 않고 돌아가기만 함 | `SuggestionViewModel`의 TODO |
| 로그아웃·회원탈퇴 | 처리 없이 로그인 화면으로만 이동 | `AccountInfoViewModel`, `MypageNavigation.kt`의 TODO |
| 약관 본문, 라이선스 목록 | 예시 본문, 버전은 직접 적음 | `AgreementTerm`, `OpenLicense`의 TODO |

## 5. 테스트

- 단위 테스트: `src/test/.../mypage/` — ViewModel마다 `XxxViewModelTest`. `TestInviteRepository`, `MainDispatcherRule`을 함께 쓴다.
- 화면 테스트: `app/src/androidTest/.../app/mypage/` — 화면마다 `XxxScreenTest`가 `XxxScreen(state, onEvent)`에 상태를 직접 넣어 확인한다.
- 이동 테스트: `app/src/androidTest/.../app/navigation/KiriAppNavigationTest.kt` — 메뉴를 눌러 각 화면으로 가고 돌아오는지 확인한다.
