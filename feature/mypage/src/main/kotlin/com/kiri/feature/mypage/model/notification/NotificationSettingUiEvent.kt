package com.kiri.feature.mypage.model.notification

import java.time.LocalTime

// 화면 → ViewModel 으로 전달하는 사용자 동작
sealed interface NotificationSettingUiEvent {
    data object AllToggled : NotificationSettingUiEvent
    data object RegularToggled : NotificationSettingUiEvent
    data object RegularTimeClicked : NotificationSettingUiEvent
    data class RegularTimeSaved(val time: LocalTime) : NotificationSettingUiEvent
    data object TimeDialogDismissed : NotificationSettingUiEvent
    data object BackClicked : NotificationSettingUiEvent
}
