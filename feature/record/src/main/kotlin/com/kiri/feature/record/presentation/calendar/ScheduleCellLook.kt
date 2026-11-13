package com.kiri.feature.record.presentation.calendar

import com.kiri.feature.record.model.RecordCalendarMark

// 약속이 있는 날짜 칸의 모양 (Figma 10693:9281)
internal enum class ScheduleCellLook {
    // 예정된 약속: Tertiary 배경, 밝은 시계
    UPCOMING,
    // 지난 약속: Secondary 배경, 어두운 시계. 취소·미확정 약속도 여기에 둔다
    PAST,
    // 오늘 + 약속: 초록 배경·테두리, 초록 시계
    TODAY,
}

// 약속 표시가 아니면 null. 사진·질문·버킷 표시는 기존 모양을 그대로 쓴다
internal fun scheduleCellLook(mark: RecordCalendarMark?, today: Boolean, past: Boolean): ScheduleCellLook? =
    when (mark) {
        RecordCalendarMark.CANCELLED -> ScheduleCellLook.PAST
        RecordCalendarMark.PLANNED, RecordCalendarMark.CONFIRMED -> when {
            today -> ScheduleCellLook.TODAY
            past -> ScheduleCellLook.PAST
            else -> ScheduleCellLook.UPCOMING
        }
        else -> null
    }
