package com.kiri.feature.mypage.model.withdraw

data class WithdrawUiState(
    // 탈퇴 요청 중인지. 그동안 탈퇴할게요 버튼을 막는다
    val isWithdrawing: Boolean = false,
)
