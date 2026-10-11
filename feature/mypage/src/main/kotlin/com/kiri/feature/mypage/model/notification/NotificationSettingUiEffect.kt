package com.kiri.feature.mypage.model.notification

// ViewModel → 화면 으로 전달하는 일회성 동작 (화면 이동 등)
sealed interface NotificationSettingUiEffect {
    // 마이페이지로 돌아간다
    data object NavigateBack : NotificationSettingUiEffect
}
