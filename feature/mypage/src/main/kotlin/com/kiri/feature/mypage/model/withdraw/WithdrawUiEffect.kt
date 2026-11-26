package com.kiri.feature.mypage.model.withdraw

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동 등)
sealed interface WithdrawUiEffect {
    // 탈퇴를 마치고 로그인 화면으로 간다
    data object NavigateToLogin : WithdrawUiEffect

    // 계정 정보로 돌아간다
    data object NavigateBack : WithdrawUiEffect

    // 탈퇴하지 못했다. 계정이 그대로라 화면에 남는다
    data class ShowWithdrawError(val cause: Throwable) : WithdrawUiEffect
}
