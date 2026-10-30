package com.kiri.feature.mypage.model.account

data class AccountInfoUiState(
    // 로그아웃 확인 팝업을 띄웠는지
    val isLogoutDialogVisible: Boolean = false,
    // 로그아웃 요청 중인지. 끝나면 로그인 화면으로 가므로 다시 false 로 돌리지 않는다
    val isLoggingOut: Boolean = false,
)
