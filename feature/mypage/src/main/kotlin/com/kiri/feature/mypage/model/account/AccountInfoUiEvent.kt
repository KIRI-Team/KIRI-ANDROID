package com.kiri.feature.mypage.model.account

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface AccountInfoUiEvent {
    data object TermsClicked : AccountInfoUiEvent
    data object OpenLicenseClicked : AccountInfoUiEvent
    data object LogoutClicked : AccountInfoUiEvent
    data object LogoutCanceled : AccountInfoUiEvent
    data object LogoutConfirmed : AccountInfoUiEvent
    data object WithdrawClicked : AccountInfoUiEvent
    data object BackClicked : AccountInfoUiEvent
}
