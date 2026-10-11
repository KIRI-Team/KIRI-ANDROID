package com.kiri.feature.mypage.model.notification

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface NotificationSettingUiEvent {
    data object AllToggled : NotificationSettingUiEvent
    data object RegularToggled : NotificationSettingUiEvent
    data object RegularTimeClicked : NotificationSettingUiEvent
    data object BackClicked : NotificationSettingUiEvent
}
