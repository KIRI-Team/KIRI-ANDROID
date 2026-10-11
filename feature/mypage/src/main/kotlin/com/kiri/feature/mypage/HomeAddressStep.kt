package com.kiri.feature.mypage

enum class HomeAddressStep(
    val displayName: String,
) {
    // 등록한 주소 보기
    SAVED("주소 있음"),

    // 주소가 없을 때 등록
    REGISTER("주소 등록"),

    // 등록한 주소 수정
    EDIT("주소 수정"),

    // 우편번호 찾기
    SEARCH("주소 검색"),
}
