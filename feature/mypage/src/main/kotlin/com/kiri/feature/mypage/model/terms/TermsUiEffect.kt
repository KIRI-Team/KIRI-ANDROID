package com.kiri.feature.mypage.model.terms

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동 등)
sealed interface TermsUiEffect {
    // 계정 정보로 돌아간다
    data object NavigateBack : TermsUiEffect
}
