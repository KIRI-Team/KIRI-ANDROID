package com.kiri.feature.onboarding

enum class OnboardingStep(
    val displayName: String
) {
    // 모든 플로우에서 이용하는 온보딩 화면

    INPUT_NICKNAME("닉네임 입력"),
    CHOOSE_ROLE("역할 선택"),
    ONBOARDING_MAIN("온보딩 메인"),


    // 가족 공간 생성 플로우에서 이용하는 온보딩 화면

    INPUT_FAMILY_NAME("가족명 입력"),
    GENERATE_FAMILY_CODE_LOADING("코드 생성"),
    GENERATE_FAMILY_CODE("코드 생성 완료"),


    // 코드 입력 플로우에서 이용하는 온보딩 화면

    INPUT_FAMILY_CODE("가족 코드 입력");
}