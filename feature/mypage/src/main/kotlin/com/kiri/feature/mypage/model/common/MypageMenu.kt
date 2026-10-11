package com.kiri.feature.mypage.model.common

// 마이페이지 메뉴 묶음 (화면에 이 순서대로 보인다)
enum class MypageSection(val title: String) {
    MY("내 설정"),
    FAMILY("가족 설정"),
    SETTINGS("설정"),
}

// 마이페이지 메뉴. 각 메뉴는 같은 이름의 화면으로 이동한다
enum class MypageMenu(val title: String, val section: MypageSection) {
    MY_PREFERENCE("내 취향 설정", MypageSection.MY),
    NOTIFICATION_SETTING("알림 설정", MypageSection.MY),
    FAMILY_SETTING("가족 기본 설정", MypageSection.FAMILY),
    HOME_ADDRESS("우리집 주소 설정", MypageSection.FAMILY),
    INVITE_LINK("공용초대링크 관리", MypageSection.FAMILY),
    ACCOUNT_INFO("계정 정보", MypageSection.SETTINGS),
    SUGGESTION("건의함", MypageSection.SETTINGS),
}
