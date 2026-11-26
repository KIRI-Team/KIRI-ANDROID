package com.kiri.feature.mypage.model.account

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동 등)
sealed interface AccountInfoUiEffect {
    data object NavigateToTerms : AccountInfoUiEffect
    data object NavigateToOpenLicense : AccountInfoUiEffect
    data object NavigateToWithdraw : AccountInfoUiEffect

    // 로그아웃하고 로그인 화면으로 간다
    data object NavigateToLogin : AccountInfoUiEffect

    // 마이페이지로 돌아간다
    data object NavigateBack : AccountInfoUiEffect
}
