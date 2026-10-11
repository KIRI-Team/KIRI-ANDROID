package com.kiri.feature.mypage.model.notification

import java.time.LocalTime

data class NotificationSettingUiState(
    val allEnabled: Boolean = true,
    // 전체 알림을 꺼도 값은 그대로 두었다가 다시 켜면 되살린다
    val regularEnabled: Boolean = true,
    val regularTime: LocalTime = LocalTime.of(9, 0),
) {
    // 전체 알림이 꺼져 있으면 정기 알림 설정은 바꿀 수 없다
    val canChangeRegular: Boolean
        get() = allEnabled

    // 정기 알림이 실제로 켜져 있을 때만 발송 시각을 바꿀 수 있다
    val canChangeRegularTime: Boolean
        get() = allEnabled && regularEnabled

    // 예: 오전 9시, 오후 3시 30분
    val regularTimeText: String
        get() {
            val period = if (regularTime.hour < 12) "오전" else "오후"
            val hour = (regularTime.hour % 12).let { if (it == 0) 12 else it }
            val minute = if (regularTime.minute == 0) "" else " ${regularTime.minute}분"
            return "$period ${hour}시$minute"
        }
}
