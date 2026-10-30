package com.kiri.feature.mypage.model.withdraw

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface WithdrawUiEvent {
    data object WithdrawClicked : WithdrawUiEvent
    data object BackClicked : WithdrawUiEvent
}
